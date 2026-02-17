        LOC 60          ; start at decimal 60 (octal 000074)

Start:  DATA End        ; symbol resolution: address of End into memory
        MLT 0,2         ; valid: rx in {0,2}, ry in {0,2}
        DVD 2,0
        TRR 1,3         ; equality flag behavior (assembler encodes)
        AND 0,1
        ORR 2,3
        NOT 3
End:    HLT             ; label target
