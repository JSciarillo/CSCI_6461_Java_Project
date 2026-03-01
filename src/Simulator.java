package src;
import java.io.*;

/**
 * Simulator.java
 * 
 * Ties CPU + Memory together and provides "front-panel style" operations:
 * - reset (clears memory and registers)
 * - load program from a load-file
 * - singleStep / run
 */
public final class Simulator {
    private final CPU cpu;
    private final Memory mem;

    public Simulator(int memSizeWords) {
        this.cpu = new CPU();
        this.mem = new Memory(memSizeWords);
        reset();
    }

    public void reset(){
        cpu.reset();
        mem.clear();
    }

    /**
     * Load a program from a "Load.txt"-style file:
     * each line: "<octalAddr> <octalWord>"
     * - Blank lines allowed
     * - Lines beginning with ';' treated as comments
     */
    public void loadProgramFromFile(String filename) throws IOException{
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            for (String line; (line = br.readLine()) != null; ) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith(";")) continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 2) continue;

                int addr = Integer.parseInt(parts[0], 8);
                int value = Integer.parseInt(parts[1], 8);

                mem.write(addr, value);
            }
        }
    }

    public void depositMemory(int address, int value) {
        mem.write(address, value);
    }

    public int readMemory(int address) {
        return mem.read(address);
    }

    public void setPC(int address) {
        cpu.setPC(address);
    }

    public void setRegister(int rIndex, int value) {
        cpu.setR(rIndex, value);
    }

    public void setIndexRegister(int ixIndex, int value) {
        cpu.setIX(ixIndex, value);
    }

    public void singleStep(){
        cpu.singleStep(mem);
    }

    /**
     * Runs until HALT or until maxSteps is reached
     */
    public void run(int maxSteps) {
        int steps = 0;
        while (!cpu.isHalted() && steps < maxSteps) {
            cpu.singleStep(mem);
            steps++;
        }
        if(!cpu.isHalted()) {
            System.out.println("Run stopped after maxSteps="
                + maxSteps + " (possible infinite loop).");
        }
    }

    public CPU getCPU() { return cpu; }
    public Memory getMemory() { return mem; }

    public int getMemoryAtMAR() {
        return mem.read(cpu.getMAR());
    }
}