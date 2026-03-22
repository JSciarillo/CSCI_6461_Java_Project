package src;

/**
 * Memory.java
 * 
 * Simple word-addressable memory.
 * - Installed size: typically 2048 words (address 0..2047)
 * - Word size: 16 bits
 * 
 * Part I uses direct read/write
 */
public final class Memory {
    private final int[] mem;
    private static final int WORD_MASK = 0xFFFF;

    public Memory(int sizeWords) {
        if (sizeWords <= 0) throw new IllegalArgumentException("sizeWords must be > 0");
        this.mem = new int[sizeWords];
    }

    public int size() { return mem.length; }

    // Sets all memory words to zero
    public void clear() {
        for (int i = 0; i < mem.length; i++) {
            mem[i] = 0;
        }
    }

    public int read(int address){
        int a = normalizeAddress(address);
        return mem[a] & WORD_MASK;
    }
    
    public void write(int address, int value){
        int a = normalizeAddress(address);
        mem[a] = value & WORD_MASK;
    }

    private int normalizeAddress(int address) {
        if (address < 0 || address >= mem.length) {
            throw new IndexOutOfBoundsException("Invalid memory address: " + address);
        }
        return address;
    }
}
