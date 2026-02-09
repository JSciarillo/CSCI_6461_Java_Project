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
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Assembler {
    private String sourceFile = "SourceFile.txt";
    private String listeningOutput = "listeningOutput.txt";
    private String loadFile = "loadFile.txt";
    private List<String> sourceCode = new ArrayList<String>();
    private Map<String, Integer> symbolTable = new HashMap<String, Integer>();
    private Integer curAddr = 0;

    private void passOne(String sourceFile) throws IOException {        
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
        
        // 1. Set code location to 0
        // 2. Read a line of the file
        while((line = reader.readLine()) != null) {
            // 3. Use the split command to break the line into its parts 
            sourceCode.add(line);
            String[] tokens = line.trim().split("\\s+");

            /**
             *  4. Process the line, if it is a label, add the label to a 
             *  dictionary with the code location. Process the rest of 
             *  the line (it could be blank, if so no code is generated). 
             */  
            if (tokens[0].endsWith(":")) {
                String label = tokens[0].substring(0, tokens[0].length() - 1);
                symbolTable.put(label, curAddr);
            }

            // 5. If code or data was generated increment the code location 
            //      and go to step 2 until termination. 
            curAddr++;
        }
        reader.close();
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
        Assembler assembler = new Assembler();
        assembler.assemble("source_file.txt");
    }
}