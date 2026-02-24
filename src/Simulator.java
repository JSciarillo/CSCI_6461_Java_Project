package src;

public final class Simulator {
    private final CPU cpu;
    private final Memory mem;

    public Simulator(int memSizeWords, int wordBits) {
        this.cpu = new CPU();
        this.mem = new Memory(memSizeWords, wordBits);
        this.cpu.reset();
    }

    public void reset(){
        cpu.reset();
        // Maybe add clear memory
    }

    public void depositMemory(int address, int value) {
        mem.write(address, value);
    }

    public int readMemory(int address) {
        return mem.read(address);
    }

    public void setPC(int address) {
        cpu.setPC(address);
    }

    public void setRegister(int rIndex, int value) {
        cpu.setR(rIndex, value);
    }

    public void singleStep(){
        cpu.signleStep(mem);
    }

    public CPU getCPU() { return cpu; }
    public Memory getMemory() { return mem; }

    public int getMemoryAtMAR() {
        return mem.read(cpu.getMAR());
    }

    // Temp test
    public static void main(String[] args) {
        Simulator s = new Simulator(2048, 16);

        int A = 100;
        int X = 0x1234;

        s.depositMemory(A, X);
        s.setPC(A);

        s.singleStep();

        System.out.println("MAR=" + s.getCPU().getMAR());
        System.out.println("IR=" + s.getCPU().getIR());
        System.out.println("PC=" + s.getCPU().getPC());
        System.out.println("MEM[MAR]=" + s.getMemoryAtMAR());
    }
}