package src;

/**
 * CPU.java
 * 
 * Implements the core fetch/decode/execute cycle of the CPU.
 * 
 * ISA Notes:
 * - 16-bit words
 * - PC/MAR are 12-bit conceptually, but we only install 2048 words
 * - Load/Store/Transfer format uses a 5-bit address field
 * - Indexing (IX1...IX3) is used to reach the rest of memory
 * - IX field value 0 => no indexing. There is no IX0 register.
 * 
 * Part I scope: implement HLT + LOAD/STORE (LDR/STR/LDA/LDX/STX).
 */
public final class CPU {
    // Program-visible registers
    private final int[] R = new int[4]; // R0 - R3
    private final int[] IX = new int[4];

    // Internal registers
    private int pc;  // 12-bit
    private int mar; // 12-bit
    private int mbr; // 16-bit
    private int ir;  // 16-bit

    private boolean halted;

    // Masks
    private static final int WORD_MASK = 0xFFFF;


    public void reset() {
        for (int i = 0; i < R.length; i++) { R[i] = 0; }
        for (int i = 0; i < IX.length; i++) { IX[i] = 0; }
        pc = mar = mbr = ir = 0;
        halted = false;
    }

    /**
     * Executes ONE instruction (fetch -> decode -> execute).
     * This is the behavior needed for "Single Step" mode in the UI.
     */
    public void singleStep(Memory mem){
        if (halted) return;

        // ---------------
        // FETCH
        // ---------------
        mar = pc;
        mbr = mem.read(mar);
        ir = mbr;

        // PC increrments ONCE per instruction fetch
        // Transfer instructions will override PC during execute if they branch/jump
        pc = nextAddress(pc, mem);

        System.out.println("Executing: PC=" + pc + " MAR=" + mar + " IR=" + String.format("%04X", ir) + " Opcode=" + ((ir >> 10) & 0x3F));

        // ---------------
        // DECODE
        // ---------------
        int opcode = (ir >> 10) & 0x3F;  // 6-bit opcode
        int rField = (ir >> 8) & 0x3;    // 2-bit register field
        int ixField = (ir >> 6) & 0x3;   // 2-bit index seelector
        int iField = (ir >> 5) & 0x1;    // indirect bit
        int addr5 = ir & 0x1F;           // 5-bit address field

        // ---------------
        // EXECUTE
        // ---------------
        int ea = computeEA(mem, ixField, iField, addr5);
        switch (opcode) {
            case 0: //HLT
                halted = true;
                return;
            case 01: //LDR r, x, address[,I]
                R[rField] = mem.read(ea) & WORD_MASK;
                return;
            case 02: // STR r, x, address[,I]
                mem.write(ea, R[rField]);
                return;
            case 03: // LDA r, x, address[,I] => r <- EA
                R[rField] = ea & WORD_MASK;
                return;
            case 041: // LDX x, address[,I] (x = 1..3) => Xx <- c(EA)
                // LDX uses the IX field to specify which index register to load.
                // rField is ignored for this instruction.
                if (ixField == 0) {
                    // No X0 exists; treat as illegal usage for LDX
                    System.err.println("Illegal LDX: x cannot be 0 (no IX0).");
                    halted = true;
                    return;
                }
                IX[ixField] = mem.read(ea) & WORD_MASK;
                return;
            case 042: // STX x, address[,I] (x = 1..3) => c(EA) <- Xx
                if (ixField == 0) {
                    // No X0 exists; treat as illegal usage for STX
                    System.err.println("Illegal STX: x cannot be 0 (no IX0).");
                    halted = true;
                    return;
                }
                mem.write(ea, IX[ixField]);
                return;
            default:
                //for unknown opcode
                System.err.println("Unknown opcode: " + String.format("%02o", opcode));
                halted = true;
        }
    }

    /**
     * Effective Address computation:
     * - If IX=0: EA = AddressField (5-bit value 0..31)
     * - Else:    EA = c(IX) + AddressField
     * - If I=1:  EA = c(EA)  (indirect)
     */
    private int computeEA(Memory mem, int ixField, int iField, int addressField) {
        int ea;

        // Base address from 5-bit address field
        ea = addressField & 0x1F;

        // Indexing: add IX1...IX3 if selected
        if (ixField == 0) {
            ea = ea + (IX[ixField] & 0xFFFF);
        }

        // Bound to installed memory range by letting Memory enforce bounds.
        // Idirect: EA <- M[EA]
        if (iField == 1) {
            ea = mem.read(ea);
        }

        return ea;
    }
    
    private static int nextAddress(int currentPC, Memory mem) {
        // Installed memory is 0...(size-1)
        int next = currentPC + 1;
        if (next >= mem.size()) next = 0;
        return next;
    }

    // ---------------
    // Getters / setters for GUI
    // ---------------
    public int getPC()  { return pc; }
    public int getMAR() { return mar; }
    public int getMBR() { return mbr & WORD_MASK; }
    public int getIR()  { return ir & WORD_MASK; }

    public int getR(int idx) {
        if (idx < 0 || idx > 3) throw new IllegalArgumentException("R index out of range");
        return R[idx] & WORD_MASK;
    }

    public int getIX(int idx) {
        if (idx < 0 || idx > 3) throw new IllegalArgumentException("IX index out of range");
        return IX[idx] & WORD_MASK;
    }
        
    public void setPC(int pc) { 
        if (pc < 0) throw new IllegalArgumentException("PC must be >= 0");
        this.pc = pc; 
    }

    public void setR(int idx, int value) {
        if (idx < 0 || idx > 3) throw new IllegalArgumentException("R index out of range");
        R[idx] = value & WORD_MASK;
    }
    public void setIX(int idx, int value) {
        if (idx < 0 || idx > 3) throw new IllegalArgumentException("IX index out of range");
        if (idx == 0) throw new IllegalArgumentException("IX0 does not exist; valid IX index is 1..3");
        IX[idx] = value & WORD_MASK;
    }

    public boolean isHalted() { return halted; }
    public void setHalted(boolean halted) { this.halted = halted; }
}
