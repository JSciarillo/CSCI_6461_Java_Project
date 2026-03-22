package src;

import java.io.*;

public final class Simulator {
    private final CPU cpu;
    private final Memory mem;
    private final Cache cache;

    public Simulator(int memSizeWords, int wordBits) {
        this.cpu = new CPU();
        this.mem = new Memory(memSizeWords, wordBits);
        this.cache = new Cache(mem, 16); // 16 cache lines for Part II
        this.cpu.reset();
    }

    public void reset() {
        cpu.reset();
        for (int i = 0; i < mem.size(); i++) {
            mem.write(i, 0);
        }
        cache.reset();
    }

    public void loadProgramFromFile(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 2)
                    continue;

                int addr = Integer.parseInt(parts[0], 8);
                int value = Integer.parseInt(parts[1], 8);

                mem.write(addr, value); // loader writes to real memory
            }
        }
        cache.reset(); // invalidate cache after new program is loaded
    }

    public void depositMemory(int address, int value) {
        mem.write(address, value);
        cache.reset();
    }

    public int readMemory(int address) {
        return cache.read(address);
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

    public void singleStep() {
        cpu.singleStep(cache);
    }

    public void run() {
        while (!cpu.isHalted()) {
            cpu.singleStep(cache);
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

    public int getMemoryAtMAR() {
        return cache.read(cpu.getMAR());
    }
}