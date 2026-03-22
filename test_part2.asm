LOC 6
        DATA 5
        DATA 3
        DATA 10
        DATA 0
        DATA 50

        LOC 20
        LDR 0,0,6
        LDR 1,0,7
        
        AMR 0,0,7
        SMR 0,0,7
        
        AIR 0,10
        SIR 0,5
        
        LDR 0,0,6
        LDR 2,0,7
        
        MLT 0,2
        
        LDR 0,0,8
        LDR 2,0,7
        DVD 0,2
        
        TRR 0,2
        
        LDR 0,0,6
        LDR 1,0,7
        AND 0,1
        
        LDR 0,0,6
        LDR 1,0,7
        ORR 0,1
        
        LDR 0,0,6
        NOT 0
        
        LDR 0,0,6
        SRC 0,2,1,1
        
        LDR 1,0,8
        SRC 1,1,0,1
        
        LDR 2,0,6
        RRC 2,3,1,1
        
        LDR 0,0,6
        AIR 0,5
        JZ 0,0,10
        AIR 0,1
        
        LDR 0,0,9
        JZ 0,0,15
        AIR 0,20
        AIR 0,20
        
        LDR 1,0,6
        JNE 1,0,18
        AIR 1,1
        
        LDR 2,0,7
        SOB 2,0,20
        
        JMA 0,25
        
        LDR 0,0,7
        LDR 1,0,7
        TRR 0,1
        JCC 3,0,28
        AIR 0,31
        
        HLT