; Indexed addressing test: EA = IX1 + addressField
; We'll place data at M[100] and store result at M[101].
;
; Data:
;   M[100] = 0x0BAD (2989)
; Program:
;   IX1 <- M[7] (90)
;   R0 <- M[IX1 + 10] = M[100]
;   M[IX1 + 11] <- R0 = M[101]
;   HLT

        LOC 7
        DATA 90

        LOC 100
        DATA 2989

        LOC 20
        LDX 1,7        ; IX1 = 90
        LDR 0,1,10     ; EA=90+10=100 -> R0=0x0BAD
        STR 0,1,11     ; EA=90+11=101 -> M[101]=R0
        HLT

        LOC 101
        DATA 0