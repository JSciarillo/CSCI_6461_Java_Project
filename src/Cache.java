package src;

public final class Cache {
    private final CacheLine[] lines;
    private final Memory memory;
    private long nextOrder = 1;

    private int lastAccessAddress = -1;
    private boolean lastAccessHit = false;
    private String lastOperation = "";

    public Cache(Memory memory, int numLines) {
        this.memory = memory;
        this.lines = new CacheLine[numLines];
        for (int i = 0; i < numLines; i++) {
            lines[i] = new CacheLine();
        }
    }

    public void reset() {
        nextOrder = 1;
        lastAccessAddress = -1;
        lastAccessHit = false;
        lastOperation = "";
        for (CacheLine line : lines) {
            line.valid = false;
            line.address = 0;
            line.data = 0;
            line.fifoOrder = 0;
        }
    }

    public int read(int address) {
        int idx = findLine(address);
        lastAccessAddress = address;
        lastOperation = "READ";

        if (idx >= 0) {
            lastAccessHit = true;
            return lines[idx].data & 0xFFFF;
        }

        lastAccessHit = false;
        int value = memory.read(address);
        insertLine(address, value);
        return value & 0xFFFF;
    }

    public void write(int address, int value) {
        lastAccessAddress = address;
        lastOperation = "WRITE";

        int idx = findLine(address);
        if (idx >= 0) {
            lastAccessHit = true;
            lines[idx].data = value & 0xFFFF;
        } else {
            lastAccessHit = false;
            insertLine(address, value);
        }

        memory.write(address, value);
    }

    private int findLine(int address) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].valid && lines[i].address == address) {
                return i;
            }
        }
        return -1;
    }

    private void insertLine(int address, int value) {
        int target = firstInvalidLine();
        if (target < 0) {
            target = fifoVictim();
        }

        lines[target].valid = true;
        lines[target].address = address;
        lines[target].data = value & 0xFFFF;
        lines[target].fifoOrder = nextOrder++;
    }

    private int firstInvalidLine() {
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].valid)
                return i;
        }
        return -1;
    }

    private int fifoVictim() {
        int victim = 0;
        long oldest = lines[0].fifoOrder;

        for (int i = 1; i < lines.length; i++) {
            if (lines[i].fifoOrder < oldest) {
                oldest = lines[i].fifoOrder;
                victim = i;
            }
        }
        return victim;
    }

    public CacheLine[] getLines() {
        return lines;
    }

    public String getLastAccessSummary() {
        if (lastAccessAddress < 0)
            return "No cache accesses yet";
        return String.format(
                "%s addr=%04X %s",
                lastOperation,
                lastAccessAddress,
                lastAccessHit ? "HIT" : "MISS");
    }
}