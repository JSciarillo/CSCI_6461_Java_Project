; Indexed + Indirect combo:
;   base = IX1 + addressField
;   EA = M[base] (because I=1)
;   R0 = M[EA]
;
; Setup:
;   IX1 = 80 (from M[7])
;   base = 80 + 10 = 90
;   M[90] = 120 (pointer)
;   M[120] = 0x1234 (4660)
;
; Program:
;   LDX 1,7
;   LDR 0,1,10,1  => R0 = M[M[90]] = M[120] = 0x1234
;   HLT

        LOC 7
        DATA 80

        LOC 90
        DATA 120

        LOC 120
        DATA 4660

        LOC 20
        LDX 1,7
        LDR 0,1,10,1
        HLT