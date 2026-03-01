; LDA tests: LDA loads EA itself, not memory contents.
; We'll test:
;   1) direct:      LDA 1,0,25      => R1 = 25
;   2) indexed:     IX1=80; LDA 2,1,10 => R2 = 90
;   3) indirect EA: M[12]=100; LDA 3,0,12,1 => R3 = 100  (EA becomes M[12])
;
; Program begins at 20.

        LOC 7
        DATA 80

        LOC 12
        DATA 100

        LOC 20
        LDA 1,0,25
        LDX 1,7
        LDA 2,1,10
        LDA 3,0,12,1
        HLT