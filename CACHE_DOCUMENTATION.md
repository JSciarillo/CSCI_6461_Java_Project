# CSCI 6461 -- Cache Project (Part II)

**Team:** Oliver Krisetya, Jasmine Sciarillo, Srikar Atluri  
**Course:** CSCI 6461 Computer Architecture  
**Semester:** Spring 2026  

---

## Part II Scope

Part II extends the Part I simulator to support a complete execution environment including:

- Arithmetic instructions (AMR, SMR, AIR, SIR)
- Register-to-register operations (MLT, DVD, TRR, AND, ORR, NOT)
- Shift and rotate operations (SRC, RRC)
- Control flow instructions (JZ, JNE, JCC, JMA, JSR, RFS, SOB, JGE)
- Input/Output instructions (IN, OUT, CHK)
- Condition Code (CC) register
- Fully associative cache
- Application-level program (Program 1)

---

## System Overview

The simulator models a **16-bit CISC-style architecture** with:

- Word size: 16 bits  
- Memory size: up to 2048 words  
- Address field: 5 bits (0–31 direct addressing)  
- Extended addressing via index registers  

The system consists of:

- CPU (instruction execution)
- Memory (main storage)
- Cache (performance layer)
- GUI (user interaction)

---

## Condition Code (CC)

The Condition Code (CC) register is implemented as a 4-bit register:

| Bit | Meaning |
|-----|--------|
| 0 | Overflow |
| 1 | Underflow |
| 2 | Divide-by-zero |
| 3 | Equal (result of TRR) |

### Behavior

- Arithmetic instructions update overflow/underflow bits
- Division sets divide-by-zero flag when applicable
- TRR sets equality flag
- Branch instructions (e.g., JCC) depend on CC values

---

## Input / Output Design

### IN Instruction

```
R[r] ← next character from input buffer
```


- Input is provided through the GUI
- Each call to IN reads one character
- If input buffer is empty → returns 0

### OUT Instruction

```
console ← console + character(R[r])
```


- Outputs the lower 8 bits as an ASCII character
- Output appears in GUI console

### CHK Instruction

- Returns device readiness (always ready in this implementation)

---

## Cache Design

### Structure

- Fully associative cache
- Fixed number of cache lines
- Each line contains:
  - valid bit
  - tag
  - data word

### Replacement Policy

- FIFO (First-In, First-Out)

### Write Policy

- Write-through:
  - Writes update both cache and memory

### Operation

#### Read:
1. Search all cache lines
2. If match → cache hit
3. If not → cache miss → fetch from memory → insert into cache

#### Write:
1. Update memory
2. Update cache (if present)

### Observations

- First access to any address results in a **cache miss**
- Repeated accesses result in **cache hits**
- Cache behavior is visible in GUI

---

## Instruction Set Additions

### Arithmetic

- AMR: Add memory to register
- SMR: Subtract memory from register
- AIR: Add immediate
- SIR: Subtract immediate

### Register Operations

- MLT: Multiply register pair
- DVD: Divide register pair
- TRR: Test register equality
- AND, ORR, NOT: Logical operations

### Shift / Rotate

- SRC: Shift register
- RRC: Rotate register

### Control Flow

- JZ, JNE: Conditional jumps
- JCC: Jump on condition code
- JMA: Unconditional jump
- JSR / RFS: Subroutine support
- SOB: Loop control
- JGE: Conditional comparison

---

## Program 1

### Description

Program 1 implements a closest-value search:

1. Read 20 input values
2. Store them in memory
3. Read a target value
4. Compute closest value from stored set
5. Output target and closest match

---

## Important Implementation Detail

Due to the simulator I/O design:

- Input is processed **character-by-character**
- Each value is treated as a **single ASCII character**
- Program is designed for digit inputs (`0–9`)

Example:

```
Input: 123456789012345678905
```


- First 20 → stored values
- Last character → target

---

## Memory Layout

| Address Range | Usage |
|--------------|------|
| 0–7 | Variables |
| 8–31 | Instructions |
| 60–79 | Array storage |

---

## Algorithm

For each stored value:

1. Compute:

```
diff = value - target
```

2. Convert to absolute value:

```
if diff < 0 → diff = -diff
```

3. Compare with best difference:
- if smaller → update best value

---

## Example Execution

### Input

```
123456789012345678905
```

### Output

```
123456789012345678905
```


Explanation:

- First 20 characters echoed during input
- Target = 5
- Closest value = 5

---

## Testing Instructions

1. Run simulator:

```
java -jar C6461Simulator.jar
```


2. Click **IPL (Load Program)**

3. Load:

```
program1.asm
```

4. Enter input:

```
123456789012345678905
```


5. Click **Send**

6. Click **Run**

---

## Design Decisions

- Fully associative cache chosen for simplicity
- FIFO replacement ensures deterministic behavior
- Write-through policy keeps memory consistent
- Character-based I/O simplifies implementation
- Index registers used to overcome 5-bit address limitation
- Program layout avoids instruction/data overlap

---

## Limitations

- Address field limited to 5 bits (0–31)
- Input is character-based (not full integer parsing)
- Output is ASCII-based
- No floating-point support in this part

---

## Summary

Part II extends the simulator into a complete system with:

- Full instruction set
- Working cache
- Functional I/O system
- Application-level program execution

The simulator now supports:

- arithmetic and logic
- control flow
- memory hierarchy
- interactive execution
