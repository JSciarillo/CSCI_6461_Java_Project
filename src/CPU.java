package src;

public final class CPU {
    private final int[] R = new int[4]; // R0 - R3
    private final int[] IX = new int[4];

    private int pc;
    private int mar;
    private int mbr;
    private int ir;
    private int cc;

    private String consoleInput = "";
    private StringBuilder consoleOutput = new StringBuilder();

    private boolean halted;

    public void reset() {
        for (int i = 0; i < R.length; i++) { R[i] = 0; }
        for (int i = 0; i < IX.length; i++) { IX[i] = 0; }
        pc = mar = mbr = ir = cc = 0;
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
            //artihmetic instructions
            case 04:
                ea = computeEffectiveAddress(mem, ix, i, addr);
                int memVal = mem.read(ea);
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
                ea = computeEffectiveAddress(mem, ix, i, addr);
                memVal = mem.read(ea);
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
                ea = computeEffectiveAddress(mem, ix, i, addr);
                if (R[r] == 0) {
                    pc = ea;
                }
                break;

            case 011:
                ea = computeEffectiveAddress(mem, ix, i, addr);
                if (R[r] != 0) {
                    pc = ea;
                }
                break;

            case 012:
                ea = computeEffectiveAddress(mem, ix, i, addr);
                boolean ccBit = (cc & (1 << r)) != 0;
                if (ccBit) {
                    pc = ea;
                }
                break;

            case 013:
                ea = computeEffectiveAddress(mem, ix, i, addr);
                pc = ea;
                break;
            case 014:
                ea = computeEffectiveAddress(mem, ix, i, addr);
                R[3] = pc;  
                pc = ea;
                break;

            case 015:
                immed = addr;  
                R[0] = immed;
                pc = R[3];
                break;

            case 016:
                ea = computeEffectiveAddress(mem, ix, i, addr);
                R[r] = (R[r] - 1) & 0xFFFF;
                if (R[r] > 0) {
                    pc = ea;
                }
                break;

            case 017:
                ea = computeEffectiveAddress(mem, ix, i, addr);
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
                if (product > 0xFFFFFFFF || product < 0) {
                    setOverflow(true);
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

}
