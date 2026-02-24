package src;

public class CPU {
    int[] R = new int[4]; // R0 - R3
    int pc;
    int mar;
    int mbr;
    int ir;

    public void signleStep(){}

    private void fetch(){}
    private void decode(){}
    private void computeEffectiveAddress(){}
    private void execute(){}
}
