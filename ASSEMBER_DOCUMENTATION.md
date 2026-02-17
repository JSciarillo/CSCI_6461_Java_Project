# CSCI 6461 - Assembler Project 0

**Team:** Oliver Krisetya, Jasmine Sciarillo, Srikar Atluri
**Course:** CSCI 6461 Computer Architecture
**Semester:** Spring 2026

## Compiling and Running the Assembler

### Requirements

- Java 17+
- Windows / macOS / Linux
- No external dependencies required

### Compiling the Assembler

From the project root:

```bash
javac -d out src\Assembler.java src\AsmTest.java
```

This compiles all classes into:

```bash
out/src/*.class
```

### Running the Assembler

To assemble a source file:

```powershell
java -cp out src.Assembler \tests\test1\source.asm
```

This generates:

```pgsql
actual.listing
actual.load
```

in the test directory

If no argument is provided, the assembler will attempt to assemble the default file specified in ``Assembler.main()``

## Running the Test Suite

To run all test folders:

```powershell
java -cp out src.AsmTest
```

To run a specific test folder:

```powershell
java -cp out src.AsmTest test1
```

Each test folder must contain:

```cpp
source.asm
expected.load
(optional) expected.listing
```

Test results will display PASS/FAIL and show differences if mismatches occur.

## Creating the Executable JAR

After compilation:

```powershell
jar cfe C6461Assembler.jar src.Assembler -C out .
```

To execute the JAR:

```powershell
java -jar C6461Assembler.jar tests\test1\source.asm
```

## Assembler Workflow

The assembler uses a two-pass design.

### Pass 1 - Symbol Resolution

- Initialize Location Counter (LC) to 0.
- Parse each source line.
- Record labels and assign addresses.
- Process directives:
  - LOC sets LC.
  - DATA allocates one word.
- Record errors for:
  - Duplicate labels
  - Unknown opcodes

Output:

- Symbol table
- Address assigned to each instruction
- Pass 1 error list

### Pass 2 - Instruction Encoding

- Resolve operands (decimal values or labels).
- Validate operand ranges.
- Encode 16-bit machine word.
- Write:

Listing Format:

```css
AAAAAA  WWWWWW  original source line
```

Load Format:

```css
AAAAAA  WWWWWW
```

All values are 6-digit octal.

Lines with errors:

- Included in listing with error message.
- Not included in load file.

## Design Notes

### Class Responsibilities

#### Assembler

- Controls pass one and pass two
- Manages file I/O
- Produces Listing and Load files

#### AsmLine

- Represents a parsed source line.
- Stores label, opcode, operands, address.

#### Pass1Result

- Symbol table
- Parsed lines
- Errors

#### Pass2Result

- Encoding errors

#### EncodeResult

- Machine word
- Operand validation errors

#### AssemblerResult

- Aggregated assembly result
- Used by CLI and test harness

## Error Handling

Errors are categorized as:

### Pass 1 Errors

- Duplicate label
- Unknown opcode/directive

### Pass 2 Errors

- Undefined symbol
- Operand out of range
- Register constraint violations
- Invalid immediate values

Errors are reported clearly and do not crash the assembler.

## Testing

Test folders are located in:

```css
tests/
```

Each test verifies:

- Correct symbol resolution
- Correct encoding
- Correct output formatting
- Error detection (if applicable)

The test harness compares actual output to expected output using golden-file comparison.
