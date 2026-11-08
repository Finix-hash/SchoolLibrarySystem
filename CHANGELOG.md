# Change History

This project started from the supplied `LibraryMember`, `StudentMember`,
`FacultyMember`, `StaffMember`, and `Book` classes. Everything else was added
during this session, then the password input was fixed twice.

## 1. Initial build (files added)

New files created:

| File | Purpose |
|------|---------|
| `src/Loan.java` | Borrowing period, due date, overdue-day calculation |
| `src/FileHandler.java` | Persistence for members, staff, books, loans, receipts |
| `src/Library.java` | Authentication, borrow/return rules, fines, receipts |
| `src/Main.java` | Console menus and input |
| `README.md` | Run instructions and design/security notes |
| `run.bat`, `run.ps1` | Compile-and-run launchers |
| `.gitignore` | Ignore build output and runtime data |

Small additions to supplied classes:

- `src/Book.java`: added `addCopies(int)` so staff can add copies to an ISBN.
- `src/StaffMember.java` line 19: added package-private `getPassword()` so
  `FileHandler` can save staff credentials.
- `src/Library.java`: delimiter validation, finite-payment validation, receipt
  number includes time, borrow receipt label changed from a second `Borrowed`
  field to `Loans now`.

## 2. First password fix — one input source

**File:** `src/Main.java`

### Original (broken)

```java
private static final Scanner INPUT = new Scanner(System.in);
...
private static String readPassword(String prompt) {
    Console console = System.console();
    if (console != null) {
        char[] password = console.readPassword(prompt);
        return password == null ? null : new String(password);
    }
    return readLine(prompt);
}
```

The ID used `Scanner`, but the password used `Console.readPassword()`. In a real
terminal both readers competed for `System.in`, so the password prompt could
hang or swallow input.

### Changed lines 12–16

Before:

```java
private static final Scanner INPUT = new Scanner(System.in);
private static final Library LIBRARY = new Library();
```

After:

```java
private static final Console CONSOLE = System.console();
private static final Scanner INPUT = (CONSOLE == null) ? new Scanner(System.in) : null;
private static final Library LIBRARY = new Library();
```

### Changed lines 491–501 (`readLine`)

Before:

```java
private static String readLine(String prompt) {
    System.out.print(prompt);
    if (!INPUT.hasNextLine()) {
        return null;
    }
    return INPUT.nextLine().trim();
}
```

After:

```java
private static String readLine(String prompt) {
    if (CONSOLE != null) {
        String line = CONSOLE.readLine(prompt);
        return line == null ? null : line.trim();
    }
    System.out.print(prompt);
    if (!INPUT.hasNextLine()) {
        return null;
    }
    return INPUT.nextLine().trim();
}
```

This fixed the hang, but `readPassword` still hid the typing.

## 3. Second password fix — visible password (current)

**File:** `src/Main.java`, lines 503–511

Before:

```java
private static String readPassword(String prompt) {
    if (CONSOLE != null) {
        char[] password = CONSOLE.readPassword(prompt);
        return password == null ? null : new String(password);
    }
    return readLine(prompt);
}
```

After (current):

```java
/**
 * Reads a password as a normal, visible line. Console.readPassword() is
 * deliberately not used: in several Windows terminals it hides the typing
 * (or leaves the prompt unresponsive), so the user cannot tell whether the
 * keys are being accepted. Visible input keeps the login usable.
 */
private static String readPassword(String prompt) {
    return readLine(prompt);
}
```

The password now uses the exact same input path as the Staff/Member ID, so the
characters are visible while typing. `Console.readPassword()` is no longer
called anywhere.

### Current line map for `Main.java`

| Lines | Item |
|-------|------|
| 15–16 | `CONSOLE` / conditional `Scanner` setup |
| 94, 115 | Staff and member login call `readPassword` |
| 209, 228 | Register member / register staff call `readPassword` |
| 491–501 | `readLine` (Console with Scanner fallback) |
| 503–511 | `readPassword` (now just `readLine`) |
| 535 | Reject `NaN` / `Infinity` payment amounts |

## 4. Documentation update

`README.md`, run section: replaced

> Passwords are read without echo when a real console is attached.

with

> Passwords are typed as a normal visible line so the login never appears
> unresponsive; a production system should mask the input and hash the stored
> password instead.

## How to verify

```bat
cd SchoolLibrarySystem
run.bat
```

Then enter:

```text
Staff ID: S001
Password: admin123
```

You should see `admin123` appear as you type. Member login: `M001` /
`student123`.
