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

    private String lastCardReaderContents = "";

    public Simulator(int memSizeWords) {
        this.cpu = new CPU();
        this.mem = new Memory(memSizeWords);
        this.cache = new Cache(mem, DEFAULT_CACHE_LINES);
        reset();
    }

    public Simulator() {
        this(2048);
    }

    public synchronized void reset() {
        cpu.reset();
        mem.clear();
        cache.reset();

        mem.write(1, 6);
        mem.write(6, 0);
    }

    public synchronized void loadProgramFromFile(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("Program file not found: " + filename);
        }

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

                if (firstLoadedAddress < 0) {
                    firstLoadedAddress = addr;
                }
            }
        }

        cache.reset();

        if (firstLoadedAddress >= 0) {
            cpu.setPC(firstLoadedAddress);
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
    }

    public synchronized void reloadCardReaderInput() {
        cpu.setCardReaderInput(lastCardReaderContents);
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
    }

    public synchronized void setConsoleInput(String input) {
        cpu.setConsoleInput(input);
    }

    public synchronized void appendConsoleInput(String input) {
        cpu.appendConsoleInput(input);
    }

    public synchronized void clearConsoleOutput() {
        cpu.clearConsoleOutput();
    }

    public synchronized String getConsoleOutput() {
        return cpu.getConsoleOutput();
    }

    public synchronized void depositMemory(int address, int value) {
        mem.write(address, value & 0xFFFF);
        cache.reset();
    }

    public synchronized int readMemory(int address) {
        return cache.read(address) & 0xFFFF;
    }

    public synchronized void setPC(int address) {
        cpu.setPC(address);
    }

    public synchronized void setRegister(int rIndex, int value) {
        cpu.setR(rIndex, value);
    }

    public synchronized void setIndexRegister(int ixIndex, int value) {
        cpu.setIX(ixIndex, value);
    }

    public synchronized void singleStep() {
        if (!cpu.isHalted()) {
            cpu.singleStep(cache);
        }
    }

    public synchronized void run(int maxSteps) {
        int steps = 0;
        while (!cpu.isHalted() && steps < maxSteps) {
            cpu.singleStep(cache);
            steps++;
        }

        if (!cpu.isHalted()) {
            System.out.println("Run stopped after maxSteps="
                    + maxSteps + " (possible infinite loop).");
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
    }
}