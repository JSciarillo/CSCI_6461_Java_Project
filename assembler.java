/**
 * Assembler.java
 * 
 * Processes incoming source files and converts them into two output
 *  files via a two pass process: "Listening.txt" and "Load.txt"
*/


import java.io.File;
import java.io.IOException;

public class Assembler {
    private String sourceFile = "SourceFile.txt";
    private String listeningOutput = "listeningOutput.txt";
    private String loadFile = "loadFile.txt";
    
    // Misc Instructions
    // Load/Store Instructions
    // Transfer Instructions
    // Arithemtic and Logical Instructions
    // I/O Operations
    // Floating Point Instructions/Vector Operations

    private void passOne(String sourceFile) throws IOException {
        System.out.println("Executing Pass One...");
        // 1. Set code location to 0

        // 2. Read a line of the file

        // 3. Use the split command to break the line into its parts 

        // 4. Process the line, if it is a label, add the label to a 
        //      dictionary with the code location. Process the rest of 
        //      the line (it could be blank, if so no code is generated). 
        //      Check for errors in the code. 

        // 5. If code or data was generated increment the code location 
        //      and go to step 2 until termination. 
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

    public void assemble(String sourceFile) {
        this.sourceFile = sourceFile;
        try {
            passOne(sourceFile);
            passTwo(sourceFile);
        } catch (IOException e) {
            System.out.println("Error during assembly: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("Running tests...");
        // Assembler logic here
    }
}