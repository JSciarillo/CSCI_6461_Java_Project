; Bounds behavior test (optional):
; EA = IX1 + 31 should go out of 2048-word memory if IX1 is near end.
;
; Setup:
;   IX1 = 2040 (from M[7])
;   LDR 0,1,31 => EA = 2040 + 31 = 2071 (out of bounds)
;
; Expected behavior depends on your policy:
; - If you HALT on error: R0 stays 0 and program stops early.
; - If exception escapes: test harness should treat it as expected.
;
; Only include this test once your harness expects a fault.

        LOC 7
        DATA 2040

        LOC 20
        LDX 1,7
        LDR 0,1,31
        HLT