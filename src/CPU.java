package src;

/**
 * CPU.java
 *
 * Implements the core fetch / decode / execute cycle of the C6461 CPU.
 *
 * Current scope:
 * - Non-floating point / non-vector instruction set needed through Part III
 * - Basic machine fault handling
 * - Basic TRAP handling
 * - Simple console input/output model used by current Simulator/GUI flow
 */
public final class CPU {
    // Program-visible registers
    private final int[] R = new int[4]; // R0-R3
    private final int[] IX = new int[4]; // IX1-IX3 used, IX0 unused

    // Internal registers
    private int pc; // address of next instruction
    private int mar; // memory address register
    private int mbr; // memory buffer register
    private int ir; // instruction register
    private int cc; // condition code register
    private int mfr; // machine fault register

    // Fault codes per project spec
    private static final int FAULT_RESERVED_MEMORY = 0x1; // 0001
    private static final int FAULT_ILLEGAL_TRAP = 0x2; // 0010
    private static final int FAULT_ILLEGAL_OPCODE = 0x4; // 0100
    private static final int FAULT_MEMORY_OVERFLOW = 0x8; // 1000

    // Simple I/O buffers for current Part III workflow
    private final StringBuilder consoleInput = new StringBuilder();
    private final StringBuilder cardReaderInput = new StringBuilder();
    private final StringBuilder consoleOutput = new StringBuilder();

    private boolean halted;

    private static final int WORD_MASK = 0xFFFF;

    public void reset() {
        for (int i = 0; i < R.length; i++) {
            R[i] = 0;
        }
        for (int i = 0; i < IX.length; i++) {
            IX[i] = 0;
        }

        pc = 0;
        mar = 0;
        mbr = 0;
        ir = 0;
        cc = 0;
        mfr = 0;
        halted = false;

        consoleInput.setLength(0);
        cardReaderInput.setLength(0);
        consoleOutput.setLength(0);
    }

    public void singleStep(Cache cache) {
        if (halted)
            return;

        // ----------------
        // Fetch
        // ----------------
        mar = pc;
        if (!validateInstructionFetchAddress(mar, cache))
            return;

        mbr = cache.read(mar) & WORD_MASK;
        ir = mbr;
        pc = (pc + 1) & 0x7FF; // keep within installed address range

        System.out.println("Executing: PC=" + String.format("%04o", pc)
                + " MAR=" + String.format("%04o", mar)
                + " IR=" + String.format("%04X", ir)
                + " Opcode=" + String.format("%02o", ((ir >> 10) & 0x3F)));

        // ----------------
        // Decode
        // ----------------
        int opcode = (ir >> 10) & 0x3F;
        int r = (ir >> 8) & 0x3;
        int ix = (ir >> 6) & 0x3;
        int i = (ir >> 5) & 0x1;
        int addr = ir & 0x1F;

        int ea;

        switch (opcode) {
            // =========================================================
            // Miscellaneous
            // =========================================================
            case 000: // HLT
                halted = true;
                break;

            case 030: { // TRAP
                int trapCode = ir & 0xF;
                executeTrap(trapCode, cache);
                break;
            }

            // =========================================================
            // Load / Store
            // =========================================================
            case 001: // LDR r,x,address[,I]
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (halted)
                    break;
                if (!validateDataAddress(ea, false, cache))
                    break;
                R[r] = cache.read(ea) & WORD_MASK;
                break;

            case 002: // STR r,x,address[,I]
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (halted)
                    break;
                if (!validateDataAddress(ea, true, cache))
                    break;
                cache.write(ea, R[r] & WORD_MASK);
                break;

            case 003: // LDA r,x,address[,I]
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (halted)
                    break;
                R[r] = ea & WORD_MASK;
                break;

            case 041: // LDX x,address[,I]
                if (ix == 0) {
                    signalMachineFault(FAULT_ILLEGAL_OPCODE, cache);
                    break;
                }
                ea = computeEffectiveAddressForIndexInstruction(cache, i, addr);
                if (halted)
                    break;
                if (!validateDataAddress(ea, false, cache))
                    break;
                IX[ix] = cache.read(ea) & WORD_MASK;
                break;

            case 042: // STX x,address[,I]
                if (ix == 0) {
                    signalMachineFault(FAULT_ILLEGAL_OPCODE, cache);
                    break;
                }
                ea = computeEffectiveAddressForIndexInstruction(cache, i, addr);
                if (halted)
                    break;
                if (!validateDataAddress(ea, true, cache))
                    break;
                cache.write(ea, IX[ix] & WORD_MASK);
                break;

            // =========================================================
            // Arithmetic and Logical (memory/immediate)
            // =========================================================
            case 004: { // AMR
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (halted)
                    break;
                if (!validateDataAddress(ea, false, cache))
                    break;

                int lhs = toSigned16(R[r]);
                int rhs = toSigned16(cache.read(ea));
                int sum = lhs + rhs;

                setOverflow(sum > Short.MAX_VALUE || sum < Short.MIN_VALUE);
                setUnderflow(false);

                R[r] = toUnsigned16(sum);
                break;
            }

            case 005: { // SMR
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (halted)
                    break;
                if (!validateDataAddress(ea, false, cache))
                    break;

                int lhs = toSigned16(R[r]);
                int rhs = toSigned16(cache.read(ea));
                int diff = lhs - rhs;

                setUnderflow(diff < Short.MIN_VALUE);
                setOverflow(diff > Short.MAX_VALUE);

                R[r] = toUnsigned16(diff);
                break;
            }

            case 006: { // AIR
                int immed = addr;
                int regVal = toSigned16(R[r]);
                int result = regVal + immed;

                setOverflow(result > Short.MAX_VALUE || result < Short.MIN_VALUE);
                setUnderflow(false);

                R[r] = toUnsigned16(result);
                break;
            }

            case 007: { // SIR
                int immed = addr;
                int regVal = toSigned16(R[r]);
                int result = regVal - immed;

                setUnderflow(result < Short.MIN_VALUE);
                setOverflow(result > Short.MAX_VALUE);

                R[r] = toUnsigned16(result);
                break;
            }

            // =========================================================
            // Transfer
            // =========================================================
            case 010: // JZ
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (halted)
                    break;
                if (R[r] == 0) {
                    pc = ea & 0x7FF;
                }
                break;

            case 011: // JNE
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (halted)
                    break;
                if (R[r] != 0) {
                    pc = ea & 0x7FF;
                }
                break;

            case 012: { // JCC
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (halted)
                    break;
                boolean ccBit = (cc & (1 << r)) != 0;
                if (ccBit) {
                    pc = ea & 0x7FF;
                }
                break;
            }

            case 013: // JMA
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (halted)
                    break;
                pc = ea & 0x7FF;
                break;

            case 014: // JSR
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (halted)
                    break;
                R[3] = pc & WORD_MASK; // pc already points to next instruction
                pc = ea & 0x7FF;
                break;

            case 015: { // RFS
                int returnCode = addr;
                R[0] = returnCode & WORD_MASK;
                pc = R[3] & 0x7FF;
                break;
            }

            case 016: // SOB
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (halted)
                    break;
                R[r] = (R[r] - 1) & WORD_MASK;
                if (toSigned16(R[r]) > 0) {
                    pc = ea & 0x7FF;
                }
                break;

            case 017: { // JGE
                ea = computeEffectiveAddress(cache, ix, i, addr);
                if (halted)
                    break;
                int signedVal = toSigned16(R[r]);
                if (signedVal >= 0) {
                    pc = ea & 0x7FF;
                }
                break;
            }

            // =========================================================
            // Register-to-register
            // =========================================================
            case 070: { // MLT
                int ry = (ir >> 6) & 0x3;
                if ((r != 0 && r != 2) || (ry != 0 && ry != 2)) {
                    signalMachineFault(FAULT_ILLEGAL_OPCODE, cache);
                    break;
                }

                int m1 = toSigned16(R[r]);
                int m2 = toSigned16(R[ry]);
                int product = m1 * m2;

                R[r] = (product >>> 16) & WORD_MASK;
                R[r + 1] = product & WORD_MASK;

                setOverflow(false); // can refine later if needed
                break;
            }

            case 071: { // DVD
                int ry = (ir >> 6) & 0x3;
                if ((r != 0 && r != 2) || (ry != 0 && ry != 2)) {
                    signalMachineFault(FAULT_ILLEGAL_OPCODE, cache);
                    break;
                }

                int divisor = toSigned16(R[ry]);
                if (divisor == 0) {
                    setDivZero(true);
                    break;
                }

                setDivZero(false);
                int dividend = toSigned16(R[r]);
                int quotient = dividend / divisor;
                int remainder = dividend % divisor;

                R[r] = toUnsigned16(quotient);
                R[r + 1] = toUnsigned16(remainder);
                break;
            }

            case 072: { // TRR
                int ry = (ir >> 6) & 0x3;
                setEqualOrNot(R[r] == R[ry]);
                break;
            }

            case 073: { // AND
                int ry = (ir >> 6) & 0x3;
                R[r] = (R[r] & R[ry]) & WORD_MASK;
                break;
            }

            case 074: { // ORR
                int ry = (ir >> 6) & 0x3;
                R[r] = (R[r] | R[ry]) & WORD_MASK;
                break;
            }

            case 075: // NOT
                R[r] = (~R[r]) & WORD_MASK;
                break;

            // =========================================================
            // Shift / Rotate
            // =========================================================
            case 031: { // SRC
                int count = ir & 0xF;
                int lr = (ir >> 6) & 0x1; // 1 = left, 0 = right
                int al = (ir >> 7) & 0x1; // 1 = logical, 0 = arithmetic

                if (count == 0)
                    break;

                int value = R[r] & WORD_MASK;

                if (lr == 1) {
                    value = (value << count) & WORD_MASK;
                } else {
                    if (al == 0) {
                        short signedValue = (short) value;
                        signedValue >>= count;
                        value = signedValue & WORD_MASK;
                    } else {
                        value = (value >>> count) & WORD_MASK;
                    }
                }

                R[r] = value;
                break;
            }

            case 032: { // RRC
                int count = ir & 0xF;
                int lr = (ir >> 6) & 0x1;

                if (count == 0)
                    break;

                int value = R[r] & WORD_MASK;
                count = count % 16;

                if (lr == 1) {
                    value = ((value << count) | (value >>> (16 - count))) & WORD_MASK;
                } else {
                    value = ((value >>> count) | (value << (16 - count))) & WORD_MASK;
                }

                R[r] = value;
                break;
            }

            // =========================================================
            // I/O
            // =========================================================
            case 061: { // IN
                int devid = addr;

                if (devid == 0) { // keyboard
                    R[r] = readCharFromBuffer(consoleInput);
                } else if (devid == 2) { // card reader
                    R[r] = readCharFromBuffer(cardReaderInput);
                } else {
                    R[r] = 0;
                }
                break;
            }

            case 062: { // OUT
                int devid = addr;

                if (devid == 1) {
                    char ch = (char) (R[r] & 0xFF);
                    synchronized (consoleOutput) {
                        consoleOutput.append(ch);
                    }
                }
                break;
            }

            case 063: { // CHK
                int devid = addr;
                if (devid == 0) {
                    synchronized (consoleInput) {
                        R[r] = consoleInput.length() == 0 ? 0 : 1;
                    }
                } else if (devid == 1) {
                    R[r] = 1;
                } else if (devid == 2) {
                    synchronized (cardReaderInput) {
                        R[r] = cardReaderInput.length() == 0 ? 0 : 1;
                    }
                } else {
                    R[r] = 0;
                }
                break;
            }

            // =========================================================
            // Illegal / unknown
            // =========================================================
            default:
                signalMachineFault(FAULT_ILLEGAL_OPCODE, cache);
                break;
        }
    }

    private int readCharFromBuffer(StringBuilder buffer) {
        synchronized (buffer) {
            if (buffer.length() == 0) {
                return 0; // still needed for polling, BUT see below fix
            }
            char ch = buffer.charAt(0);
            buffer.deleteCharAt(0);
            return ch & WORD_MASK;
        }
    }
    
    public void setCardReaderInput(String input) {
        synchronized (cardReaderInput) {
            cardReaderInput.setLength(0);
            if (input != null) {
                cardReaderInput.append(input);
            }
        }
    }

    private int computeEffectiveAddress(Cache cache, int ixField, int iField, int addressField) {
        int ea = addressField;

        if (ixField != 0) {
            ea = (IX[ixField] + addressField) & 0x7FF;
        }

        if (!isInstalledAddress(ea)) {
            signalMachineFault(FAULT_MEMORY_OVERFLOW, cache);
            return 0;
        }

        if (iField == 1) {
            if (!validateDataAddress(ea, false, cache))
                return 0;
            ea = cache.read(ea) & 0x7FF;

            if (!isInstalledAddress(ea)) {
                signalMachineFault(FAULT_MEMORY_OVERFLOW, cache);
                return 0;
            }
        }

        return ea;
    }

    private int computeEffectiveAddressForIndexInstruction(Cache cache, int iField, int addressField) {
        int ea = addressField;

        if (!isInstalledAddress(ea)) {
            signalMachineFault(FAULT_MEMORY_OVERFLOW, cache);
            return 0;
        }

        if (iField == 1) {
            if (!validateDataAddress(ea, false, cache))
                return 0;
            ea = cache.read(ea) & 0x7FF;

            if (!isInstalledAddress(ea)) {
                signalMachineFault(FAULT_MEMORY_OVERFLOW, cache);
                return 0;
            }
        }

        return ea;
    }

    private int toUnsigned16(int v) {
        return v & WORD_MASK;
    }

    private int toSigned16(int v) {
        return (short) (v & WORD_MASK);
    }

    private boolean isReservedAddress(int addr) {
        return addr >= 0 && addr <= 5;
    }

    private boolean isInstalledAddress(int addr) {
        return addr >= 0 && addr < 2048;
    }

    private boolean validateInstructionFetchAddress(int addr, Cache cache) {
        if (!isInstalledAddress(addr)) {
            signalMachineFault(FAULT_MEMORY_OVERFLOW, cache);
            return false;
        }
        return true;
    }

    private boolean validateDataAddress(int addr, boolean isWrite, Cache cache) {
        if (!isInstalledAddress(addr)) {
            signalMachineFault(FAULT_MEMORY_OVERFLOW, cache);
            return false;
        }

        // Conservative protection:
        // block normal program writes into reserved memory
        if (isWrite && isReservedAddress(addr)) {
            signalMachineFault(FAULT_RESERVED_MEMORY, cache);
            return false;
        }

        return true;
    }

    private void signalMachineFault(int faultCode, Cache cache) {
        mfr = faultCode & 0xF;

        try {
            // Save current PC in reserved machine-fault location
            cache.write(4, pc & WORD_MASK);

            // Memory location 1 contains machine-fault handler address
            int handler = cache.read(1) & 0x7FF;
            if (!isInstalledAddress(handler)) {
                halted = true;
                consoleOutput.append("\nMACHINE FAULT: invalid handler address\n");
                return;
            }

            pc = handler;
        } catch (Exception e) {
            halted = true;
            consoleOutput.append("\nMACHINE FAULT: ")
                    .append(Integer.toString(faultCode))
                    .append("\n");
        }
    }

    private void executeTrap(int trapCode, Cache cache) {
        if (trapCode < 0 || trapCode > 15) {
            signalMachineFault(FAULT_ILLEGAL_TRAP, cache);
            return;
        }

        try {
            // Save return PC in location 2
            cache.write(2, pc & WORD_MASK);

            // Location 0 contains the trap table base address
            int trapTableBase = cache.read(0) & 0x7FF;
            if (!isInstalledAddress(trapTableBase + trapCode)) {
                signalMachineFault(FAULT_MEMORY_OVERFLOW, cache);
                return;
            }

            int trapRoutineAddr = cache.read(trapTableBase + trapCode) & 0x7FF;
            if (!isInstalledAddress(trapRoutineAddr)) {
                signalMachineFault(FAULT_MEMORY_OVERFLOW, cache);
                return;
            }

            pc = trapRoutineAddr;
        } catch (Exception e) {
            halted = true;
            consoleOutput.append("\nTRAP ERROR\n");
        }
    }

    public void appendConsoleInput(String input) {
        if (input == null || input.isEmpty()) {
            return;
        }
        synchronized (consoleInput) {
            consoleInput.append(input);
        }
    }

    // ----------------
    // Getters / setters
    // ----------------
    public int getPC() {
        return pc & 0x7FF;
    }

    public int getMAR() {
        return mar & 0x7FF;
    }

    public int getMBR() {
        return mbr & WORD_MASK;
    }

    public int getIR() {
        return ir & WORD_MASK;
    }

    public int getMFR() {
        return mfr & 0xF;
    }

    public int getR(int idx) {
        if (idx < 0 || idx > 3) {
            throw new IllegalArgumentException("R index out of range");
        }
        return R[idx] & WORD_MASK;
    }

    public int getIX(int idx) {
        if (idx < 0 || idx > 3) {
            throw new IllegalArgumentException("IX index out of range");
        }
        return IX[idx] & WORD_MASK;
    }

    public void setPC(int pc) {
        if (pc < 0) {
            throw new IllegalArgumentException("PC must be >= 0");
        }
        this.pc = pc & 0x7FF;
    }

    public void setR(int idx, int value) {
        if (idx < 0 || idx > 3) {
            throw new IllegalArgumentException("R index out of range");
        }
        R[idx] = value & WORD_MASK;
    }

    public void setIX(int idx, int value) {
        if (idx < 0 || idx > 3) {
            throw new IllegalArgumentException("IX index out of range");
        }
        if (idx == 0) {
            throw new IllegalArgumentException("IX0 does not exist; valid IX index is 1..3");
        }
        IX[idx] = value & WORD_MASK;
    }

    // ----------------
    // Console I/O helpers
    // ----------------
    public void setConsoleInput(String input) {
        synchronized (consoleInput) {
            consoleInput.setLength(0);
            if (input != null) {
                consoleInput.append(input);
            }
        }
    }

    public String getConsoleOutput() {
        synchronized (consoleOutput) {
            return consoleOutput.toString();
        }
    }

    public void clearConsoleOutput() {
        synchronized (consoleOutput) {
            consoleOutput.setLength(0);
        }
    }

    // ----------------
    // CC helpers
    // ----------------
    public int getCC() {
        return cc & 0xF;
    }

    public boolean getOverflow() {
        return (cc & 0x1) != 0;
    }

    public boolean getUnderflow() {
        return (cc & 0x2) != 0;
    }

    public boolean getDivZero() {
        return (cc & 0x4) != 0;
    }

    public boolean getEqualOrNot() {
        return (cc & 0x8) != 0;
    }

    public void setOverflow(boolean val) {
        if (val)
            cc |= 0x1;
        else
            cc &= ~0x1;
    }

    public void setUnderflow(boolean val) {
        if (val)
            cc |= 0x2;
        else
            cc &= ~0x2;
    }

    public void setDivZero(boolean val) {
        if (val)
            cc |= 0x4;
        else
            cc &= ~0x4;
    }

    public void setEqualOrNot(boolean val) {
        if (val)
            cc |= 0x8;
        else
            cc &= ~0x8;
    }

    public void clearCC() {
        cc = 0;
    }

    public boolean isHalted() {
        return halted;
    }

    public void setHalted(boolean halted) {
        this.halted = halted;
    }
}