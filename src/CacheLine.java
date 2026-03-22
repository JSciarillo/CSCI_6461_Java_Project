package src;

public final class CacheLine {
    boolean valid;
    int address; // full memory word address
    int data; // 16-bit word
    long fifoOrder;

    public CacheLine() {
        this.valid = false;
        this.address = 0;
        this.data = 0;
        this.fifoOrder = 0;
    }
}