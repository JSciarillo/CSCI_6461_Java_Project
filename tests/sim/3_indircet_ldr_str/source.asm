; Indirect addressing test: I=1 => EA = M[addressField]
; Then load/store through pointer.
;
; Setup:
;   M[12] = 100      (pointer)
;   M[100] = 0xBEEF  (48879)
; Program:
;   R0 <- M[M[12]] = M[100]
;   M[M[13]] <- R0, where M[13]=101
;   HLT

        LOC 12
        DATA 100       ; pointer to 100
        DATA 101       ; pointer to 101  (at LOC 13)

        LOC 100
        DATA 48879     ; 0xBEEF

        LOC 20
        LDR 0,0,12,1   ; indirect load via M[12]
        STR 0,0,13,1   ; indirect store via M[13]
        HLT

        LOC 101
        DATA 0