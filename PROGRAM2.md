# CSCI 6461 – Program 2 Documentation

## Overview

Program 2 demonstrates full system functionality of the C6461 simulator, including:

* File input (card reader)
* Console input (keyboard)
* Console output (printer)
* Memory traversal
* String comparison logic
* Sentence and word tracking

The program reads a paragraph, prints it, then searches for a user-provided word.

---

## Program Behavior

### Step 1 – Load Paragraph

* The paragraph is read from a file using the **card reader (device 2)**.
* Each character is stored sequentially in memory starting at `PSTART`.

### Step 2 – Print Paragraph

* The program iterates through memory and outputs each character to the printer (device 1).
* Stops when a null terminator (`0`) is encountered.

### Step 3 – Read Search Word

* The program reads user input from the keyboard (device 0).
* The word is stored in memory starting at `WSTART`.

### Step 4 – Initialize Counters

* `SENTNUM ← 1`
* `WORDNUM ← 1`

---

## Search Logic

The program scans the paragraph character by character:

### Word Detection

* Words are separated by:

  * space (`32`)
  * period (`46`)
  * newline (`10`)
  * carriage return (`13`)
  * null (`0`)

### Sentence Handling

* When a period is encountered:

  * `SENTNUM ← SENTNUM + 1`
  * `WORDNUM ← 1`
  * Pointer skips both `.` and following space

### Word Handling

* When a space is encountered:

  * `WORDNUM ← WORDNUM + 1`

---

## Matching Logic

1. At each word start, the program compares:

   * paragraph word (X1)
   * search word (X2)

2. If all characters match:

   * Perform boundary check (next char must be separator)

3. If valid match:

   ```
   <word> <sentence#> <word#>
   ```

4. If no match found after full scan:

   ```
   <word> 0 0
   ```

---

## Memory Layout

| Name    | Address | Description                            |
| ------- | ------- | -------------------------------------- |
| PSTART  | 400     | Paragraph storage                      |
| WSTART  | 700+    | Search word storage (must not overlap) |
| SENTNUM | 12      | Sentence counter                       |
| WORDNUM | 13      | Word counter                           |
| TMP1    | 14      | Temporary pointer storage              |

---

## Important Implementation Details

### 1. Word Boundary Fix

* Ensures matches only occur at full word boundaries

### 2. Sentence Transition Fix

* After `.` the pointer advances twice:

  * skip period
  * skip following space

### 3. Memory Separation Fix

* `WSTART` must be placed beyond paragraph length
* Prevents overwriting paragraph during input

---

## How to Run

1. Click **IPL**
2. Select `program2.asm`
3. Select paragraph file
4. Click **Run**
5. Enter search word in console input
6. Click **Run** again

---

## Example

### Input

```
yard
```

### Output

```
yard 2 6
```

---

## Test Cases

| Input | Output    |
| ----- | --------- |
| the   | the 1 1   |
| cat   | cat 1 2   |
| yard  | yard 2 6  |
| bird  | bird 3 2  |
| field | field 6 6 |
| xyz   | xyz 0 0   |

---

## Conclusion

Program 2 demonstrates:

* correct memory traversal
* correct word detection
* correct sentence tracking
* correct boundary validation
* correct handling of not-found cases

This confirms full functionality of the simulator for Part III.
