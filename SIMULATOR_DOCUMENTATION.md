# CSCI 6461 -- Simulator Project (Part I)

**Team:** Oliver Krisetya, Jasmine Sciarillo, Srikar Atluri
**Course:** CSCI 6461 Computer Architecture
**Semester:** Spring 2026

## Compiling and Running the Simulator

### Requirements

- Java 17+
- Windows / macOS / Linux
- No external dependencies required

### Compiling the Simulator

From the project root:

``` bash
javac -d out src/*.java
```

### Running the GUI Simulator

``` powershell
java -cp out src.GUI
```

### Running the Simulator Test Suite

To run all simulator tests:

``` powershell
java -cp out src.SimTest
```

To run a specific simulator test:

``` powershell
java -cp out src.SimTest 1_basic_ldr_str
```

## Part I Scope

Part I implements:

- HLT
- LDR
- STR
- LDA
- LDX
- STX
- Fetch / Decode / Execute cycle
- Effective Address computation
- Single-port memory
- Single-step and run modes

Floating-point, I/O, branching, and cache are not required until later
parts.

## System Overview

The simulator models a **16-bit CISC-style architecture**.

### Machine Properties

- Word size: 16 bits
- Maximum memory: 2048 words
- Addressable range: 0 -- 2047
- Opcode field: 6 bits
- Register field: 2 bits
- Index field: 2 bits
- Indirect bit: 1 bit
- Address field: 5 bits

## Architecture Components

### CPU

#### General Purpose Registers

- R0
- R1
- R2
- R3

#### Index Registers

- IX1
- IX2
- IX3

(IX field value 0 means "no indexing" --- IX0 does not exist.)

#### Special Registers

- PC -- Program Counter
- MAR -- Memory Address Register
- MBR -- Memory Buffer Register
- IR -- Instruction Register

### Memory

- 2048 words maximum
- 16-bit word size
- Address bounds enforcement
- All memory initialized to 0 on reset


## Fetch--Decode--Execute Cycle

### Fetch

    MAR ← PC
    MBR ← Memory[MAR]
    IR  ← MBR
    PC  ← PC + 1

### Decode

    Opcode  = IR[15:10]
    R       = IR[9:8]
    IX      = IR[7:6]
    I       = IR[5]
    Address = IR[4:0]

### Effective Address

    EA = Address
    If IX ≠ 0 → EA = EA + IX[IX]
    If I = 1 → EA = Memory[EA]

## Implemented Instructions

### HLT (Opcode 00)

Halts execution.

### LDR  r, x, address[,I]

```
R[r] ← Memory[EA]
```

### STR r, x, address[,I]

```
Memory[EA] ← R[r]
```

### LDA r, x, address[,I]

```
R[r] ← EA
```

### LDX x, address[,I]

```
IX[x] ← Memory[EA]
```

### STX x, address[,I]

```
Memory[EA] ← IX[x]
```

## Error Handling

- Unknown opcode → CPU halts
- Invalid IX usage → CPU halts
- Memory bounds violation → CPU halts

## Design Decisions

- PC increments during fetch.
- Branch/transfer instructions (future parts) will override PC in execute stage.
- All values masked to 16 bits.
- IX[0] is unused.
- Memory size is configurable but capped at 2048 words.

## Trace Output
Each instruction prints a debug trace:

```
Executing: PC=XX MAR=XX IR=XXXX Opcode=XX
```

This helps debug instruction sequencing and EA computating

## Summary

Part I delivers:

- Working 16-bit CPU simulator
- Fully functional load/store instructions
- Accurate effective address computation
- GUI front panel
- Deterministic simulator test suite

The simulator correctly models:

- Instruction fetch cycle
- Address calculation
- Register/memory interaction
- Halt behavior
- Memory protection