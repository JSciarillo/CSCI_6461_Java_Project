package src;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class AsmTest {

    public static void main(String[] args) throws Exception {
        Path testsRoot = Paths.get("tests", "asm");
        if (!Files.isDirectory(testsRoot)) {
            System.out.println("ERROR: tests/asm directory not found at: " + testsRoot.toAbsolutePath());
            System.exit(1);
        }

        // If user passes one argument, run only that folder
        if (args.length == 1) {
            Path testDir = testsRoot.resolve(args[0]);
            runOneTest(testDir);
            System.out.println("PASS: " + args[0]);
            return;
        }

        // Otherwise run all test directories that contain source.asm
        List<Path> testDirs = listImmediateSubdirs(testsRoot);

        int passed = 0;
        int failed = 0;

        for (Path dir : testDirs) {
            String name = dir.getFileName().toString();

            // treat as a test only if it has source.asm
            if (!Files.exists(dir.resolve("source.asm"))) continue;

            try {
                runOneTest(dir);
                System.out.println("PASS: " + name);
                passed++;
            } catch (AssertionError ae) {
                System.out.println("FAIL: " + name);
                System.out.println(indent(ae.getMessage(), 2));
                failed++;
            }
        }

        System.out.printf("%nSummary: %d passed, %d failed%n", passed, failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void runOneTest(Path testDir) throws Exception {
        Path src = testDir.resolve("source.asm");
        if (!Files.exists(src)) {
            throw new AssertionError("Missing file: " + src);
        }

        Path expectedErrors = testDir.resolve("expected.errors");
        boolean expectsErrors = Files.exists(expectedErrors);

        Path expectedLoad = testDir.resolve("expected.load");
        Path expectedListing = testDir.resolve("expected.listing"); // optional

        // For positive tests, expected.load is required
        if (!expectsErrors && !Files.exists(expectedLoad)) {
            throw new AssertionError("Missing file: " + expectedLoad + " (required for positive tests)");
        }

        // For negative tests, expected.errors is required
        if (expectsErrors && !Files.exists(expectedErrors)) {
            throw new AssertionError("Missing file: " + expectedErrors + " (required for negative tests)");
        }

        // Write outputs into the test folder
        Path actualListingPath = testDir.resolve("actual.listing");
        Path actualLoadPath = testDir.resolve("actual.load");

        // Ensure outputs reflect THIS run (avoid stale files from previous runs)
        Files.deleteIfExists(actualListingPath);
        Files.deleteIfExists(actualLoadPath);

        File actualListing = actualListingPath.toFile();
        File actualLoad = actualLoadPath.toFile();

        Assembler asm = new Assembler();
        Assembler.AssemblerResult res = asm.assemble(src.toFile(), actualListing, actualLoad);

        // ----------------------------
        // Negative test: expect errors
        // ----------------------------
        if (expectsErrors) {
            if (res.errors == null || res.errors.isEmpty()) {
                throw new AssertionError("Expected assembler errors, but assembler reported none.");
            }

            String expectedErrText = normalize(Files.readString(expectedErrors, StandardCharsets.UTF_8));
            String actualErrText = normalize(String.join("\n", res.errors));

            // Tolerant matching: each non-empty line in expected.errors must appear somewhere in actual error output
            List<String> missing = new ArrayList<>();
            for (String expLine : expectedErrText.split("\n")) {
                String needle = expLine.trim();
                if (needle.isEmpty()) continue;
                if (!actualErrText.contains(needle)) missing.add(needle);
            }

            if (!missing.isEmpty()) {
                throw new AssertionError(
                        "Assembler errors did not contain expected text.\n" +
                        "Missing:\n" + indent(String.join("\n", missing), 2) + "\n" +
                        "Actual:\n" + indent(actualErrText, 2)
                );
            }

            // For negative tests, we do not require load/listing comparisons.
            return;
        }

        // ----------------------------
        // Positive test: expect success
        // ----------------------------
        if (res.errors != null && !res.errors.isEmpty()) {
            throw new AssertionError("Assembler errors:\n" + String.join("\n", res.errors));
        }

        if (!actualLoad.exists()) {
            throw new AssertionError("Assembler did not produce actual.load at: " + actualLoad.getAbsolutePath());
        }

        // Compare expected.load vs actual.load
        String actualLoadText = normalize(Files.readString(actualLoad.toPath(), StandardCharsets.UTF_8));
        String expectedLoadText = normalize(Files.readString(expectedLoad, StandardCharsets.UTF_8));
        assertEqualsWithDiff("expected.load vs actual.load", expectedLoadText, actualLoadText);

        // Compare listing only if expected.listing exists
        if (Files.exists(expectedListing)) {
            if (!actualListing.exists()) {
                throw new AssertionError("Assembler did not produce actual.listing at: " + actualListing.getAbsolutePath());
            }
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

        String msg = name + " mismatch at char index " + i + "\n" +
                "\n--- Expected (around diff) ---\n" + snippet(expected, i) +
                "\n--- Actual (around diff) ---\n" + snippet(actual, i);

        throw new AssertionError(msg);
    }

    private static int firstDiffIndex(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            if (a.charAt(i) != b.charAt(i)) return i;
        }
        return n; // one is prefix of the other
    }

    private static String snippet(String s, int i) {
        int start = Math.max(0, i - 80);
        int end = Math.min(s.length(), i + 80);
        return s.substring(start, end).replace("\n", "\\n\n");
    }

    private static String normalize(String s) {
        // normalize line endings + strip trailing whitespace per line + trim ends
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

    private static String indent(String s, int spaces) {
        String pad = " ".repeat(Math.max(0, spaces));
        String[] lines = s.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            out.append(pad).append(lines[i]);
            if (i < lines.length - 1) out.append("\n");
        }
        return out.toString();
    }
}
