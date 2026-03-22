package src;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * SimTest
 *
 * Runs Part I simulator tests.
 * Each test directory contains:
 *   - source.asm
 *   - expected.final   (required)  OR expected.trace (optional)
 *
 * expected.final format (key=value per line):
 *   PC=000020
 *   R0=0000
 *   R1=1234
 *   IX1=0010
 *   MEM[000030]=00FF
 * Values are HEX by default (4 digits for registers/words, 6 digits for addresses if you want).
 *
 * Lines beginning with ';' or '#' are comments.
 */
public final class SimTest {

    public static void main(String[] args) throws Exception {
        Path root = Paths.get("tests/sim");
        if (!Files.isDirectory(root)) {
            System.out.println("ERROR: tests/sim directory not found at: " + root.toAbsolutePath());
            System.exit(1);
        }

        if (args.length == 1) {
            runOne(root.resolve(args[0]));
            return;
        }

        int passed = 0, failed = 0;
        for (Path dir : listImmediateSubdirs(root)) {
            if (!Files.exists(dir.resolve("source.asm"))) continue;

            String name = dir.getFileName().toString();
            try {
                runOne(dir);
                System.out.println("PASS: " + name);
                passed++;
            } catch (AssertionError ae) {
                System.out.println("FAIL: " + name);
                System.out.println(ae.getMessage());
                failed++;
            }
        }

        System.out.println("Sim tests complete. passed=" + passed + " failed=" + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void runOne(Path testDir) throws Exception {
        if (!Files.isDirectory(testDir)) {
            throw new IllegalArgumentException("Not a directory: " + testDir);
        }

        Path src = testDir.resolve("source.asm");
        if (!Files.exists(src)) {
            throw new IllegalArgumentException("Missing source.asm in " + testDir);
        }

        Path expectedFinal = testDir.resolve("expected.final");
        Path expectedTrace = testDir.resolve("expected.trace");

        // Assemble to temp files inside test dir for easier debugging
        File listing = testDir.resolve("Listing.txt").toFile();
        File load = testDir.resolve("Load.txt").toFile();

        Assembler asm = new Assembler();
        Assembler.AssemblerResult res = asm.assemble(src.toFile(), listing, load);

        if (!res.success) {
            throw new AssertionError("Assembly failed:\n" + String.join("\n", res.errors));
        }

        // Load into simulator
        Simulator sim = new Simulator(2048);
        sim.reset();
        sim.loadProgramFromFile(load.getAbsolutePath());

        // Decide entry point:
        // Option 1: first generated address in assembled output
        Integer entry = firstInstructionAddress(res.lines);
        if (entry == null) throw new AssertionError("No instructions/data generated.");
        sim.setPC(entry);

        // Run/trace
        if (Files.exists(expectedTrace)) {
            runTraceTest(sim, expectedTrace);
        } else if (Files.exists(expectedFinal)) {
            runFinalStateTest(sim, expectedFinal);
        } else {
            throw new IllegalArgumentException("Missing expected.final or expected.trace in " + testDir);
        }
    }

    private static void runFinalStateTest(Simulator sim, Path expectedFinal) throws Exception {
        // Run until HALT or safety cap
        int maxSteps = 100_000;
        int steps = 0;
        while (!sim.getCPU().isHalted() && steps < maxSteps) {
            sim.singleStep();
            steps++;
        }
        if (!sim.getCPU().isHalted()) {
            throw new AssertionError("Program did not HALT within " + maxSteps + " steps.");
        }

        Map<String, String> expected = parseExpectedFile(expectedFinal);

        // Compare registers
        assertEqHex("PC", sim.getCPU().getPC(), expected);
        for (int i = 0; i < 4; i++) {
            assertEqHex("R" + i, sim.getCPU().getR(i), expected);
        }

        // If your CPU exposes IX getters (recommended), check IX1..IX3
        // If not, remove these assertions or add getIX() in CPU.
        for (int i = 1; i <= 3; i++) {
            assertEqHex("IX" + i, sim.getCPU().getIX(i), expected);
        }

        // Compare memory expectations: keys like MEM[000030]=00FF
        for (String k : expected.keySet()) {
            if (k.startsWith("MEM[")) {
                int addr = parseBracketAddr(k); // supports hex like MEM[000030]
                int got = sim.readMemory(addr);
                int exp = parseHex(expected.get(k));
                if ((got & 0xFFFF) != (exp & 0xFFFF)) {
                    throw new AssertionError("Mismatch " + k + ": expected=" + hex4(exp) + " got=" + hex4(got));
                }
            }
        }
    }

    private static void runTraceTest(Simulator sim, Path expectedTrace) throws Exception {
        // expected.trace format example:
        // STEP 0: PC=0010 IR=....
        // STEP 1: ...
        // You can start with final-state tests first; trace is optional.
        throw new UnsupportedOperationException("expected.trace not implemented yet. Use expected.final for now.");
    }

    private static Integer firstInstructionAddress(List<Assembler.AsmLine> lines) {
        // Prefer the first actual instruction (not DATA/LOC).
        // This prevents starting execution on DATA words like at LOC 6.
        for (Assembler.AsmLine al : lines) {
            if (al.address == null || al.op == null) continue;

            String op = al.op.toUpperCase(Locale.ROOT);

            // Skip directives / non-executable pseudo-ops
            if (op.equals("LOC") || op.equals("DATA")) continue;

            // Anything else here is an instruction mnemonic in your assembler.
            return al.address;
        }

        // Fallback: if no instruction found, fall back to first generated word
        for (Assembler.AsmLine al : lines) {
            if (al.address != null) return al.address;
        }

        return null;
    }

    // ---------- Helpers ----------

    private static Map<String, String> parseExpectedFile(Path p) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            // Full-line comments
            if (line.startsWith(";") || line.startsWith("#")) continue;

            // Strip inline comments
            int semi = line.indexOf(';');
            if (semi >= 0) line = line.substring(0, semi).trim();
            int hash = line.indexOf('#');
            if (hash >= 0) line = line.substring(0, hash).trim();

            if (line.isEmpty()) continue;

            int eq = line.indexOf('=');
            if (eq < 0) continue;

            String key = line.substring(0, eq).trim();
            String val = line.substring(eq + 1).trim();

            if (!key.isEmpty() && !val.isEmpty()) {
                out.put(key, val);
            }
        }
        return out;
    }

    private static void assertEqHex(String regName, int got, Map<String, String> expected) {
        if (!expected.containsKey(regName)) return; // allow partial expectations
        int exp = parseHex(expected.get(regName));
        if ((got & 0xFFFF) != (exp & 0xFFFF)) {
            throw new AssertionError("Mismatch " + regName + ": expected=" + hex4(exp) + " got=" + hex4(got));
        }
    }

    private static int parseHex(String s) {
        s = s.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        return Integer.parseInt(s, 16);
    }

    private static int parseBracketAddr(String key) {
        // MEM[000030] or MEM[0x0030]
        int l = key.indexOf('[');
        int r = key.indexOf(']');
        if (l < 0 || r < 0 || r <= l + 1) throw new IllegalArgumentException("Bad mem key: " + key);
        String inside = key.substring(l + 1, r).trim();
        if (inside.startsWith("0x") || inside.startsWith("0X")) inside = inside.substring(2);
        return Integer.parseInt(inside, 16);
    }

    private static String hex4(int v) {
        return String.format("%04X", v & 0xFFFF);
    }

    private static List<Path> listImmediateSubdirs(Path root) throws IOException {
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
            for (Path p : ds) if (Files.isDirectory(p)) out.add(p);
        }
        out.sort(Comparator.comparing(Path::toString));
        return out;
    }

    private static Integer firstGeneratedAddress(List<Assembler.AsmLine> lines) {
        for (Assembler.AsmLine al : lines) {
            if (al.address != null) return al.address;
        }
        return null;
    }
}