import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Business logic for the library: authentication, inventory, borrowing,
 * returns, fines and persistence. The console UI ({@link Main}) only talks to
 * this class, so rules are enforced even if a different UI is added later.
 */
public class Library {
    public static final double FINE_PER_DAY = 5.00;
    public static final int LOAN_PERIOD_DAYS = 14;

    private final List<LibraryMember> members;
    private final List<StaffMember> staff;
    private final List<Book> books;
    private final List<Loan> loans;

    private static int receiptSequence = 0;

    public Library() {
        members = new ArrayList<>(FileHandler.loadMembers());
        staff = new ArrayList<>(FileHandler.loadStaff());
        books = new ArrayList<>(FileHandler.loadBooks());
        loans = new ArrayList<>(FileHandler.loadLoans());
        seedDemoDataIfEmpty();
    }

    // ------------------------------------------------------------------
    // Authentication
    // ------------------------------------------------------------------
    public LibraryMember authenticateMember(String memberId, String password) {
        LibraryMember member = findMember(memberId);
        if (member != null && member.verifyPassword(password)) {
            return member;
        }
        return null;
    }

    public StaffMember authenticateStaff(String staffId, String password) {
        StaffMember member = findStaff(staffId);
        if (member != null && member.verifyPassword(password)) {
            return member;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------
    public LibraryMember findMember(String memberId) {
        if (memberId == null) {
            return null;
        }
        for (LibraryMember member : members) {
            if (member.getMemberId().equalsIgnoreCase(memberId.trim())) {
                return member;
            }
        }
        return null;
    }

    public StaffMember findStaff(String staffId) {
        if (staffId == null) {
            return null;
        }
        for (StaffMember member : staff) {
            if (member.getStaffId().equalsIgnoreCase(staffId.trim())) {
                return member;
            }
        }
        return null;
    }

    public Book findBook(String isbn) {
        if (isbn == null) {
            return null;
        }
        for (Book book : books) {
            if (book.getIsbn().equalsIgnoreCase(isbn.trim())) {
                return book;
            }
        }
        return null;
    }

    public Loan findActiveLoan(String memberId, String isbn) {
        for (Loan loan : loans) {
            if (loan.isActive()
                    && loan.getMemberId().equalsIgnoreCase(memberId)
                    && loan.getIsbn().equalsIgnoreCase(isbn)) {
                return loan;
            }
        }
        return null;
    }

    public String getBookTitle(String isbn) {
        Book book = findBook(isbn);
        return book == null ? "(unknown title)" : book.getTitle();
    }

    // ------------------------------------------------------------------
    // Inventory management (staff)
    // ------------------------------------------------------------------
    public Result addBook(String title, String author, String isbn, int copies) {
        if (isBlank(title) || isBlank(author) || isBlank(isbn)) {
            return new Result(false, "Title, author and ISBN are required.");
        }
        if (hasDelimiter(title) || hasDelimiter(author) || hasDelimiter(isbn)) {
            return new Result(false, "Fields may not contain the '|' character.");
        }
        if (copies <= 0) {
            return new Result(false, "Copies must be at least 1.");
        }

        Book existing = findBook(isbn);
        if (existing != null) {
            existing.addCopies(copies);
            FileHandler.saveBooks(books);
            return new Result(true, "Added " + copies + " more copy/copies of \""
                    + existing.getTitle() + "\". Available now: " + existing.getAvailableCopies() + ".");
        }

        Book book = new Book(title.trim(), author.trim(), isbn.trim(), copies);
        books.add(book);
        FileHandler.saveBooks(books);
        return new Result(true, "Added \"" + book.getTitle() + "\" (" + copies + " copy/copies).");
    }

    public Result removeBook(String isbn) {
        Book book = findBook(isbn);
        if (book == null) {
            return new Result(false, "No book found with ISBN " + isbn + ".");
        }
        for (Loan loan : loans) {
            if (loan.isActive() && loan.getIsbn().equalsIgnoreCase(book.getIsbn())) {
                return new Result(false, "Cannot remove \"" + book.getTitle()
                        + "\" while copies are still on loan.");
            }
        }
        books.remove(book);
        FileHandler.saveBooks(books);
        return new Result(true, "Removed \"" + book.getTitle() + "\" from the inventory.");
    }

    public Result registerMember(String role, String name, String memberId, String password) {
        if (isBlank(name) || isBlank(memberId) || isBlank(password)) {
            return new Result(false, "Name, ID and password are required.");
        }
        if (hasDelimiter(name) || hasDelimiter(memberId) || hasDelimiter(password)) {
            return new Result(false, "Fields may not contain the '|' character.");
        }
        if (findMember(memberId) != null || findStaff(memberId) != null) {
            return new Result(false, "ID " + memberId + " is already in use.");
        }

        LibraryMember member;
        if ("FACULTY".equalsIgnoreCase(role)) {
            member = new FacultyMember(name.trim(), memberId.trim(), password);
        } else {
            member = new StudentMember(name.trim(), memberId.trim(), password);
        }
        members.add(member);
        FileHandler.saveMembers(members);
        return new Result(true, "Registered " + member.getName() + " (" + member.getMemberId()
                + ", borrow limit " + member.getMaxBorrowLimit() + ").");
    }

    public Result registerStaff(String name, String staffId, String password) {
        if (isBlank(name) || isBlank(staffId) || isBlank(password)) {
            return new Result(false, "Name, staff ID and password are required.");
        }
        if (hasDelimiter(name) || hasDelimiter(staffId) || hasDelimiter(password)) {
            return new Result(false, "Fields may not contain the '|' character.");
        }
        if (findStaff(staffId) != null || findMember(staffId) != null) {
            return new Result(false, "ID " + staffId + " is already in use.");
        }
        StaffMember member = new StaffMember(name.trim(), staffId.trim(), password);
        staff.add(member);
        FileHandler.saveStaff(staff);
        return new Result(true, "Registered staff account for " + member.getName() + ".");
    }

    // ------------------------------------------------------------------
    // Borrow / return
    // ------------------------------------------------------------------
    public Result borrowBook(String memberId, String isbn) {
        return borrowBook(memberId, isbn, LocalDate.now());
    }

    /** Date-parameterised overload, useful for tests and back-dated entries. */
    public Result borrowBook(String memberId, String isbn, LocalDate today) {
        LibraryMember member = findMember(memberId);
        if (member == null) {
            return new Result(false, "Member not found.");
        }
        Book book = findBook(isbn);
        if (book == null) {
            return new Result(false, "No book found with ISBN " + isbn + ".");
        }
        if (member.getPenaltyBalance() > 0.0001) {
            return new Result(false, "Borrowing blocked: outstanding penalty of "
                    + money(member.getPenaltyBalance()) + ". Please settle it first.");
        }

        int activeCount = activeLoanCount(memberId);
        if (findActiveLoan(memberId, book.getIsbn()) != null) {
            return new Result(false, "This member already has an active loan for \""
                    + book.getTitle() + "\".");
        }
        if (activeCount >= member.getMaxBorrowLimit()) {
            return new Result(false, "Borrowing limit reached (" + activeCount + "/"
                    + member.getMaxBorrowLimit() + "). Return a book first.");
        }
        if (!book.borrowCopy()) {
            return new Result(false, "No copies of \"" + book.getTitle() + "\" are available right now.");
        }

        Loan loan = new Loan(member.getMemberId(), book.getIsbn(), today,
                today.plusDays(LOAN_PERIOD_DAYS));
        loans.add(loan);
        persistAll();

        String receipt = buildBorrowReceipt(member, book, loan, activeCount + 1);
        FileHandler.appendReceipt(receipt);
        return new Result(true, "Borrowed \"" + book.getTitle() + "\". Due "
                + loan.getDueDate() + ".", receipt);
    }

    public Result returnBook(String memberId, String isbn) {
        return returnBook(memberId, isbn, LocalDate.now());
    }

    /** Date-parameterised overload so overdue fines can be tested deterministically. */
    public Result returnBook(String memberId, String isbn, LocalDate today) {
        LibraryMember member = findMember(memberId);
        if (member == null) {
            return new Result(false, "Member not found.");
        }
        Book book = findBook(isbn);
        String resolvedIsbn = book == null ? isbn : book.getIsbn();
        Loan loan = findActiveLoan(memberId, resolvedIsbn);
        if (loan == null) {
            return new Result(false, "No active loan found for this member and ISBN.");
        }

        long daysLate = loan.daysOverdue(today);
        double fine = daysLate * FINE_PER_DAY;

        loan.markReturned(today);
        if (book != null) {
            book.returnCopy();
        }
        if (fine > 0) {
            member.setPenaltyBalance(member.getPenaltyBalance() + fine);
        }
        persistAll();

        String receipt = buildReturnReceipt(member, book, loan, daysLate, fine, today);
        FileHandler.appendReceipt(receipt);
        String message = "Returned \"" + (book == null ? isbn : book.getTitle()) + "\".";
        if (fine > 0) {
            message += " Overdue fine: " + money(fine) + ".";
        }
        return new Result(true, message, receipt);
    }

    public Result payPenalty(String memberId, double amount) {
        LibraryMember member = findMember(memberId);
        if (member == null) {
            return new Result(false, "Member not found.");
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            return new Result(false, "Payment must be a finite amount greater than zero.");
        }
        double balance = member.getPenaltyBalance();
        if (balance <= 0.0001) {
            return new Result(false, member.getName() + " has no outstanding penalty.");
        }

        double applied = Math.min(amount, balance);
        member.setPenaltyBalance(round2(balance - applied));
        FileHandler.saveMembers(members);

        String message = "Collected " + money(applied) + " from " + member.getName()
                + ". Outstanding: " + money(member.getPenaltyBalance()) + ".";
        if (amount > applied) {
            message += " Change: " + money(amount - applied) + ".";
        }
        return new Result(true, message);
    }

    // ------------------------------------------------------------------
    // Reporting
    // ------------------------------------------------------------------
    public int activeLoanCount(String memberId) {
        int count = 0;
        for (Loan loan : loans) {
            if (loan.isActive() && loan.getMemberId().equalsIgnoreCase(memberId)) {
                count++;
            }
        }
        return count;
    }

    public List<Loan> activeLoansFor(String memberId) {
        List<Loan> result = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan.isActive() && loan.getMemberId().equalsIgnoreCase(memberId)) {
                result.add(loan);
            }
        }
        return result;
    }

    public List<Loan> loanHistoryFor(String memberId) {
        List<Loan> result = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan.getMemberId().equalsIgnoreCase(memberId)) {
                result.add(loan);
            }
        }
        return result;
    }

    public List<Loan> getActiveLoans() {
        List<Loan> result = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan.isActive()) {
                result.add(loan);
            }
        }
        return result;
    }

    public List<Loan> getLoans() {
        return Collections.unmodifiableList(loans);
    }

    public List<LibraryMember> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public List<StaffMember> getStaff() {
        return Collections.unmodifiableList(staff);
    }

    public List<Book> getBooks() {
        return Collections.unmodifiableList(books);
    }

    public double accruedFine(Loan loan, LocalDate onDate) {
        return loan.daysOverdue(onDate) * FINE_PER_DAY;
    }

    // ------------------------------------------------------------------
    // Receipts
    // ------------------------------------------------------------------
    private String buildBorrowReceipt(LibraryMember member, Book book, Loan loan, int activeCount) {
        String line = "============================================================";
        return line + System.lineSeparator()
                + "              SCHOOL LIBRARY - BORROW RECEIPT" + System.lineSeparator()
                + line + System.lineSeparator()
                + "Receipt No  : " + nextReceiptNo("BR") + System.lineSeparator()
                + "Member      : " + member.getName() + " (" + member.getMemberId() + ")" + System.lineSeparator()
                + "Book        : " + book.getTitle() + System.lineSeparator()
                + "Author      : " + book.getAuthor() + System.lineSeparator()
                + "ISBN        : " + book.getIsbn() + System.lineSeparator()
                + "Borrowed    : " + loan.getBorrowDate() + System.lineSeparator()
                + "Due Date    : " + loan.getDueDate() + System.lineSeparator()
                + "Loans now   : " + activeCount + " of " + member.getMaxBorrowLimit() + " allowed" + System.lineSeparator()
                + line + System.lineSeparator()
                + "Please return by the due date. Overdue fine: "
                + money(FINE_PER_DAY) + " per day." + System.lineSeparator()
                + line;
    }

    private String buildReturnReceipt(LibraryMember member, Book book, Loan loan,
                                      long daysLate, double fine, LocalDate returnedOn) {
        String line = "============================================================";
        String title = book == null ? loan.getIsbn() : book.getTitle();
        return line + System.lineSeparator()
                + "              SCHOOL LIBRARY - RETURN RECEIPT" + System.lineSeparator()
                + line + System.lineSeparator()
                + "Receipt No  : " + nextReceiptNo("RT") + System.lineSeparator()
                + "Member      : " + member.getName() + " (" + member.getMemberId() + ")" + System.lineSeparator()
                + "Book        : " + title + System.lineSeparator()
                + "ISBN        : " + loan.getIsbn() + System.lineSeparator()
                + "Borrowed    : " + loan.getBorrowDate() + System.lineSeparator()
                + "Due Date    : " + loan.getDueDate() + System.lineSeparator()
                + "Returned    : " + returnedOn + System.lineSeparator()
                + "Days Late   : " + daysLate + System.lineSeparator()
                + "Fine        : " + money(fine) + System.lineSeparator()
                + "Balance     : " + money(member.getPenaltyBalance()) + System.lineSeparator()
                + line;
    }

    private static synchronized String nextReceiptNo(String prefix) {
        receiptSequence++;
        return prefix + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
                + "-" + String.format(Locale.ROOT, "%04d", receiptSequence);
    }

    // ------------------------------------------------------------------
    // Persistence and seeding
    // ------------------------------------------------------------------
    private void persistAll() {
        FileHandler.saveMembers(members);
        FileHandler.saveStaff(staff);
        FileHandler.saveBooks(books);
        FileHandler.saveLoans(loans);
    }

    private void seedDemoDataIfEmpty() {
        boolean changed = false;

        if (members.isEmpty()) {
            members.add(new StudentMember("Alice Tan", "M001", "student123"));
            members.add(new StudentMember("Ben Lim", "M002", "student123"));
            members.add(new FacultyMember("Dr. Chen Wei", "F001", "faculty123"));
            changed = true;
        }
        if (staff.isEmpty()) {
            staff.add(new StaffMember("Ms. Wong", "S001", "admin123"));
            changed = true;
        }
        if (books.isEmpty()) {
            books.add(new Book("Clean Code", "Robert C. Martin", "9780132350884", 3));
            books.add(new Book("Effective Java", "Joshua Bloch", "9780134685991", 2));
            books.add(new Book("Introduction to Algorithms", "Cormen et al.", "9780262046305", 2));
            books.add(new Book("The Pragmatic Programmer", "Hunt & Thomas", "9780135957059", 1));
            changed = true;
        }

        if (changed) {
            persistAll();
        }
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------
    public static String money(double amount) {
        return String.format(Locale.ROOT, "RM %.2f", amount);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean hasDelimiter(String value) {
        return value != null && value.contains("|");
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Outcome of an operation, optionally carrying a printable receipt. */
    public static final class Result {
        private final boolean success;
        private final String message;
        private final String receipt;

        public Result(boolean success, String message) {
            this(success, message, "");
        }

        public Result(boolean success, String message, String receipt) {
            this.success = success;
            this.message = message;
            this.receipt = receipt == null ? "" : receipt;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getReceipt() { return receipt; }
    }
}
