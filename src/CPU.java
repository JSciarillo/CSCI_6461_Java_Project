package src;

public final class CPU {
    private final int[] R = new int[4]; // R0 - R3
    private final int[] IX = new int[4];

    private int pc;
    private int mar;
    private int mbr;
    private int ir;

    private boolean halted;

    public void reset() {
        for (int i = 0; i < R.length; i++) { R[i] = 0; }
        for (int i = 0; i < IX.length; i++) { IX[i] = 0; }
        pc = mar = mbr = ir = 0;
        halted = false;
    }

    public void singleStep(Memory mem){
        if (halted) return;

        //fetch
        mar = pc;
        mbr = mem.read(mar);
        ir = mbr;
        pc = (pc + 1) & 0x7FF;

        System.out.println("Executing: PC=" + pc + " MAR=" + mar + " IR=" + String.format("%04X", ir) + " Opcode=" + ((ir >> 10) & 0x3F));

        pc = (pc + 1) & 0x7FF;
        //decode
        int opcode = (ir >> 10) & 0x3F;
        int r = (ir >> 8) & 0x3;
        int ix = (ir >> 6) & 0x3;
        int i = (ir >> 5) & 0x1;
        int addr = ir & 0x1F;

        //load/store
        int ea;
        switch (opcode) {
            //HLT
            case 0:
                halted = true;
                break;
            //LDR r, x, address [ I]
            case 01:
                ea = computeEffectiveAddress(mem, ix, i, addr);
                R[r] = mem.read(ea);
                break;
            case 02:
                ea = computeEffectiveAddress(mem, ix, i, addr);
                mem.write(ea, R[r]);
                break;
            case 03:
                ea = computeEffectiveAddress(mem, ix, i, addr);
                R[r] = ea;
                break;
            case 041:
                ea = computeEffectiveAddress(mem, ix, i, addr);
                IX[r] = mem.read(ea);
                break;
            case 042:
                ea = computeEffectiveAddress(mem, ix, i, addr);
                mem.write(ea, IX[r]);
                break;
            default:
                //for unknown opcode
                System.err.println("Unknown opcode: " + String.format("%02o", opcode));
                halted = true;

        }

    }

    private int computeEffectiveAddress(Memory mem, int ixField, int iField, int addressField) {
        int ea;

        //Indexing
        if (ixField == 0) {
            ea = addressField;
        }
        else {
            ea = (IX[ixField] + addressField) & 0x7FF;
        }

        //Indirect addressing
        if (iField == 1) {
            ea = mem.read(ea) & 0x7FF;
        }

        return ea;
    }
    
    // private void decode(){}
    // private void computeEffectiveAddress(){}
    // private void execute(){}

    public int getPC()  { return pc; }
    public int getMAR() { return mar; }
    public int getMBR() { return mbr; }
    public int getIR()  { return ir; }

    public int getR(int idx) {
        if (idx < 0 || idx > 3)
            throw new IllegalArgumentException("R index out of range");
            return R[idx];
    }

    public int getIX(int idx) {
        if (idx < 0 || idx > 3)
            throw new IllegalArgumentException("IX index out of range");
            return IX[idx];
    }
        


    public void setPC(int pc) { this.pc = pc; }

    public boolean isHalted() { return halted; }
    public void setHalted(boolean halted) { this.halted = halted; }

    public void setR(int idx, int value) {
        if (idx < 0 || idx > 3)
            throw new IllegalArgumentException("R index out of range");
            R[idx] = value & 0xFFFF;
    }
    public void setIX(int idx, int value) {
        if (idx < 0 || idx > 3) throw new IllegalArgumentException("IX index out of range");
        IX[idx] = value & 0xFFFF;

    }
}
