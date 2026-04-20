package src;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Simulator.java
 *
 * Ties CPU + Memory + Cache together and provides front-panel style operations.
 * Main fixes:
 * - synchronized methods for safer GUI/run-thread interaction
 * - appendConsoleInput() instead of only replacing the whole buffer
 */
public final class Simulator {
    private static final int DEFAULT_CACHE_LINES = 16;

    private final CPU cpu;
    private final Memory mem;
    private final Cache cache;
    private final TraceLogger logger;

    private String lastCardReaderContents = "";

    public Simulator(int memSizeWords) {
        this.cpu = new CPU();
        this.mem = new Memory(memSizeWords);
        this.cache = new Cache(mem, DEFAULT_CACHE_LINES);

        try {
            this.logger = new TraceLogger("sim_trace.txt");
        } catch (IOException e) {
            throw new RuntimeException("Failed to open sim_trace.txt for logging", e);
        }

        cpu.setLogger(logger);
        logger.log("SYSTEM", "Simulator created with memoryWords=" + memSizeWords
                + " cacheLines=" + DEFAULT_CACHE_LINES);

        reset();
    }

    public Simulator() {
        this(2048);
    }

    public synchronized void reset() {
        logger.log("SIM", "Reset requested");

        cpu.reset();
        mem.clear();
        cache.reset();

        mem.write(1, 6);
        mem.write(6, 0);

        logger.log("SIM", "Reset complete: memory cleared, cache reset, fault handler initialized");
    }

    public synchronized void loadProgramFromFile(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("Program file not found: " + filename);
        }

        logger.log("SIM", "Loading program file: " + filename);

        int firstLoadedAddress = -1;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String line; (line = br.readLine()) != null;) {
                line = stripComment(line).trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (parts.length < 2) {
                    continue;
                }

                int addr = Integer.parseInt(parts[0], 8);
                int value = Integer.parseInt(parts[1], 8);

                mem.write(addr, value & 0xFFFF);
                logger.log("LOAD", String.format("Program word loaded: M[%04o] <- %06o", addr, value & 0xFFFF));

                if (firstLoadedAddress < 0) {
                    firstLoadedAddress = addr;
                }
            }
        }

        cache.reset();
        logger.log("SIM", "Cache reset after program load");

        if (firstLoadedAddress >= 0) {
            cpu.setPC(firstLoadedAddress);
            logger.log("SIM", String.format("Program load complete. PC set to %04o", firstLoadedAddress));
        }
    }

    public synchronized void loadTextFileIntoCardReader(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("Input text file not found: " + filename);
        }

        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (!first) {
                    sb.append('\n');
                }
                sb.append(line);
                first = false;
            }
        }

        lastCardReaderContents = sb.toString();
        cpu.setCardReaderInput(lastCardReaderContents);
        logger.log("SIM", "Card reader loaded from file: " + filename
                + " chars=" + lastCardReaderContents.length());
    }

    public synchronized void reloadCardReaderInput() {
        cpu.setCardReaderInput(lastCardReaderContents);
        logger.log("SIM", "Card reader input reloaded. chars=" + lastCardReaderContents.length());
    }

    public synchronized void loadTextFileIntoConsoleInput(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("Input text file not found: " + filename);
        }

        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (!first) {
                    sb.append('\n');
                }
                sb.append(line);
                first = false;
            }
        }

        cpu.setConsoleInput(sb.toString());
        logger.log("INPUT", "Console input loaded from file: " + filename
                + " chars=" + sb.length());
    }

    public synchronized void setConsoleInput(String input) {
        cpu.setConsoleInput(input);
        logger.log("INPUT", "Console input replaced with: [" + printable(input) + "]");
    }

    public synchronized void appendConsoleInput(String input) {
        cpu.appendConsoleInput(input);
        logger.log("INPUT", "Console input appended: [" + printable(input) + "]");
    }

    public synchronized void clearConsoleOutput() {
        cpu.clearConsoleOutput();
        logger.log("SIM", "Console output cleared");
    }

    public synchronized String getConsoleOutput() {
        return cpu.getConsoleOutput();
    }

    public synchronized void depositMemory(int address, int value) {
        mem.write(address, value & 0xFFFF);
        cache.reset();
        logger.log("PANEL", String.format("Deposit memory: M[%04o] <- %06o", address, value & 0xFFFF));
    }

    public synchronized int readMemory(int address) {
        return cache.read(address) & 0xFFFF;
    }

    public synchronized void setPC(int address) {
        cpu.setPC(address);
        logger.log("PANEL", String.format("PC manually set to %04o", address));
    }

    public synchronized void setRegister(int rIndex, int value) {
        cpu.setR(rIndex, value);
        logger.log("PANEL", String.format("R%d manually set to %06o", rIndex, value & 0xFFFF));
    }

    public synchronized void setIndexRegister(int ixIndex, int value) {
        cpu.setIX(ixIndex, value);
        logger.log("PANEL", String.format("X%d manually set to %06o", ixIndex, value & 0xFFFF));
    }

    public synchronized void singleStep() {
        if (!cpu.isHalted()) {
            logger.log("SIM", String.format("Single step requested at PC=%04o", cpu.getPC()));
            cpu.singleStep(cache);
        } else {
            logger.log("SIM", "Single step requested but CPU already halted");
        }
    }

    public synchronized void run(int maxSteps) {
        logger.log("SIM", "Run requested with maxSteps=" + maxSteps);

        int steps = 0;
        while (!cpu.isHalted() && steps < maxSteps) {
            cpu.singleStep(cache);
            steps++;
        }

        if (!cpu.isHalted()) {
            logger.log("SIM", "Run stopped after maxSteps=" + maxSteps + " (possible infinite loop)");
            System.out.println("Run stopped after maxSteps="
                    + maxSteps + " (possible infinite loop).");
        } else {
            logger.log("SIM", "Run halted normally after steps=" + steps);
        }
    }

    public CPU getCPU() {
        return cpu;
    }

    public Memory getMemory() {
        return mem;
    }

    public Cache getCache() {
        return cache;
    }

    public TraceLogger getLogger() {
        return logger;
    }

    public synchronized int getMemoryAtMAR() {
        return cache.read(cpu.getMAR()) & 0xFFFF;
    }

    private String stripComment(String line) {
        int idx = line.indexOf(';');
        if (idx >= 0) {
            return line.substring(0, idx);
        }
        return line;
    }

    public synchronized void setCardReaderInput(String input) {
        cpu.setCardReaderInput(input);
        logger.log("INPUT", "Card reader input replaced with: [" + printable(input) + "]");
    }

    private String printable(String s) {
        if (s == null) return "null";
        return s.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}