/**
 * Assembler.java
 * 
 * Processes incoming source files and converts them into two output
 *  files via a two pass process: "Listening.txt" and "Load.txt"
*/


import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class Assembler {
    private int lc = 0; // code location counter
    private final Map<String, Integer> symtab = new HashMap<String, Integer>();

    private final List<String> sourceCode = new ArrayList<>();
    private final List<Integer> addrByLine = new ArrayList<>();
    private final List<String> passOneErrors = new ArrayList<>();

    private static final Set<String> ISA_OPCODES = new HashSet<>(Arrays.asList(
        // Mescellaneous Instructions
        "HLT", "TRAP",
        // Load/Store Instructions
        "LDR", "STR", "LDA", "LDX", "STX",
        // Transfer Instructions
        "JZ", "JNE", "JCC", "JMA", "JSR", "RFS", "SOB", "JGE",
        // Arithmetic and Logical Instructions
        "AMR", "SMR", "AIR", "SIR", "MLT", "DVD", "TRR", "AND", "ORR", "NOT", "SRC", "RRC",
        // I/O Operations
        "IN", "OUT", "CHK",
        // Floating Point Instructions
        "FADD", "FSUB", "VADD", "VSUB", "CNVRT", "LDFR", "STFR"
    ));

    private void passOne(String sourceFile) throws IOException {        
        lc = 0; // 1. Set code location to 0
        sourceCode.clear();
        addrByLine.clear();
        passOneErrors.clear();
        symtab.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            String raw;
            int lineNo = 0;

            // 2. Read a line of the file
            while ((raw = reader.readLine()) != null) {
                lineNo++;
                sourceCode.add(raw);

                // Strip comments
                String line = raw;
                int semi = line.indexOf(';');
                if (semi >= 0) line = line.substring(0, semi);
                line = line.trim();

                if (line.isEmpty()) { 
                    addrByLine.add(null);
                    continue; 
                }

                // 3. Use split to break the line into parts
                String[] tokens = line.split("\\s+");
                int i = 0;

                // 4a. If it is a label, add to dictionary with code location
                if(tokens[i].endsWith(":")) {
                    String label = tokens[i].substring(0, tokens[i].length() - 1);
                    String key = label.toUpperCase(Locale.ROOT);

                    if (!key.matches("[A-Z_][A-Z0-9_]*")) {
                        passOneErrors.add("Line " + lineNo + ": Invalid label '" + label + "'");
                    } else if (symtab.containsKey(key)) {
                        passOneErrors.add("Line " + lineNo + ": Duplicate label '" + label + "'");
                    } else {
                        symtab.put(key, lc);
                    }

                    i++;
                }

                if (i >= tokens.length) {
                    addrByLine.add(null);
                    continue;
                }

                String op = tokens[i].toUpperCase(Locale.ROOT);

                // LOC n => sets LC, no word generated
                if(op.equals("LOC")) {
                    if (i + 1 >= tokens.length) {
                        passOneErrors.add("Line " + lineNo + ": LOC missing operand");
                        addrByLine.add(null);
                        continue;
                    }
                    String nStr = tokens[i + 1];
                    try {
                        int newLc = Integer.parseInt(nStr, 10);
                        lc = newLc;
                    } catch (NumberFormatException ex) {
                        passOneErrors.add("Line " + lineNo + ": LOC operand must be decimal, got '" + nStr + "'");
                    }
                    addrByLine.add(null);
                    continue;
                }

                // Data x => allocates 1 word at LC, then LC++
                if (op.equals("DATA")) {
                    addrByLine.add(lc);
                    
                    if (i + 1 >= tokens.length) {
                        passOneErrors.add("Line " + lineNo + ": Data missing operand");
                    }

                    lc += 1;
                    continue;
                }

                if (ISA_OPCODES.contains(op)) {
                    addrByLine.add(lc);
                    lc += 1;
                    continue;
                }

                // Unknown operation
                passOneErrors.add("Line " + lineNo + ": Unknown opcode/directive '" + op + "'");
                addrByLine.add(null);
            }
        }
    }

    private void passTwo(String sourceFile) throws IOException {
        System.out.println("Executing Pass Two...");
        // 1. Set code location to 0

        // 2. Read a line of the file 

        // 3. Use the split command to break the line into it parts 

        // 4. Convert the code according to the second field. 

        // 5. Add line to listing file and to load file. 

        // 6. If code or data generated, increment the code counter, and 
        //      go to step2 until termination. 
    }

    public void debugPrintPassOne() {
        System.out.println("SYMTAB:");
        for (var e : symtab.entrySet()) {
            System.out.println("  " + e.getKey() + " = " + e.getValue());
        }
        System.out.println("\nADDR BY LINE:");
        for (int i = 0; i < sourceCode.size(); i++) {
            System.out.printf("%3d  %6s  %s%n",
                    (i + 1),
                    (addrByLine.get(i) == null ? "------" : addrByLine.get(i).toString()),
                    sourceCode.get(i));
        }
        System.out.println("\nPASS1 ERRORS:");
        for (String err : passOneErrors) System.out.println("  " + err);
    }

    public void assemble(String sourceFile) {
        try {
            passOne(sourceFile);
            // DEBUG
            debugPrintPassOne();
            passTwo(sourceFile);
        } catch (IOException e) {
            System.out.println("Error during assembly: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("Running tests...");
        // Assembler logic here
        Assembler assembler = new Assembler();
        assembler.assemble("source_file.txt");
    }
}