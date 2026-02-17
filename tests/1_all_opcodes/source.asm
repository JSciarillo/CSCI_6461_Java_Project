        LOC 6
Start:  DATA End
        DATA 10

        ; Load/Store
        LDR 0,0,1
        STR 0,0,2
        LDA 1,0,3
        LDX 1,4
        STX 1,5

        ; Transfer
        JZ 0,0,6
        JNE 0,0,7
        JCC 0,0,8
        JMA 0,9
        JSR 0,10
        RFS 31
        SOB 0,0,11
        JGE 0,0,12

        ; Arithmetic mem/immed
        AMR 0,0,13
        SMR 0,0,14
        AIR 2,15
        SIR 2,1

        ; Reg-Reg ops (use legal regs for MLT/DVD)
        MLT 0,2
        DVD 2,0
        TRR 1,3
        AND 0,1
        ORR 2,3
        NOT 3

        ; Shift/Rotate
        SRC 1,3,1,1
        RRC 2,4,0,1

        ; I/O
        IN 0,0
        OUT 1,1
        CHK 2,1

End:    HLT
