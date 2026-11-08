# School Library Management System (Java)

A console-based library system built around the supplied `LibraryMember` /
`StudentMember` / `FacultyMember` / `StaffMember` / `Book` classes, completed
with a text-file persistence layer, borrowing receipts, and a return/check-in
workflow with overdue fines.

## Features

- **Two login roles** — staff (`StaffMember`) and library members
  (`StudentMember` / `FacultyMember`), each with a 3-attempt login.
- **Inheritance and polymorphism** — `StudentMember` (limit 3) and
  `FacultyMember` (limit 10) override `getMaxBorrowLimit()`.
- **Inventory management** — staff can add titles, add copies to an existing
  ISBN, remove titles (blocked while copies are on loan), and browse stock.
- **Member management** — staff can register students/faculty and additional
  staff accounts.
- **Borrowing rules** — per-role limits, no duplicate active loan for the same
  title, no borrowing with an unpaid penalty, and no borrowing when all copies
  are out.
- **Return / check-in** — closes the active loan, restores the copy, calculates
  an overdue fine (RM 5.00 per day), and updates the member's balance.
- **Receipts** — every borrow and return prints a receipt and appends it to
  `receipts.txt`.
- **Persistence** — all state is reloaded from plain-text files on startup.
- **Penalty payments** — members can pay their own balance; staff can collect
  payments and give change.

## Project layout

```
SchoolLibrarySystem/
├── src/
│   ├── LibraryMember.java      base class (encapsulated identity + penalty)
│   ├── StudentMember.java      borrow limit 3
│   ├── FacultyMember.java      borrow limit 10
│   ├── StaffMember.java        staff credentials
│   ├── Book.java               title + available copy counter
│   ├── Loan.java               one member/book borrowing period
│   ├── FileHandler.java        members/staff/books/loans/receipts persistence
│   ├── Library.java            business rules, fines, receipts
│   └── Main.java               console menus and input handling
├── run.ps1                     compile + run (Windows PowerShell)
├── run.bat                     compile + run (cmd.exe)
└── README.md
```

## Run it

Requirements: JDK 17 or newer (`java -version`, `javac -version`).

Windows PowerShell:

```powershell
cd SchoolLibrarySystem
.\run.ps1
```

If Windows blocks local scripts, either run
`powershell -ExecutionPolicy Bypass -File .\run.ps1` or use `run.bat`.
Passwords are typed as a normal visible line so the login never appears
unresponsive; a production system should mask the input and hash the stored
password instead.

Command Prompt:

```bat
cd SchoolLibrarySystem
run.bat
```

Manual build:

```bat
javac -encoding UTF-8 -d out src\*.java
java -cp out Main
```

Run from the project root so the `.txt` files are created there.

## Demo accounts

| Role    | ID   | Password   |
|---------|------|------------|
| Staff   | S001 | admin123   |
| Student | M001 | student123 |
| Student | M002 | student123 |
| Faculty | F001 | faculty123 |

On first launch the app seeds these accounts plus four books:
*Clean Code*, *Effective Java*, *Introduction to Algorithms*, and
*The Pragmatic Programmer*.

## Rules

| Rule                | Value                          |
|---------------------|--------------------------------|
| Loan period         | 14 days                        |
| Overdue fine        | RM 5.00 per day, whole days    |
| Student borrow limit| 3 active loans                 |
| Faculty borrow limit| 10 active loans                |
| Base member limit   | 2 active loans                 |

Borrowing is refused when the member has an unpaid penalty, has reached the
role limit, already has that title out, or no copy is available. Fines are
charged when the book is returned and must be cleared before the next borrow.

## Data files

Created automatically in the working directory:

| File           | Format |
|----------------|--------|
| `members.txt`  | `TYPE\|name\|memberId\|password\|penaltyBalance` |
| `staff.txt`    | `name\|staffId\|password` |
| `books.txt`    | `title\|author\|isbn\|availableCopies` |
| `loans.txt`    | `memberId\|isbn\|borrowDate\|dueDate\|returnDate` (`-` = still out) |
| `receipts.txt` | appended printable borrow/return receipts |

Values are pipe-delimited and dates use ISO `yyyy-MM-dd`. Pipes and newlines in
user input are sanitised before writing.

## Design notes

- `LibraryMember` keeps its fields `private`; subclasses only change behaviour
  through `getMaxBorrowLimit()`, which is called polymorphically by `Library`.
- `Library` owns all rules and persistence calls, so `Main` cannot bypass a
  limit or fine.
- `FileHandler` uses `PrintWriter` for writing and `Scanner` for reading, with
  one loader/saver per entity. Malformed rows are skipped with a warning rather
  than crashing the app.
- `Library` exposes date-parameterised overloads (`borrowBook(..., LocalDate)`
  and `returnBook(..., LocalDate)`) so overdue-fine behaviour can be tested
  without waiting for real time to pass.

## Verified scenarios

Tested against a clean workspace with JDK 17:

1. Member `M001` borrows *Clean Code* → stock drops from 3 to 2, receipt printed
   and written to `receipts.txt`; state survives a restart.
2. Returning a loan seven days late → `Days Late: 7`, `Fine: RM 35.00`, balance
   updated; a new borrow is then blocked until the RM 35.00 is paid.
3. Borrowing beyond the student limit (3) is blocked.
4. Borrowing a title the member already has out is blocked.
5. Staff can add a book, add copies, register a member, and are prevented from
   removing a title with an active loan.
6. Staff payment collection applies the balance, reports change, and persists
   the zero balance.

## Security note

The supplied model stores and compares passwords as plain text. That keeps this
teaching example faithful to the original classes, but it is **not suitable for
real deployment**. In production, store a salted slow hash (BCrypt, Argon2, or
PBKDF2) instead of the password, never write it to a text file, and never print
it. A production version should also use a real database with transactions
instead of rewriting whole text files.

This app is intentionally single-user/local; there is no concurrent-access
locking around the data files.
