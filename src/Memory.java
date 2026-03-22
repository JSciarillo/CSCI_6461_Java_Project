package src;

public final class Memory {
    private final int[] mem;
    private final int wordMask;

    public Memory(int sizeWords, int wordBits) {
        if (sizeWords <= 0) throw new IllegalArgumentException("sizeWords must be > 0");
        if (wordBits <= 0 || wordBits > 31) throw new IllegalArgumentException("wordBits invalid");
        this.mem = new int[sizeWords];
        this.wordMask = (1 << wordBits) - 1;
    }

    public int size() { return mem.length; }

    public int read(int address){
        int a = normalizeAddress(address);
        return mem[a] & wordMask;
    }
    
    public void write(int address, int value){
        int a = normalizeAddress(address);
        mem[a] = value & wordMask;
    }

    private int normalizeAddress(int address) {
        if (address < 0 || address >= mem.length) {
            throw new IndexOutOfBoundsException("Invalid memory address: " + address);
        }
        return address;
    }
}
