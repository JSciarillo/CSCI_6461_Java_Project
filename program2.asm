; ============================================================
; program2.asm
; Program 2
; - Read paragraph from card reader (device 2)
; - Store paragraph in memory
; - Print paragraph
; - Read search word from keyboard (device 0)
; - Search for exact whole-word match
; - If found, print: <word> <sentence#> <word#>
; - If not found, print: <word> 0 0
; ============================================================

        LOC 6
BOOT:   LDX 3,22              ; X3 <- MAINBASE
        JMA 3,0               ; jump to main block

; ------------------------------------------------------------
; low memory constants / vars / block bases
; actual addresses 8..29
; ------------------------------------------------------------
        LOC 8
SPACECHR:      DATA 32        ; 8
DOTCHR:        DATA 46        ; 9
ASCII0:        DATA 48        ; 10
ONE:           DATA 1         ; 11
SENTNUM:       DATA 0         ; 12
WORDNUM:       DATA 0         ; 13
TMP1:          DATA 0         ; 14
TMP2:          DATA 0         ; 15
NEWLINE:       DATA 10        ; 16
CRCHR:         DATA 13        ; 17
ZERO:          DATA 0         ; 18
PSTART:        DATA 400       ; 19
WSTART:        DATA 800       ; 20
PTRTMP:        DATA 0         ; 21
MAINBASE:      DATA 64        ; 22
INPUTBASE:     DATA 96        ; 23
SCANBASE:      DATA 132       ; 24
COMPBASE:      DATA 168       ; 25
NFBASE:        DATA 208       ; 26
BOUNDARYBASE:  DATA 256       ; 27
FOUNDBASE:     DATA 288       ; 28
MISMATCHBASE:  DATA 320       ; 29

; ------------------------------------------------------------
; main block @64
; read paragraph, print paragraph, jump to input block
; X3 is base = 64 for local jumps
; ------------------------------------------------------------
        LOC 64
MainStart:
        LDX 1,19              ; X1 <- PARASTART

ReadParaLoop:
        IN 0,2
        JZ 0,3,10             ; -> EndReadPara

        STR 0,1,0
        STX 1,21              ; PTRTMP <- X1
        LDR 1,0,21
        AIR 1,1
        STR 1,0,21
        LDX 1,21
        JMA 3,1               ; -> ReadParaLoop

EndReadPara:
        STR 0,1,0             ; zero terminator
        LDX 1,19              ; X1 <- PARASTART

PrintParaLoop:
        LDR 0,1,0
        JZ 0,3,21             ; -> AfterPrintPara
        OUT 0,1

        STX 1,21
        LDR 1,0,21
        AIR 1,1
        STR 1,0,21
        LDX 1,21
        JMA 3,12              ; -> PrintParaLoop

AfterPrintPara:
        LDR 0,0,16            ; NEWLINE
        OUT 0,1

        LDX 3,23              ; X3 <- INPUTBASE
        JMA 3,0

; ------------------------------------------------------------
; input block @96
; read search word from keyboard, init counters, jump to scan
; X3 is base = 96 for local jumps
; ------------------------------------------------------------
        LOC 96
InputBlock:
        LDX 1,20              ; X1 <- WORDSTART

WaitFirstChar:
        CHK 0,0
        JZ 0,3,1              ; stay until keyboard has input

        IN 0,0
        JZ 0,3,1              ; ignore accidental 0 while waiting

ReadWordLoop:
        LDR 1,0,16            ; NEWLINE?
        TRR 0,1
        JCC 3,3,24            ; -> EndReadWord

        LDR 1,0,17            ; CR?
        TRR 0,1
        JCC 3,3,24            ; -> EndReadWord

        LDR 1,0,8             ; SPACE?
        TRR 0,1
        JCC 3,3,24            ; -> EndReadWord

        STR 0,1,0
        STX 1,21              ; PTRTMP <- X1
        LDR 1,0,21
        AIR 1,1
        STR 1,0,21
        LDX 1,21

WaitNextChar:
        CHK 0,0
        JZ 0,3,20             ; -> WaitNextChar

        IN 0,0
        JMA 3,5               ; -> ReadWordLoop

EndReadWord:
        LDR 0,0,18            ; ZERO
        STR 0,1,0

        LDR 0,0,11            ; ONE
        STR 0,0,12            ; SENTNUM <- 1
        STR 0,0,13            ; WORDNUM <- 1

        LDX 1,19              ; X1 <- PARASTART
        LDX 3,24              ; X3 <- SCANBASE
        JMA 3,0

; ------------------------------------------------------------
; scan block @132
; X3 is base = 132
; ------------------------------------------------------------
        LOC 132
ScanLoop:
        LDR 0,1,0
        JZ 0,3,18             ; -> NotFoundDispatch

        LDR 1,0,8             ; space?
        TRR 0,1
        JCC 3,3,20            ; -> SepDispatch

        LDR 1,0,9             ; dot?
        TRR 0,1
        JCC 3,3,22            ; -> DotDispatch

        LDR 1,0,16            ; newline?
        TRR 0,1
        JCC 3,3,20            ; -> SepDispatch

        LDR 1,0,17            ; carriage return?
        TRR 0,1
        JCC 3,3,20            ; -> SepDispatch

        STX 1,14              ; TMP1 <- current paragraph word start
        LDX 2,20              ; X2 <- WORDSTART
        LDX 3,25              ; X3 <- COMPBASE
        JMA 3,0

NotFoundDispatch:
        LDX 3,26              ; X3 <- NFBASE
        JMA 3,0

SepDispatch:
        LDX 3,26
        JMA 3,19              ; -> SepEntry

DotDispatch:
        LDX 3,26
        JMA 3,29              ; -> DotEntry

; ------------------------------------------------------------
; compare block @168
; X3 is base = 168
; ------------------------------------------------------------
        LOC 168
CompareLoop:
        LDR 0,2,0
        JZ 0,3,18             ; -> BoundaryDispatch

        LDR 1,1,0
        TRR 0,1
        JCC 3,3,7             ; -> CharsMatch

        LDX 3,29              ; X3 <- MISMATCHBASE
        JMA 3,0

CharsMatch:
        STX 1,21              ; PTRTMP <- X1
        LDR 1,0,21
        AIR 1,1
        STR 1,0,21
        LDX 1,21

        STX 2,15              ; TMP2 <- X2
        LDR 0,0,15
        AIR 0,1
        STR 0,0,15
        LDX 2,15

        JMA 3,0               ; -> CompareLoop

BoundaryDispatch:
        LDX 3,27              ; X3 <- BOUNDARYBASE
        JMA 3,0

; ------------------------------------------------------------
; not-found / separator / dot block @208
; X3 is base = 208
; ------------------------------------------------------------
        LOC 208
NotFoundEntry:
        LDX 2,20              ; X2 <- WORDSTART

NFPrintWord:
        LDR 0,2,0
        JZ 0,3,10
        OUT 0,1

        STX 2,15              ; TMP2 <- X2
        LDR 0,0,15
        AIR 0,1
        STR 0,0,15
        LDX 2,15
        JMA 3,1

        LDR 0,0,8             ; space
        OUT 0,1
        LDR 0,0,10            ; ASCII0
        OUT 0,1
        LDR 0,0,8             ; space
        OUT 0,1
        LDR 0,0,10            ; ASCII0
        OUT 0,1
        HLT

SepEntry:
        LDR 0,0,13            ; WORDNUM
        AIR 0,1
        STR 0,0,13

        STX 1,14              ; TMP1 <- X1
        LDR 0,0,14
        AIR 0,1
        STR 0,0,14
        LDX 1,14

        LDX 3,24              ; X3 <- SCANBASE
        JMA 3,0

DotEntry:
        LDR 0,0,12            ; SENTNUM
        AIR 0,1
        STR 0,0,12

        LDR 0,0,11            ; ONE
        STR 0,0,13            ; WORDNUM <- 1

        STX 1,14              ; TMP1 <- X1
        LDR 0,0,14
        AIR 0,1
        STR 0,0,14
        LDX 1,14

        STX 1,14
        LDR 0,0,14
        AIR 0,1
        STR 0,0,14
        LDX 1,14

        LDX 3,24
        JMA 3,0

; ------------------------------------------------------------
; boundary block @256
; X3 is base = 256
; ------------------------------------------------------------
        LOC 256
BoundaryCheck:
        LDR 0,1,0             ; load paragraph char after matched word

        JZ 0,3,16             ; -> FoundDispatch if paragraph ended

        LDR 1,0,8             ; space?
        TRR 0,1
        JCC 3,3,16

        LDR 1,0,9             ; dot?
        TRR 0,1
        JCC 3,3,16

        LDR 1,0,16            ; newline?
        TRR 0,1
        JCC 3,3,16

        LDR 1,0,17            ; carriage return?
        TRR 0,1
        JCC 3,3,16

        LDX 3,29              ; X3 <- MISMATCHBASE
        JMA 3,0

FoundDispatch:
        LDX 3,28              ; X3 <- FOUNDBASE
        JMA 3,0

; ------------------------------------------------------------
; found block @288
; X3 is base = 288
; ------------------------------------------------------------
        LOC 288
FoundEntry:
        LDX 2,20              ; X2 <- WORDSTART

PrintFoundWord:
        LDR 0,2,0
        JZ 0,3,10
        OUT 0,1

        STX 2,15              ; TMP2 <- X2
        LDR 0,0,15
        AIR 0,1
        STR 0,0,15
        LDX 2,15
        JMA 3,1

        LDR 0,0,8             ; space
        OUT 0,1

        LDR 0,0,12            ; SENTNUM
        AMR 0,0,10            ; + ASCII0
        OUT 0,1

        LDR 0,0,8             ; space
        OUT 0,1

        LDR 0,0,13            ; WORDNUM
        AMR 0,0,10            ; + ASCII0
        OUT 0,1

        HLT

; ------------------------------------------------------------
; mismatch block @320
; X3 is base = 320
; ------------------------------------------------------------
        LOC 320
MismatchEntry:
        LDX 1,14              ; X1 <- saved word start

MismatchLoop:
        LDR 0,1,0
        JZ 0,3,29             ; -> ReturnScanOnly

        LDR 1,0,8             ; space?
        TRR 0,1
        JCC 3,3,21            ; -> MismatchSep

        LDR 1,0,9             ; dot?
        TRR 0,1
        JCC 3,3,31            ; -> MismatchDot

        LDR 1,0,16            ; newline?
        TRR 0,1
        JCC 3,3,21            ; -> MismatchSep

        LDR 1,0,17            ; carriage return?
        TRR 0,1
        JCC 3,3,21            ; -> MismatchSep

        STX 1,14              ; TMP1 <- X1
        LDR 0,0,14
        AIR 0,1
        STR 0,0,14
        LDX 1,14
        JMA 3,1

MismatchSep:
        LDR 0,0,13            ; WORDNUM
        AIR 0,1
        STR 0,0,13

        STX 1,14              ; TMP1 <- X1
        LDR 0,0,14
        AIR 0,1
        STR 0,0,14
        LDX 1,14

ReturnScanOnly:
        LDX 3,24              ; X3 <- SCANBASE
        JMA 3,0

MismatchDot:
        LDR 0,0,12            ; SENTNUM
        AIR 0,1
        STR 0,0,12

        LDR 0,0,11            ; ONE
        STR 0,0,13            ; WORDNUM <- 1

        STX 1,14              ; TMP1 <- X1
        LDR 0,0,14
        AIR 0,1
        STR 0,0,14
        LDX 1,14

        STX 1,14
        LDR 0,0,14
        AIR 0,1
        STR 0,0,14
        LDX 1,14

        LDX 3,24
        JMA 3,0

; ------------------------------------------------------------
; buffers
; ------------------------------------------------------------
        LOC 400
PARABUF: DATA 0

        LOC 440
WORDBUF: DATA 0