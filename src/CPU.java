package src;

public final class CPU {
    private final int[] R = new int[4]; // R0 - R3

    private int pc;
    private int mar;
    private int mbr;
    private int ir;

    private boolean halted;

    public void reset() {
        for (int i = 0; i < R.length; i++) { R[i] = 0; }
        pc = mar = mbr = ir = 0;
        halted = false;
    }

    public void signleStep(Memory mem){
        if (halted) return;

        fetch(mem);
    }

    private void fetch(Memory mem){
        mar = pc;
        mbr = mem.read(mar);
        ir = mbr;
        pc = pc + 1;
    }
    
    private void decode(){}
    private void computeEffectiveAddress(){}
    private void execute(){}

    public int getPC()  { return pc; }
    public int getMAR() { return mar; }
    public int getMBR() { return mbr; }
    public int getIR()  { return ir; }

    public int getR(int idx) { return R[idx]; }
    public void setR(int idx, int value) { R[idx] = value; }

    public void setPC(int pc) { this.pc = pc; }

    public boolean isHalted() { return halted; }
    public void setHalted(boolean halted) { this.halted = halted; }
}
