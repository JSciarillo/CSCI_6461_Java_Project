package src;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class AsmTest {

    public static void main(String[] args) throws Exception {
        Path testsRoot = Paths.get("tests");

        if (!Files.isDirectory(testsRoot)) {
            System.out.println("ERROR: tests/ directory not found at: " + testsRoot.toAbsolutePath());
            System.exit(1);
        }

        // If user passes one argument, run only that folder.
        if (args.length == 1) {
            runOneTest(testsRoot.resolve(args[0]));
            System.out.println("PASS: " + args[0]);
            return;
        }

        // Otherwise run all test directories under tests/
        List<Path> testDirs = listImmediateSubdirs(testsRoot);

        if (testDirs.isEmpty()) {
            System.out.println("No test folders found under: " + testsRoot.toAbsolutePath());
            return;
        }

        int passed = 0;
        int failed = 0;

        for (Path dir : testDirs) {
            String name = dir.getFileName().toString();

            // Only treat as a test if it has a source.asm
            if (!Files.exists(dir.resolve("source.asm"))) {
                continue;
            }

            try {
                runOneTest(dir);
                System.out.println("PASS: " + name);
                passed++;
            } catch (AssertionError ae) {
                System.out.println("FAIL: " + name);
                System.out.println("  " + ae.getMessage());
                failed++;
            }
        }

        System.out.printf("%nSummary: %d passed, %d failed%n", passed, failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void runOneTest(Path testDir) throws Exception {
        Path src = testDir.resolve("source.asm");
        Path expectedLoad = testDir.resolve("expected.load");
        Path expectedListing = testDir.resolve("expected.listing"); // optional

        if (!Files.exists(src)) throw new AssertionError("Missing file: " + src);
        if (!Files.exists(expectedLoad)) throw new AssertionError("Missing file: " + expectedLoad);

        // Write outputs into the test directory (repo-visible)
        File actualListing = testDir.resolve("actual.listing").toFile();
        File actualLoad = testDir.resolve("actual.load").toFile();

        Assembler asm = new Assembler();
        Assembler.AssemblerResult res = asm.assemble(src.toFile(), actualListing, actualLoad);

        if (!res.errors.isEmpty()) {
            throw new AssertionError("Assembler errors:\n" + String.join("\n", res.errors));
        }

        // Compare load
        String actualLoadText = normalize(Files.readString(actualLoad.toPath(), StandardCharsets.UTF_8));
        String expectedLoadText = normalize(Files.readString(expectedLoad, StandardCharsets.UTF_8));
        assertEqualsWithDiff("expected.load vs actual.load", expectedLoadText, actualLoadText);

        // Compare listing only if expected exists
        if (Files.exists(expectedListing)) {
            String actualListingText = normalize(Files.readString(actualListing.toPath(), StandardCharsets.UTF_8));
            String expectedListingText = normalize(Files.readString(expectedListing, StandardCharsets.UTF_8));
            assertEqualsWithDiff("expected.listing vs actual.listing", expectedListingText, actualListingText);
        }
    }

    private static List<Path> listImmediateSubdirs(Path root) throws IOException {
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) out.add(p);
            }
        }
        out.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return out;
    }

    private static void assertEqualsWithDiff(String name, String expected, String actual) {
        if (expected.equals(actual)) return;

        int i = firstDiffIndex(expected, actual);
        String msg = name
                + " mismatch at char " + i
                + "\n--- Expected snippet ---\n" + snippet(expected, i)
                + "\n--- Actual snippet ---\n" + snippet(actual, i);
        throw new AssertionError(msg);
    }

    private static int firstDiffIndex(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) if (a.charAt(i) != b.charAt(i)) return i;
        return n;
    }

    private static String snippet(String s, int i) {
        int start = Math.max(0, i - 80);
        int end = Math.min(s.length(), i + 80);
        return s.substring(start, end).replace("\n", "\\n\n");
    }

    private static String normalize(String s) {
        // normalize newlines + strip trailing whitespace
        String[] lines = s.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            out.append(rstrip(lines[i]));
            if (i < lines.length - 1) out.append("\n");
        }
        return out.toString().trim();
    }

    private static String rstrip(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }
}
