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
    private int cc;

    private String consoleInput = "";
    private StringBuilder consoleOutput = new StringBuilder();
    

    private boolean halted;

    // Masks
    private static final int WORD_MASK = 0xFFFF;


    public void reset() {
        for (int i = 0; i < R.length; i++) { R[i] = 0; }
        for (int i = 0; i < IX.length; i++) { IX[i] = 0; }
        pc = mar = mbr = ir = cc = 0;
        halted = false;
        consoleInput = "";
        consoleOutput.setLength(0);
    }

    public void singleStep(Cache cache){
        if (halted) return;

        //fetch
        mar = pc;
        mbr = cache.read(mar);
        ir = mbr;
        pc = (pc + 1) & 0x7FF;

        System.out.println("Executing: PC=" + pc + " MAR=" + mar + " IR=" + String.format("%04X", ir) + " Opcode=" + ((ir >> 10) & 0x3F));

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
                ea = computeEffectiveAddress(cache, ix, i, addr);
                R[r] = cache.read(ea);
                break;
            case 02:
                ea = computeEffectiveAddress(cache, ix, i, addr);
                cache.write(ea, R[r]);
                break;
            case 03:
                ea = computeEffectiveAddress(cache, ix, i, addr);
                R[r] = ea;
                break;
            case 041: // LDX
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (ix == 0) {
                    System.err.println("Illegal LDX: x cannot be 0 (no IX0).");
                    halted = true;
                    break;
                }
                IX[ix] = cache.read(ea) & WORD_MASK;
                break;
            case 042: // STX
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (ix == 0) {
                    System.err.println("Illegal STX: x cannot be 0 (no IX0).");
                    halted = true;
                    break;
                }
                cache.write(ea, IX[ix]);
                break;
            //artihmetic instructions
            case 04:
                ea = computeEffectiveAddress(cache, ix, i, addr);
                int memVal = cache.read(ea);
                int result = R[r] + memVal;
                
                //checks for overflow
                if (result > 32767 || result < -32768) {
                    setOverflow(true);
                } else {
                    setOverflow(false);
                }
                
                R[r] = result & 0xFFFF;
                break;
            case 05:
                ea = computeEffectiveAddress(cache, ix, i, addr);
                memVal = cache.read(ea);
                result = R[r] - memVal;
                //checks for underflow
                if (result < -32768) {
                    setUnderflow(true);
                } else {
                    setUnderflow(false);
                }
                
                R[r] = result & 0xFFFF;
                break;
            case 06:
                int immed = addr;
                result = R[r] + immed;
                
                if (result > 32767 || result < -32768) {
                    setOverflow(true);
                } else {
                    setOverflow(false);
                }
                
                R[r] = result & 0xFFFF;
                break;

            case 07:
                immed = addr;
                result = R[r] - immed;
                
                if (result < -32768) {
                    setUnderflow(true);
                } else {
                    setUnderflow(false);
                }
                
                R[r] = result & 0xFFFF;
                break;

            //transfer instructions
            case 010:
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (R[r] == 0) {
                    pc = ea;
                }
                break;

            case 011:
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (R[r] != 0) {
                    pc = ea;
                }
                break;

            case 012:
                ea = computeEffectiveAddress(cache, ix, i, addr);
                boolean ccBit = (cc & (1 << r)) != 0;
                if (ccBit) {
                    pc = ea;
                }
                break;

            case 013:
                ea = computeEffectiveAddress(cache, ix, i, addr);
                pc = ea;
                break;
            case 014:
                ea = computeEffectiveAddress(cache, ix, i, addr);
                R[3] = pc;  
                pc = ea;
                break;

            case 015:
                immed = addr;  
                R[0] = immed;
                pc = R[3];
                break;

            case 016:
                ea = computeEffectiveAddress(cache, ix, i, addr);
                R[r] = (R[r] - 1) & 0xFFFF;
                if (R[r] > 0) {
                    pc = ea;
                }
                break;

            case 017:
                ea = computeEffectiveAddress(cache, ix, i, addr);
                int signedVal = (short)R[r];
                if (signedVal >= 0) {
                    pc = ea;
                }
                break;

            //register-to-register operations
            case 070:
                int ry = (ir >> 6) & 0x3; 
                if ((r != 0 && r != 2) || (ry != 0 && ry != 2)) {
                    System.err.println("MLT: rx and ry must be 0 or 2");
                    halted = true;
                    break;
                }
                int product = R[r] * R[ry];
                R[r] = (product >> 16) & 0xFFFF;
                R[r + 1] = product & 0xFFFF;
                
                //checks overflow
                if (product > 0x7FFFFFFF || product < -0x80000000) {
                    setOverflow(true);
                } else {
                    setOverflow(false);
                }
                break;

            case 071:
                ry = (ir >> 6) & 0x3;
                if ((r != 0 && r != 2) || (ry != 0 && ry != 2)) {
                    System.err.println("DVD: rx and ry must be 0 or 2");
                    halted = true;
                    break;
                }
                
                if (R[ry] == 0) {
                    setDivZero(true);
                    break;
                }
                
                setDivZero(false);
                int quotient = R[r] / R[ry];
                int remainder = R[r] % R[ry];
                R[r] = quotient & 0xFFFF; 
                R[r + 1] = remainder & 0xFFFF;
                break;
            case 072:
                ry = (ir >> 6) & 0x3;
                if (R[r] == R[ry]) {
                    setEqualOrNot(true);
                } else {
                    setEqualOrNot(false);
                }
                break;
            case 073:
                ry = (ir >> 6) & 0x3;
                R[r] = (R[r] & R[ry]) & 0xFFFF;
                break;
            case 074:
                ry = (ir >> 6) & 0x3;
                R[r] = (R[r] | R[ry]) & 0xFFFF;
                break;

            case 075:
                R[r] = (~R[r]) & 0xFFFF;
                break;
            //shift
            case 031:
                int count = ir & 0xF;  
                int lr = (ir >> 6) & 0x1;
                int al = (ir >> 7) & 0x1;
                
                if (count == 0) 
                    break;
                
                int value = R[r];
                
                if (lr == 1) {  //shift left
                    value = (value << count) & 0xFFFF;
                } else {  //shift right
                    if (al == 0) {  //arithmetic shift
                        short signedValue = (short)value;
                        signedValue >>= count;  //arithmetic right shift
                        value = signedValue & 0xFFFF;
                    } else {  //logical shift, fills w zeros
                        value = (value >>> count) & 0xFFFF;
                    }
                }
                
                R[r] = value;
                break;

            //rotate register by count
            case 032:
                count = ir & 0xF;
                lr = (ir >> 6) & 0x1;
                if (count == 0) break; 
                
                value = R[r];
                
                if (lr == 1) {  //rotate lefr
                    value = ((value << count) | (value >>> (16 - count))) & 0xFFFF;
                } else {  //rotate right
                    value = ((value >>> count) | (value << (16 - count))) & 0xFFFF;
                }
                
                R[r] = value;
                break;
            case 061:
                int devid = addr;
                
                if (devid == 0) {
                    if (consoleInput.length() > 0) {
                        char ch = consoleInput.charAt(0);
                        consoleInput = consoleInput.substring(1);
                        R[r] = ch & 0xFFFF;
                    } else {
                        R[r] = 0;
                    }
                } else {
                    R[r] = 0;
                }
                break;

            case 062:
                devid = addr;
                
                if (devid == 1) { 
                    char ch = (char)(R[r] & 0xFF);
                    consoleOutput.append(ch);
                }
                break;

            case 063:
                int devidChk = addr;
                if (devidChk == 0 || devidChk == 1 || devidChk == 2) {
                    R[r] = 1; // ready
                } else {
                    R[r] = 0;
                }
                break;

            default:
                // for unknown opcode
                System.err.println("Unknown opcode: " + String.format("%02o", opcode));
                halted = true;
        }
    }

    private int computeEffectiveAddress(Cache cache, int ixField, int iField, int addressField) {
        int ea;

        //Indexing
        if (ixField == 0) {
            ea = addressField;
        }
        else {
            ea = (IX[ixField] + addressField) & 0x7FF;
        }

        // Bound to installed memory range by letting Memory enforce bounds.
        // Idirect: EA <- M[EA]
        if (iField == 1) {
            ea = cache.read(ea) & 0x7FF;
        }

        return ea;
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


    //I/O methods
    public void setConsoleInput(String input) {
        this.consoleInput = input;
    }

    public String getConsoleOutput() {
        return consoleOutput.toString();
    }

    public void clearConsoleOutput() {
        consoleOutput.setLength(0);
    }

    //CC methods
    public int getCC() { return cc; }
    
    public boolean getOverflow() { return (cc & 0x1) != 0; }
    public boolean getUnderflow() { return (cc & 0x2) != 0; }
    public boolean getDivZero() { return (cc & 0x4) != 0; }
    public boolean getEqualOrNot() { return (cc & 0x8) != 0; }
    
    public void setOverflow(boolean val) {
        if (val) cc |= 0x1; else cc &= ~0x1;
    }
    
    public void setUnderflow(boolean val) {
        if (val) cc |= 0x2; else cc &= ~0x2;
    }
    
    public void setDivZero(boolean val) {
        if (val) cc |= 0x4; else cc &= ~0x4;
    }
    
    public void setEqualOrNot(boolean val) {
        if (val) cc |= 0x8; else cc &= ~0x8;
    }
    
    public void clearCC() {
        cc = 0;
    }

    public boolean isHalted() { return halted; }
    public void setHalted(boolean halted) { this.halted = halted; }
}
