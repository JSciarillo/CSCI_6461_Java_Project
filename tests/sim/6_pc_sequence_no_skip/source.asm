; PC sequencing sanity:
; If PC increments twice per instruction, the second LDR will be skipped.
;
; Setup:
;   M[6]=42 (0x002A)
;   M[7]=100 (0x0064)
; Program:
;   LDR R0 <- M[6]
;   LDR R1 <- M[7]
;   HLT

        LOC 6
        DATA 42
        DATA 100

        LOC 20
        LDR 0,0,6
        LDR 1,0,7
        HLT