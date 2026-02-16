/**
 * Assembler.java
 * 
 * Processes incoming source files and converts them into two output
 *  files via a two pass process: "Listing.txt" and "Load.txt"
*/


import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class Assembler {
    private int lc = 0; //code location counter
    private final Map<String, Integer> symtab = new HashMap<String, Integer>();

    private final List<String> sourceCode = new ArrayList<>();
    private final List<Integer> addrByLine = new ArrayList<>();
    private final List<String> passOneErrors = new ArrayList<>();

    private static final Set<String> ISA_OPCODES = new HashSet<>(Arrays.asList(
        // Miscellaneous Instructions
        "HLT", "TRAP",
        // Load/Store Instructions
        "LDR", "STR", "LDA", "LDX", "STX",
        // Transfer Instructions
        "JZ", "JNE", "JCC", "JMA", "JSR", "RFS", "SOB", "JGE",
        // Arithmetic & Logical Instructions
        "AMR", "SMR", "AIR", "SIR", "MLT", "DVD", "TRR", "AND", "ORR", "NOT", "SRC", "RRC",
        // I/O Operations
        "IN", "OUT", "CHK",
        // Floating Point Instructions
        "FADD", "FSUB", "VADD", "VSUB", "CNVRT", "LDFR", "STFR"
    ));

    /**
     * Pass One
     * 1. Set code location to 0
     * 2. Read a line of the file
     * 3. Use the split command to break the line into its parts
     * 4. Process the line,
     *      - If its a label, add the label to a dictionary with the code location.
     *      - Process the rest of the line
     *      - Check for errors in the code
     * 5. If code or data was generated, increment code location and go to step 2
     *      until termination
     * 
     * @param sourceFile
     * @throws IOException
     */
    private void passOne(String sourceFile) throws IOException {        
        lc = 0; // Set code location to 0
        sourceCode.clear();
        addrByLine.clear();
        passOneErrors.clear();
        symtab.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            String raw;
            int lineNo = 0;

            // Read a line of the file
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

                // Use split to break the line into parts
                String[] tokens = line.split("\\s+");
                int i = 0;

                // If it is a label, add to dictionary with code location
                if(tokens[i].endsWith(":")) {
                    String label = tokens[i].substring(0, tokens[i].length() - 1);
                    String key = label;

                    if (symtab.containsKey(key)) {
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

    /**
     * Pass Two
     * 1. Set code location to 0
     * 2. Read a line of the file
     * 3. Use the split command to break the line into parts
     * 4. Convert the code according to the second field.
     * 5. Add line to listing file and to load file
     * 6. If code or data generated, increment the code counter, and go to step2 until termination
     *
     * @param listingFile
     * @param loadFile
     * @throws IOException
     */
    private void passTwo(String listingFile, String loadFile) throws IOException {
        // 1. Set code location to 0
        BufferedWriter list = new BufferedWriter(new FileWriter(listingFile));
        BufferedWriter load = new BufferedWriter(new FileWriter(loadFile));

        // 2. Read a line of the file 
        for (int i = 0; i < sourceCode.size(); i++) {
            String raw = sourceCode.get(i);
            Integer addr = addrByLine.get(i);

            //If there is no address, skip processing, write line to listing file
            if (addr == null){
                list.write("      " + raw + "\n");
                continue;
            }
            
            String line = raw;
            int semi = line.indexOf(';');
            if (semi >= 0) line = line.substring(0, semi);
            line = line.trim();

            // 3.Use the split command to break the line into parts 
            String[] tokens = line.split("\\s+");
            //token index
            int t = 0;

            //Skip if : is present
            if (tokens[t].endsWith(":")) {
                t++;
            }

            //Retrieve opcode
            String op = tokens[t].toUpperCase(Locale.ROOT);
            t++;

            // 4. Convert the code according to the second field.
            int word = 0;
            
            // Data
            if(op.equals("DATA")) {
                String arg = tokens[t];
                //String octalArg = null;

                // If arg is addr, normally convert
                // if(isInteger(arg)){
                //     octalArg = toOctal(Integer.parseInt(arg));
                // }

                // If not, check symtab for lable location
                if(symtab.containsKey(arg)){
                    word = symtab.get(arg);
                }
                else {
                    word = Integer.parseInt(arg);
                }
            }

            else {
                String operands = t < tokens.length ? tokens[t] : "";
                word = encodeInstruction(op, operands);
            }

            // 5. Add line to listing file and to load file. 
            String octalAddr = String.format("%06o", addr);
            String octalWord = String.format("%06o", word & 0xFFFF);

            list.write(octalAddr + "  " + octalWord + "  " + raw + "\n");
            load.write(octalAddr + "  " + octalWord + "\n");
            }

        list.close();
        load.close();
        }

    /**
     * Helper methods to encode instructions, get opcodes, and convert to octal
     */

    /**
     * Returns the opcode for a given instruction mnemonic
     * @param op
     * @return the integer opcode corresponding to the instruction mnemonic, or -1 if the mnemonic is invalid
     */
    private int getOpcode(String op) {
        switch(op) {
            case "HLT": return 00;
            case "TRAP": return 030;
            case "LDR": return 01;
            case "STR": return 02;
            case "LDA": return 03;
            case "LDX": return 041;
            case "STX": return 042;
            case "JZ": return 010;
            case "JNE": return 011;
            case "JCC": return 012;
            case "JMA": return 013;
            case "JSR": return 014;
            case "RFS": return 015;
            case "SOB": return 016;
            case "JGE": return 017;
            case "AMR": return 04;
            case "SMR": return 05;
            case "AIR": return 06;
            case "SIR": return 07;
            case "MLT": return 070;
            case "DVD": return 071;
            case "TRR": return 072;
            case "AND": return 073;
            case "ORR": return 074;
            case "NOT": return 075;
            case "SRC": return 031;
            case "RRC": return 032;
            case "IN": return 061;
            case "OUT": return 062;
            case "CHK": return 063;
            case "FADD": return 033;
            case "FSUB": return 034;
            case "VADD": return 035;
            case "VSUB": return 036;
            case "CNVRT": return 037;
            case "LDFR": return 050;
            case "STFR": return 051;
            default: return -1;
        }
    }

    /**
     * Encodes an instruction into its 16-bit machine code representation based on the opcode and operands
     * @param op the instruction mnemonic
     * @param operands the string containing the operands for the instruction, separated by commas
     * @return an integer representing the encoded machine code for the instruction
     */
    private int encodeInstruction(String op, String operands) {
        int opcode = getOpcode(op);

        String[] parts = operands.isEmpty() ? new String[0] : operands.split(",");
        
        switch (op){
            case "HLT":
                return 0;

            case "LDR":
            case "STR":
            case "LDA":
            case "AMR":
            case "SMR":
            case "JZ":
            case "JNE":
            case "JCC":
            case "JGE":
            case "SOB": {
                int r = Integer.parseInt(parts[0].trim());
                int x = Integer.parseInt(parts[1].trim());
                int addr = Integer.parseInt(parts[2].trim());
                int indx = (parts.length > 3 && parts[3].trim().equals("1")) ? 1 : 0;
                return (opcode << 10) | (r << 8) | (x << 6) | (indx << 5) | (addr & 0x1F);
            }
            case "LDX":
            case "STX":
            case "JMA":
            case "JSR": {
                int ix = Integer.parseInt(parts[0].trim());
                int addr = Integer.parseInt(parts[1].trim());
                int indx = (parts.length > 2 && parts[2].trim().equals("1")) ? 1 : 0;
                return (opcode << 10) | (ix << 6) | (indx << 5) | (addr & 0x1F);
            }

            case "RFS": {
                int immediate = parts.length > 0 ? Integer.parseInt(parts[0].trim()) : 0;
                return (opcode << 10) | (immediate & 0x1F);
            }

            case "AIR":
            case "SIR": {
                int r = Integer.parseInt(parts[0].trim());
                int immediate = Integer.parseInt(parts[1].trim());
                return (opcode << 10) | (r << 8) | (immediate & 0x1F);
            }

            case "MLT":
            case "DVD":
            case "TRR":
            case "AND":
            case "ORR":
                int rx = Integer.parseInt(parts[0].trim());
                int ry = Integer.parseInt(parts[1].trim());
                return (opcode << 10) | (rx << 8) | (ry << 6);

            case "NOT":
                int r = Integer.parseInt(parts[0].trim());
                return (opcode << 10) | (r << 8);

            case "SRC":
            case "RRC": {
                int x = Integer.parseInt(parts[0].trim());
                int count = Integer.parseInt(parts[1].trim());
                int lr = Integer.parseInt(parts[2].trim());
                int al = Integer.parseInt(parts[3].trim());
                return (opcode << 10) | (x << 8) | (al << 7) | (lr << 6) |(count & 0xF);
            }

            case "TRAP": {
                int code = Integer.parseInt(parts[0].trim());
                return (opcode << 10) | (code & 0xF);

            }
            
            case "FADD":
            case "FSUB":
            case "VADD":
            case "VSUB":
            case "CNVRT": 
            case "LDFR":
            case "STFR": {
                int ri = Integer.parseInt(parts[0].trim());
                int ix = Integer.parseInt(parts[1].trim());
                int addr = Integer.parseInt(parts[2].trim());
                int indx = (parts.length > 3 && parts[3].trim().equals("1")) ? 1 : 0;
                return (opcode << 10) | (ri << 8) | (ix << 6) | (indx << 5) | (addr & 0x1F);
            }

            case "IN":
            case "OUT":
            case "CHK": {
                int rj = Integer.parseInt(parts[0].trim());
                int devid = Integer.parseInt(parts[1].trim());
                return (opcode << 10) | (rj << 8) | (devid & 0x1F);
            }

            default:
                System.out.println("Opcode Invalid: " + op);
                return 0;
        }
    }

    /**
     * Checks if a string can be parsed as an integer
     * @param str
     * @return boolean indicating if the string is an integer
     */
    public static boolean isInteger(String str) {
        //For null input
        if (str == null) 
            return false;{ 
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Converts an integer to a 6-digit octal string
     * @param num
     * @return a string representing the octal value of the input integer, padded to 6 digits
     */
    public String toOctal(Integer num){
        return String.format("%06d", Integer.parseInt(Integer.toOctalString(num)));
    }

    /**
     * Debug method to print the symbol table, addresses by line, and any errors from pass one
     */
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
        System.out.println("\nPASS1 Errors:");
        for (String err : passOneErrors) System.out.println("  " + err);
    }

    /**
     * Main assembly method that runs pass one and pass two, and handles any IO exceptions
     */
    public void assemble(String sourceFile) {
        try {
            passOne(sourceFile);
            // DEBUG
            debugPrintPassOne();
            passTwo("Listing.txt", "Load.txt");
        } catch (IOException e) {
            System.out.println("Error during assembly: " + e.getMessage());
        }
    }

    // Main method to run the assembler with a source file
    public static void main(String[] args) {
        System.out.println("Running tests...");
        // Assembler logic here
        Assembler assembler = new Assembler();
        assembler.assemble("source_file.txt");
        //Simple test case
        // assembler.assemble("testfile.txt");
    }
}
