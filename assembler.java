/**
 * Assembler.java
 * 
 * Processes incoming source files and converts them into two output
 *  files via a two pass process: "Listing.txt" and "Load.txt"
*/

import java.io.*;
import java.util.*;
import static java.util.Map.entry;

public class Assembler {

    private static final String LISTING_FMT = "%-6s  %-6s  %s%n";
    private static final String LOAD_FMT = "%06o  %06o%n";

    public AssemblerResult assemble(File sourceFile, File listingFile, File loadFile) throws IOException {
        List<String> rawLines = readAllLines(sourceFile);

        Pass1Result p1 = passOne(rawLines);
        Pass2Result p2 = passTwo(p1.lines, p1.symtab, listingFile, loadFile);

        List<String> allErrors = new ArrayList<>();
        allErrors.addAll(p1.errors);
        allErrors.addAll(p2.errors);

        return new AssemblerResult(
            sourceFile,
            listingFile,
            loadFile,
            p1.symtab,
            p1.lines,
            allErrors,
            allErrors.isEmpty()
        );
    }

    // CLI entry-point
    public static void main(String[] args) {
        try {
            File src = new File(args.length > 0 ? args[0] : "source_file.txt");
            File listing = new File("Listing.txt");
            File load = new File("Load.txt");

            Assembler asm = new Assembler();
            AssemblerResult result = asm.assemble(src, listing, load);

            System.out.println("Assemble complete. success=" + result.success);
            if (!result.errors.isEmpty()) {
                System.out.println("Errors:");
                for (String e : result.errors) System.out.println("  " + e);
            }
        } catch (IOException e) {
            System.out.println("I/O error during assembly: " + e.getMessage());
        }
    }

    static class AsmLine {
        final int lineNo;
        final String raw;

        String comment;
        String label;
        String op;
        List<String> operands = List.of();
        Integer address;

        AsmLine(int lineNo, String raw) {
            this.lineNo = lineNo;
            this.raw = raw;
        }

        boolean isBlankOrCommentOnly() {
            return op == null;
        }

        boolean generatesWord() {
            return address != null;
        }
    }

    static class AssemblerResult {
        final File sourceFile;
        final File listingFile;
        final File loadFile;
        final Map<String, Integer> symtab;
        final List<AsmLine> lines;
        final List<String> errors;
        final boolean success;

        AssemblerResult(File sourceFile, File listingFile, File loadFile, Map<String, Integer> symtab, List<AsmLine> lines, List<String> errors, boolean success) {
            this.sourceFile = sourceFile;
            this.listingFile = listingFile;
            this.loadFile = loadFile;
            this.symtab = symtab;
            this.lines = lines;
            this.errors = errors;
            this.success = success;
        }
    }

    /**
     * CSCI6461 opcodes (octal in spec)
     * Store as integers; shifting into the top 6 bits uses (opcode << 10)
     */
    private static final Map<String, Integer> OPCODE = Map.ofEntries(
        // Misc
        entry("HLT", 0),
        entry("TRAP", 030),

        // Load/Store
        entry("LDR", 01),
        entry("STR", 02),
        entry("LDA", 03),
        entry("LDX", 041),
        entry("STX", 042),

        // Transfer
        entry("JZ", 010),
        entry("JNE", 011),
        entry("JCC", 012),
        entry("JMA", 013),
        entry("JSR", 014),
        entry("RFS", 015),
        entry("SOB", 016),
        entry("JGE", 017),

        // Arithmetic & Logical
        entry("AMR", 04),
        entry("SMR", 05),
        entry("AIR", 06),
        entry("SIR", 07),
        entry("MLT", 070),
        entry("DVD", 071),
        entry("TRR", 072),
        entry("AND", 073),
        entry("ORR", 074),
        entry("NOT", 075),
        entry("SRC", 031),
        entry("RRC", 032),

        // I/O
        entry("IN", 061),
        entry("OUT", 062),
        entry("CHK", 063),

        // Floating Point
        entry("FADD", 033),
        entry("FSUB", 034),
        entry("VADD", 035),
        entry("VSUB", 036),
        entry("CNVRT", 037),
        entry("LDFR", 050),
        entry("STFR", 051)
    );

    private static List<String> readAllLines(File f) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            for (String s; (s = br.readLine()) != null; ) {
                lines.add(s);
            }
        }
        return lines;
    }

    /**
     * Parse a raw source line into:
     *  - label (optional, ends with ':')
     *  - op (directive or instruction) (optional)
     *  - operands split by commas, with whitespace trimmed
     *  - comment (optional, after ';')
     * 
     * Handles:
     *  - blank lines
     *  - comment-only lines
     *  - operand spacing like: LDR 3,0,10,1
     */
    private static AsmLine parseLine(int lineNo, String raw) {
        AsmLine al = new AsmLine(lineNo, raw);

        String code = raw;
        int semi = code.indexOf(';');
        if (semi >= 0) {
            al.comment = code.substring(semi + 1).trim();
            code = code.substring(0, semi);
        }
        code = code.trim();
        if (code.isEmpty()) return al; // blank/comment-only

        // Split on whitespace first to find label/op
        String[] ws = code.split("\\s+");
        int i = 0;

        // Optional label 
        if (ws[i].endsWith(":")) {
            al.label = ws[i].substring(0, ws[i].length() - 1);
            i++;
            if (i >= ws.length) return al; // label-only line
        }

        // Op
        al.op = ws[i].toUpperCase(Locale.ROOT);
        i++;

        // Remaining text may include commas/spaces; reconstruct from original code
        // We rebuild from ws parts rather than slicing raw indicies to keep it simple.
        if (i < ws.length) {
            String rest = String.join(" ", Arrays.copyOfRange(ws, i, ws.length)).trim();
            al.operands = splitOperands(rest);
        } else {
            al.operands = List.of();
        }
        return al;
    }

    private static List<String> splitOperands(String operandText) {
        if (operandText == null || operandText.isBlank()) return List.of();
        String[] parts = operandText.split(",");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    // ----------------------------
    // Pass 1: build symbol table + assign addresses
    // ----------------------------

    static class Pass1Result {
        final List<AsmLine> lines;
        final Map<String, Integer> symtab;
        final List<String> errors;

        Pass1Result(List<AsmLine> lines, Map<String, Integer> symtab, List<String> errors) {
            this.lines = lines;
            this.symtab = symtab;
            this.errors = errors;
        }
    }

    private Pass1Result passOne(List<String> rawLines) {        
        int lc = 0; // Set code location to 0
        Map<String, Integer> symtab = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        List<AsmLine> lines = new ArrayList<>();

        for (int idx = 0; idx < rawLines.size(); idx++) {
            int lineNo = idx + 1;
            AsmLine al = parseLine(lineNo, rawLines.get(idx));
            lines.add(al);

            if (al.isBlankOrCommentOnly()) continue;

            // Label: define at current LC (even if the rest of line is LOC or blank)
            if (al.label != null) {
                if (symtab.containsKey(al.label)) {
                    errors.add("Line " + lineNo + ": Duplicate label '" + al.label + "'");
                } else {
                    symtab.put(al.label, lc);
                }
            }

            if (al.op == null) continue;

            // Directives
            if (al.op.equals("LOC")) {
                if (al.operands.size() != 1) {
                    errors.add("Line " + lineNo + ": LOC requires exactly 1 decimal operand");
                    continue;
                }
                Integer newLc = parseDecimalInt(al.operands.get(0));
                if (newLc == null) {
                    errors.add("Line " + lineNo + ": LOC operand must be decimal, got '" + al.operands.get(0) + "'");
                    continue;
                }
                lc = newLc;
                continue;
            }

            if (al.op.equals("DATA")) {
                // DATA allocates one word
                al.address = lc;
                lc += 1;
                // operand validity checked in pass2 (where we can resolve labels)
                continue;
            }

            // Instructions
            if (OPCODE.containsKey(al.op)) {
                al.address = lc;
                lc += 1;
                continue;
            }

            errors.add("Line " + lineNo + ": Unknown opcode/directive '" + al.op + "'");
        }

        return new Pass1Result(lines, symtab, errors);
    }

    // ----------------------------
    // Pass 2: encode + write outputs
    // ----------------------------

    static class Pass2Result {
        final List<String> errors;
        Pass2Result(List<String> errors) { this.errors = errors; }
    }

    private Pass2Result passTwo(List<AsmLine> lines,
                                Map<String, Integer> symtab,
                                File listingFile,
                                File loadFile) throws IOException {

        List<String> errors = new ArrayList<>();

        try (BufferedWriter list = new BufferedWriter(new FileWriter(listingFile));
             BufferedWriter load = new BufferedWriter(new FileWriter(loadFile))) {

            for (AsmLine al : lines) {

                // No generated word => blank address/word columns in listing
                if (!al.generatesWord()) {
                    list.write(String.format(LISTING_FMT, "", "", al.raw));
                    continue;
                }

                int word = 0;
                List<String> lineErrors = new ArrayList<>();

                if ("DATA".equals(al.op)) {
                    if (al.operands.size() != 1) {
                        lineErrors.add("DATA requires exactly 1 operand");
                        word = 0;
                    } else {
                        ResolveResult rr = resolveValue(al.operands.get(0), symtab);
                        if (!rr.ok) {
                            lineErrors.add(rr.error);
                            word = 0;
                        } else {
                            word = rr.value;
                        }
                    }
                } else {
                    EncodeResult er = encodeInstruction(al.op, al.operands, symtab);
                    word = er.word;
                    lineErrors.addAll(er.errors);
                }

                String addrField = (al.address == null) ? "" : fmtOctal6(al.address);
                String wordField = (al.address == null) ? "" : fmtOctal6(word);

                // Listing line: include errors at end (grader-friendly)
                if (lineErrors.isEmpty()) {
                    list.write(String.format(LISTING_FMT, addrField, wordField, al.raw));
                } else {
                    String errText = String.join(" | ", lineErrors);
                    list.write(String.format(LISTING_FMT, addrField, wordField, al.raw + " ; ERROR: " + errText));
                    errors.add("Line " + al.lineNo + ": " + errText);
                }

                // Load file: include word even if errors? Usually no.
                // Policy: only output to load if no errors for that line.
                if (lineErrors.isEmpty()) {
                    load.write(String.format(LOAD_FMT, al.address, word & 0xFFFF));
                }
            }
        }

        return new Pass2Result(errors);
    }

    // ----------------------------
    // Encoding
    // ----------------------------

    static class EncodeResult {
        final int word;
        final List<String> errors;
        EncodeResult(int word, List<String> errors) {
            this.word = word & 0xFFFF;
            this.errors = errors;
        }
    }

    static class ResolveResult {
        final boolean ok;
        final int value;
        final String error;
        ResolveResult(boolean ok, int value, String error) {
            this.ok = ok;
            this.value = value;
            this.error = error;
        }
        static ResolveResult ok(int v) { return new ResolveResult(true, v, null); }
        static ResolveResult err(String e) { return new ResolveResult(false, 0, e); }
    }

    private EncodeResult encodeInstruction(String op, List<String> operands, Map<String, Integer> symtab) {
        Integer opcode = OPCODE.get(op);
        if (opcode == null) return new EncodeResult(0, List.of("Unknown opcode '" + op + "'"));

        List<String> errs = new ArrayList<>();
        int word = 0;

        switch (op) {
            case "HLT": {
                word = 0;
                break;
            }

            case "TRAP": {
                if (operands.size() != 1) {
                    errs.add("TRAP requires 1 operand (code 0..15)");
                    break;
                }
                Integer code = parseDecimalInt(operands.get(0));
                if (code == null) { errs.add("TRAP code must be decimal"); break; }
                if (code < 0 || code > 15) errs.add("TRAP code out of range (0..15)");
                word = (opcode << 10) | (code & 0xF);
                break;
            }

            // r,x,address[,I] (JCC: first is cc not r)
            case "LDR": case "STR": case "LDA":
            case "AMR": case "SMR":
            case "JZ":  case "JNE": case "JGE": case "SOB":
            case "JCC": {
                int expectedMin = 3;
                if (operands.size() < expectedMin || operands.size() > 4) {
                    errs.add(op + " expects 3 operands (r/cc,x,address) plus optional I");
                    break;
                }

                // r or cc
                Integer rOrCc = parseDecimalInt(operands.get(0));
                Integer ix = parseDecimalInt(operands.get(1));
                ResolveResult addrR = resolveValue(operands.get(2), symtab);
                Integer iBit = (operands.size() == 4) ? parseDecimalInt(operands.get(3)) : 0;

                if (rOrCc == null) errs.add(op + ": first operand must be decimal");
                if (ix == null) errs.add(op + ": IX must be decimal");
                if (!addrR.ok) errs.add(op + ": " + addrR.error);
                if (iBit == null) errs.add(op + ": I must be decimal 0/1");

                if (!errs.isEmpty()) break;

                // Validate ranges
                if (op.equals("JCC")) {
                    if (rOrCc < 0 || rOrCc > 3) errs.add("cc out of range (0..3)");
                } else {
                    if (rOrCc < 0 || rOrCc > 3) errs.add("r out of range (0..3)");
                }
                if (ix < 0 || ix > 3) errs.add("IX out of range (0..3)");
                if (iBit < 0 || iBit > 1) errs.add("I must be 0 or 1");
                if (addrR.value < 0 || addrR.value > 31) errs.add("Address field out of range (0..31): " + addrR.value);

                if (!errs.isEmpty()) break;

                word = (opcode << 10)
                        | ((rOrCc & 0x3) << 8)
                        | ((ix & 0x3) << 6)
                        | ((iBit & 0x1) << 5)
                        | (addrR.value & 0x1F);
                break;
            }

            // x,address[,I] (r ignored for JMA/JSR in ISA)
            case "LDX": case "STX":
            case "JMA": case "JSR": {
                if (operands.size() < 2 || operands.size() > 3) {
                    errs.add(op + " expects 2 operands (x,address) plus optional I");
                    break;
                }

                Integer ix = parseDecimalInt(operands.get(0));
                ResolveResult addrR = resolveValue(operands.get(1), symtab);
                Integer iBit = (operands.size() == 3) ? parseDecimalInt(operands.get(2)) : 0;

                if (ix == null) errs.add(op + ": x must be decimal");
                if (!addrR.ok) errs.add(op + ": " + addrR.error);
                if (iBit == null) errs.add(op + ": I must be decimal 0/1");
                if (!errs.isEmpty()) break;

                if (ix < 0 || ix > 3) errs.add("IX out of range (0..3)");
                if (iBit < 0 || iBit > 1) errs.add("I must be 0 or 1");
                if (addrR.value < 0 || addrR.value > 31) errs.add("Address field out of range (0..31): " + addrR.value);
                if (!errs.isEmpty()) break;

                word = (opcode << 10)
                        | ((ix & 0x3) << 6)
                        | ((iBit & 0x1) << 5)
                        | (addrR.value & 0x1F);
                break;
            }

            case "RFS": {
                if (operands.size() != 1) {
                    errs.add("RFS expects 1 operand (immed)");
                    break;
                }
                Integer immed = parseDecimalInt(operands.get(0));
                if (immed == null) { errs.add("RFS immed must be decimal"); break; }
                if (immed < 0 || immed > 31) errs.add("RFS immed out of range (0..31)");
                if (!errs.isEmpty()) break;

                word = (opcode << 10) | (immed & 0x1F);
                break;
            }

            case "AIR": case "SIR": {
                if (operands.size() != 2) {
                    errs.add(op + " expects 2 operands (r,immed)");
                    break;
                }
                Integer r = parseDecimalInt(operands.get(0));
                Integer immed = parseDecimalInt(operands.get(1));
                if (r == null) errs.add("r must be decimal");
                if (immed == null) errs.add("immed must be decimal");
                if (!errs.isEmpty()) break;

                if (r < 0 || r > 3) errs.add("r out of range (0..3)");
                if (immed < 0 || immed > 31) errs.add("immed out of range (0..31)");
                if (!errs.isEmpty()) break;

                word = (opcode << 10) | ((r & 0x3) << 8) | (immed & 0x1F);
                break;
            }

            // rx,ry register-to-register ops
            case "MLT": case "DVD":
            case "TRR": case "AND": case "ORR": {
                if (operands.size() != 2) {
                    errs.add(op + " expects 2 operands (rx,ry)");
                    break;
                }
                Integer rx = parseDecimalInt(operands.get(0));
                Integer ry = parseDecimalInt(operands.get(1));
                if (rx == null) errs.add("rx must be decimal");
                if (ry == null) errs.add("ry must be decimal");
                if (!errs.isEmpty()) break;

                if (rx < 0 || rx > 3) errs.add("rx out of range (0..3)");
                if (ry < 0 || ry > 3) errs.add("ry out of range (0..3)");

                // Spec constraints for MLT/DVD: rx must be 0 or 2; ry must be 0 or 2
                if (op.equals("MLT") || op.equals("DVD")) {
                    if (!(rx == 0 || rx == 2)) errs.add(op + ": rx must be 0 or 2");
                    if (!(ry == 0 || ry == 2)) errs.add(op + ": ry must be 0 or 2");
                }

                if (!errs.isEmpty()) break;

                word = (opcode << 10) | ((rx & 0x3) << 8) | ((ry & 0x3) << 6);
                break;
            }

            case "NOT": {
                if (operands.size() != 1) {
                    errs.add("NOT expects 1 operand (r)");
                    break;
                }
                Integer r = parseDecimalInt(operands.get(0));
                if (r == null) { errs.add("r must be decimal"); break; }
                if (r < 0 || r > 3) errs.add("r out of range (0..3)");
                if (!errs.isEmpty()) break;

                word = (opcode << 10) | ((r & 0x3) << 8);
                break;
            }

            // Shift/Rotate: r,count,L/R,A/L
            case "SRC": case "RRC": {
                if (operands.size() != 4) {
                    errs.add(op + " expects 4 operands (r,count,L/R,A/L)");
                    break;
                }
                Integer r = parseDecimalInt(operands.get(0));
                Integer count = parseDecimalInt(operands.get(1));
                Integer lr = parseDecimalInt(operands.get(2));
                Integer al = parseDecimalInt(operands.get(3));

                if (r == null) errs.add("r must be decimal");
                if (count == null) errs.add("count must be decimal");
                if (lr == null) errs.add("L/R must be decimal 0/1");
                if (al == null) errs.add("A/L must be decimal 0/1");
                if (!errs.isEmpty()) break;

                if (r < 0 || r > 3) errs.add("r out of range (0..3)");
                if (count < 0 || count > 15) errs.add("count out of range (0..15)");
                if (lr < 0 || lr > 1) errs.add("L/R must be 0 or 1");
                if (al < 0 || al > 1) errs.add("A/L must be 0 or 1");
                if (!errs.isEmpty()) break;

                // Encoding (as you had): opcode[15..10] r[9..8] A/L[7] L/R[6] count[3..0]
                word = (opcode << 10)
                        | ((r & 0x3) << 8)
                        | ((al & 0x1) << 7)
                        | ((lr & 0x1) << 6)
                        | (count & 0xF);
                break;
            }

            // I/O: r,devid
            case "IN": case "OUT": case "CHK": {
                if (operands.size() != 2) {
                    errs.add(op + " expects 2 operands (r,devid)");
                    break;
                }
                Integer r = parseDecimalInt(operands.get(0));
                Integer devid = parseDecimalInt(operands.get(1));
                if (r == null) errs.add("r must be decimal");
                if (devid == null) errs.add("devid must be decimal");
                if (!errs.isEmpty()) break;

                if (r < 0 || r > 3) errs.add("r out of range (0..3)");
                if (devid < 0 || devid > 31) errs.add("devid out of range (0..31)");
                if (!errs.isEmpty()) break;

                word = (opcode << 10) | ((r & 0x3) << 8) | (devid & 0x1F);
                break;
            }

            default: {
                // If you add FP/vector later, copy the r,x,address[,I] pattern and validate FR ranges.
                errs.add("Encoding not implemented for " + op);
                break;
            }
        }

        return new EncodeResult(word, errs);
    }

    // ----------------------------
    // Resolution + parsing helpers
    // ----------------------------

    private static ResolveResult resolveValue(String token, Map<String, Integer> symtab) {
        Integer dec = parseDecimalInt(token);
        if (dec != null) return ResolveResult.ok(dec);
        Integer addr = symtab.get(token);
        if (addr != null) return ResolveResult.ok(addr);
        return ResolveResult.err("Undefined symbol or invalid decimal '" + token + "'");
    }

    private static Integer parseDecimalInt(String s) {
        if (s == null) return null;
        try {
            // Spec says LOC/DATA operands are decimal in examples.
            // If you later want octal literals, add detection like "0o" or leading "0" policy.
            return Integer.parseInt(s.trim(), 10);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String fmtOctal6(int value) {
        return String.format("%06o", value & 0xFFFF);
    }

    private static String blank6() {
        return "      ";
    }
}
