; Simple Part I test: LDR/STR/LDA + halt
LOC 10
DATA 0       ; placeholder

; Put a value at address 20
LOC 20
DATA 4660    ; 0x1234 decimal

; Program begins at 30
LOC 30
LDR 0,0,20   ; R0 <- M[20] (direct)
STR 0,0,21   ; M[21] <- R0
HLT

LOC 21
DATA 0