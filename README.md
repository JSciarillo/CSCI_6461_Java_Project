# CSCI 6461 - Computer Architecture Class Project

A Java simulator for the **C6461** computer architecture, built for GWU's CSCI 6461 (Computer Architectures) course. The project implements an assembler and a full instruction-cycle simulator — including registers, memory, cache, and an operator's console GUI — developed in four progressive segments over the course of a semester.
 
## Overview
 
The C6461 is a simplified 16-bit instruction-set computer used to teach core computer architecture concepts: instruction fetch/decode/execute, addressing modes, memory and cache design, and basic I/O. This repository contains:
 
- **An assembler** that translates C6461 symbolic assembly into 16-bit octal machine code, along with a listing file showing each instruction's memory address and encoded value.
- **A CPU simulator** with a GUI operator's console (and an optional field engineer's console) that loads, single-steps, and runs machine-code programs.
- **Two demonstration programs** written in C6461 assembly that exercise the instruction set end to end.
## Project Structure
 
```
├── src/                          # Simulator and assembler source code
├── jars/                         # Packaged, runnable JAR builds
├── tests/                        # Test cases for the assembler and simulator
├── program1.asm                  # Program 1 source (closest-number search)
├── test_part2.asm                # Program 2 / Part II test source
├── source_file.txt               # Sample assembler input
├── manifest.txt                  # JAR manifest (main class entry point)
├── ASSEMBER_DOCUMENTATION.md     # How to build/run/use the assembler
├── SIMULATOR_DOCUMENTATION.md    # How to build/run/use the simulator & console
├── CACHE_DOCUMENTATION.md        # Cache design (Part II)
└── GITHUB_LOGS.txt               # Commit/submission log for grading
```
 
## Requirements
 
- **Java JDK 1.8** or later
- No external dependencies — the project builds and runs with the standard JDK
## Building and Running
 
Pre-built JAR files are provided under `jars/`. To run the assembler or simulator directly:
 
```bash
java -jar jars/<assembler-jar-name>.jar
java -jar jars/<simulator-jar-name>.jar
```
 
To rebuild from source:
 
```bash
javac -d out $(find src -name "*.java")
jar cfm CSCI6461.jar manifest.txt -C out .
java -jar CSCI6461.jar
```
 
See `ASSEMBER_DOCUMENTATION.md` and `SIMULATOR_DOCUMENTATION.md` for detailed, up-to-date build and usage instructions, including how the assembler's input/output files are formatted and how the simulator's console controls work.
 
## The Assembler
 
The assembler reads a symbolic source file (e.g. `source_file.txt`), resolves `LOC` directives and labels, and emits a listing file containing each instruction's octal address and its 16-bit octal encoding (opcode, register, index register, indirection bit, and address fields). Example:
 
```
LOC 10
LDR 3,0,15
```
 
encodes to `003417` (octal) at address `10` (decimal).
 
## The Simulator
 
The simulator models:
 
- **Registers**: general-purpose registers (R0–R3), index registers, PC, MAR, MBR, IR, and condition/status registers
- **Memory**: a 2048-word single-port memory, zeroed on power-up
- **Cache** *(Part II+)*: a fully associative, unified cache using FIFO replacement (16 lines)
- **Operator's console**: register/status displays, an IPL button, and Run / Halt / Single-Step controls, plus switches to load registers and memory
- **Field engineer's console** *(optional)*: internal-state views for debugging the simulator itself
On IPL, the simulator prompts for a program file to load (simulating a ROM/card load), then halts at the start of the loaded program awaiting Run or Single Step.
 
## Test Programs
 
1. **Program 1** — Reads 20 signed integers from the keyboard, echoes them to the console printer, then reads a target number and prints the value from the list closest to it.
2. **Program 2** — Loads a six-sentence paragraph from a file, prints it to the console, then searches for a user-supplied word and reports its sentence number and word position if found.
## Project Segments
 
This repository tracks work across the four graded segments of the course project:
 
| Segment | Focus |
|---|---|
| 0 – Assembler | Symbolic-to-octal assembler |
| I – Basic Machine | Core registers, simple memory, Load/Store instructions, initial console UI |
| II – Memory & Cache | Cache design, (nearly) full instruction set, Program 1 demo |
| III – Execute All Instructions | Complete instruction set, Program 2 demo |
| IV – Floating Point/Vector *or* Enhanced Scheduling | One of: FP/vector ops with pipelining, or branch prediction/speculative execution with trap handling |
 
Full requirements for each segment are in the course's project description document.
 
## Documentation
 
- `ASSEMBER_DOCUMENTATION.md` — assembler usage and I/O format
- `SIMULATOR_DOCUMENTATION.md` — simulator/console usage and layout
- `CACHE_DOCUMENTATION.md` — cache line format and replacement policy
- `GITHUB_LOGS.txt` — submission/commit history for grading
## Course Context
 
Developed for **CSCI 6461: Computer Architectures**, Department of Computer Science, George Washington University.
 
