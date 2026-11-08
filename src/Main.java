import java.io.Console;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Console front end. All rules live in {@link Library}; this class handles
 * menus, input and presentation only.
 */
public class Main {
    // One single input source for the whole app: Console when a real terminal
    // is attached, Scanner only as a fallback (IDEs / piped input). Mixing the
    // two readers over System.in can swallow or hang prompts.
    private static final Console CONSOLE = System.console();
    private static final Scanner INPUT = (CONSOLE == null) ? new Scanner(System.in) : null;
    private static final Library LIBRARY = new Library();

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("           SCHOOL LIBRARY MANAGEMENT SYSTEM");
        System.out.println("============================================================");
        System.out.println("Demo accounts:");
        System.out.println("  Staff  : S001 / admin123");
        System.out.println("  Student: M001 / student123");
        System.out.println("  Faculty: F001 / faculty123");
        System.out.println("============================================================");

        boolean running = true;
        while (running) {
            printMainMenu();
            Integer choice = readInt("Enter choice: ");
            if (choice == null) {
                break;
            }
            switch (choice) {
                case 1 -> staffLogin();
                case 2 -> memberLogin();
                case 0 -> running = false;
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
        System.out.println("Goodbye.");
    }

    // ------------------------------------------------------------------
    // Menus
    // ------------------------------------------------------------------
    private static void printMainMenu() {
        System.out.println();
        System.out.println("--- MAIN MENU ---");
        System.out.println("1. Staff login");
        System.out.println("2. Member login");
        System.out.println("0. Exit");
    }

    private static void printStaffMenu(StaffMember staff) {
        System.out.println();
        System.out.println("--- STAFF MENU (" + staff.getName() + ") ---");
        System.out.println("1. View inventory");
        System.out.println("2. Add book / copies");
        System.out.println("3. Remove book");
        System.out.println("4. Register member");
        System.out.println("5. Register staff account");
        System.out.println("6. View members");
        System.out.println("7. View active loans");
        System.out.println("8. Collect penalty payment");
        System.out.println("0. Logout");
    }

    private static void printMemberMenu(LibraryMember member) {
        System.out.println();
        System.out.println("--- MEMBER MENU (" + member.getName() + " / " + member.getMemberId() + ") ---");
        System.out.println("1. Browse books");
        System.out.println("2. Search books");
        System.out.println("3. Borrow a book");
        System.out.println("4. Return a book");
        System.out.println("5. My active loans");
        System.out.println("6. My loan history");
        System.out.println("7. Pay penalty");
        System.out.println("0. Logout");
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------
    private static void staffLogin() {
        System.out.println("\n--- STAFF LOGIN ---");
        for (int attempt = 1; attempt <= 3; attempt++) {
            String id = readLine("Staff ID: ");
            if (id == null) {
                return;
            }
            String password = readPassword("Password: ");
            if (password == null) {
                return;
            }
            StaffMember staff = LIBRARY.authenticateStaff(id, password);
            if (staff != null) {
                System.out.println("Welcome, " + staff.getName() + "!");
                staffMenu(staff);
                return;
            }
            System.out.println("Invalid credentials. Attempts left: " + (3 - attempt));
        }
    }

    private static void memberLogin() {
        System.out.println("\n--- MEMBER LOGIN ---");
        for (int attempt = 1; attempt <= 3; attempt++) {
            String id = readLine("Member ID: ");
            if (id == null) {
                return;
            }
            String password = readPassword("Password: ");
            if (password == null) {
                return;
            }
            LibraryMember member = LIBRARY.authenticateMember(id, password);
            if (member != null) {
                System.out.println("Welcome, " + member.getName() + "!");
                memberMenu(member);
                return;
            }
            System.out.println("Invalid credentials. Attempts left: " + (3 - attempt));
        }
    }

    // ------------------------------------------------------------------
    // Staff workflow
    // ------------------------------------------------------------------
    private static void staffMenu(StaffMember staff) {
        while (true) {
            printStaffMenu(staff);
            Integer choice = readInt("Enter choice: ");
            if (choice == null) {
                return;
            }
            switch (choice) {
                case 1 -> printInventory();
                case 2 -> addBookFlow();
                case 3 -> removeBookFlow();
                case 4 -> registerMemberFlow();
                case 5 -> registerStaffFlow();
                case 6 -> printMembers();
                case 7 -> printActiveLoans();
                case 8 -> collectPenaltyFlow();
                case 0 -> {
                    System.out.println("Logged out.");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void addBookFlow() {
        System.out.println("\n--- ADD BOOK / COPIES ---");
        String title = readLine("Title (blank to cancel): ");
        if (title == null || title.isEmpty()) {
            return;
        }
        String author = readLine("Author: ");
        if (author == null || author.isEmpty()) {
            System.out.println("Author is required.");
            return;
        }
        String isbn = readLine("ISBN: ");
        if (isbn == null || isbn.isEmpty()) {
            System.out.println("ISBN is required.");
            return;
        }
        Integer copies = readInt("Number of copies: ");
        if (copies == null) {
            return;
        }
        printResult(LIBRARY.addBook(title, author, isbn, copies));
    }

    private static void removeBookFlow() {
        System.out.println("\n--- REMOVE BOOK ---");
        String isbn = readLine("ISBN to remove (blank to cancel): ");
        if (isbn == null || isbn.isEmpty()) {
            return;
        }
        printResult(LIBRARY.removeBook(isbn));
    }

    private static void registerMemberFlow() {
        System.out.println("\n--- REGISTER MEMBER ---");
        Integer role = readInt("Role (1 = Student, 2 = Faculty, 0 = cancel): ");
        if (role == null || role == 0) {
            return;
        }
        if (role != 1 && role != 2) {
            System.out.println("Invalid role.");
            return;
        }
        String name = readLine("Full name: ");
        if (name == null || name.isEmpty()) {
            System.out.println("Name is required.");
            return;
        }
        String id = readLine("Member ID: ");
        if (id == null || id.isEmpty()) {
            System.out.println("Member ID is required.");
            return;
        }
        String password = readPassword("Password: ");
        if (password == null || password.isEmpty()) {
            System.out.println("Password is required.");
            return;
        }
        printResult(LIBRARY.registerMember(role == 2 ? "FACULTY" : "STUDENT", name, id, password));
    }

    private static void registerStaffFlow() {
        System.out.println("\n--- REGISTER STAFF ACCOUNT ---");
        String name = readLine("Full name (blank to cancel): ");
        if (name == null || name.isEmpty()) {
            return;
        }
        String id = readLine("Staff ID: ");
        if (id == null || id.isEmpty()) {
            System.out.println("Staff ID is required.");
            return;
        }
        String password = readPassword("Password: ");
        if (password == null || password.isEmpty()) {
            System.out.println("Password is required.");
            return;
        }
        printResult(LIBRARY.registerStaff(name, id, password));
    }

    private static void collectPenaltyFlow() {
        System.out.println("\n--- COLLECT PENALTY PAYMENT ---");
        String memberId = readLine("Member ID (blank to cancel): ");
        if (memberId == null || memberId.isEmpty()) {
            return;
        }
        LibraryMember member = LIBRARY.findMember(memberId);
        if (member == null) {
            System.out.println("Member not found.");
            return;
        }
        System.out.println("Outstanding balance: " + Library.money(member.getPenaltyBalance()));
        if (member.getPenaltyBalance() <= 0.0001) {
            System.out.println("No payment is due.");
            return;
        }
        Double amount = readDouble("Amount received: RM ");
        if (amount == null) {
            return;
        }
        printResult(LIBRARY.payPenalty(memberId, amount));
    }

    // ------------------------------------------------------------------
    // Member workflow
    // ------------------------------------------------------------------
    private static void memberMenu(LibraryMember member) {
        while (true) {
            printMemberMenu(member);
            Integer choice = readInt("Enter choice: ");
            if (choice == null) {
                return;
            }
            switch (choice) {
                case 1 -> printInventory();
                case 2 -> searchBooksFlow();
                case 3 -> borrowFlow(member);
                case 4 -> returnFlow(member);
                case 5 -> printMyLoans(member);
                case 6 -> printMyHistory(member);
                case 7 -> payPenaltyFlow(member);
                case 0 -> {
                    System.out.println("Logged out.");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void searchBooksFlow() {
        System.out.println("\n--- SEARCH BOOKS ---");
        String query = readLine("Search title / author / ISBN: ");
        if (query == null || query.isEmpty()) {
            return;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        List<Book> books = LIBRARY.getBooks();
        boolean found = false;
        System.out.printf("%-36s %-24s %-15s %s%n", "TITLE", "AUTHOR", "ISBN", "AVAILABLE");
        System.out.println("-".repeat(92));
        for (Book book : books) {
            if (book.getTitle().toLowerCase(Locale.ROOT).contains(needle)
                    || book.getAuthor().toLowerCase(Locale.ROOT).contains(needle)
                    || book.getIsbn().toLowerCase(Locale.ROOT).contains(needle)) {
                printBookRow(book);
                found = true;
            }
        }
        if (!found) {
            System.out.println("No matching books.");
        }
    }

    private static void borrowFlow(LibraryMember member) {
        System.out.println("\n--- BORROW A BOOK ---");
        System.out.println("Active loans: " + LIBRARY.activeLoanCount(member.getMemberId())
                + " / " + member.getMaxBorrowLimit());
        String isbn = readLine("ISBN to borrow (blank to cancel): ");
        if (isbn == null || isbn.isEmpty()) {
            return;
        }
        Library.Result result = LIBRARY.borrowBook(member.getMemberId(), isbn);
        printResult(result);
        if (result.isSuccess()) {
            printReceipt(result);
        }
    }

    private static void returnFlow(LibraryMember member) {
        System.out.println("\n--- RETURN A BOOK ---");
        List<Loan> active = LIBRARY.activeLoansFor(member.getMemberId());
        if (active.isEmpty()) {
            System.out.println("You have no books on loan.");
            return;
        }
        System.out.println("Books currently on loan:");
        for (Loan loan : active) {
            System.out.println("  " + loan.getIsbn() + "  " + LIBRARY.getBookTitle(loan.getIsbn()));
        }
        String isbn = readLine("ISBN to return (blank to cancel): ");
        if (isbn == null || isbn.isEmpty()) {
            return;
        }
        Library.Result result = LIBRARY.returnBook(member.getMemberId(), isbn);
        printResult(result);
        if (result.isSuccess()) {
            printReceipt(result);
        }
    }

    private static void payPenaltyFlow(LibraryMember member) {
        System.out.println("\n--- PAY PENALTY ---");
        double balance = member.getPenaltyBalance();
        System.out.println("Outstanding balance: " + Library.money(balance));
        if (balance <= 0.0001) {
            System.out.println("You have no outstanding penalty.");
            return;
        }
        Double amount = readDouble("Amount to pay: RM ");
        if (amount == null) {
            return;
        }
        printResult(LIBRARY.payPenalty(member.getMemberId(), amount));
    }

    // ------------------------------------------------------------------
    // Tables
    // ------------------------------------------------------------------
    private static void printInventory() {
        System.out.println("\n--- INVENTORY ---");
        List<Book> books = LIBRARY.getBooks();
        if (books.isEmpty()) {
            System.out.println("The inventory is empty.");
            return;
        }
        System.out.printf("%-36s %-24s %-15s %s%n", "TITLE", "AUTHOR", "ISBN", "AVAILABLE");
        System.out.println("-".repeat(92));
        for (Book book : books) {
            printBookRow(book);
        }
    }

    private static void printBookRow(Book book) {
        System.out.printf("%-36s %-24s %-15s %d%n",
                truncate(book.getTitle(), 36),
                truncate(book.getAuthor(), 24),
                book.getIsbn(),
                book.getAvailableCopies());
    }

    private static void printMembers() {
        System.out.println("\n--- MEMBERS ---");
        List<LibraryMember> members = LIBRARY.getMembers();
        if (members.isEmpty()) {
            System.out.println("No members registered.");
            return;
        }
        System.out.printf("%-8s %-8s %-24s %-9s %s%n", "ID", "TYPE", "NAME", "LOANS", "PENALTY");
        System.out.println("-".repeat(78));
        for (LibraryMember member : members) {
            String type = member instanceof StudentMember ? "Student"
                    : member instanceof FacultyMember ? "Faculty" : "Member";
            int active = LIBRARY.activeLoanCount(member.getMemberId());
            System.out.printf("%-8s %-8s %-24s %-9s %s%n",
                    member.getMemberId(),
                    type,
                    truncate(member.getName(), 24),
                    active + "/" + member.getMaxBorrowLimit(),
                    Library.money(member.getPenaltyBalance()));
        }
    }

    private static void printActiveLoans() {
        System.out.println("\n--- ACTIVE LOANS ---");
        List<Loan> loans = LIBRARY.getActiveLoans();
        if (loans.isEmpty()) {
            System.out.println("No books are currently on loan.");
            return;
        }
        LocalDate today = LocalDate.now();
        System.out.printf("%-8s %-30s %-34s %-12s %s%n", "MEMBER", "BOOK", "TITLE", "DUE", "STATUS");
        System.out.println("-".repeat(104));
        for (Loan loan : loans) {
            LibraryMember member = LIBRARY.findMember(loan.getMemberId());
            String name = member == null ? loan.getMemberId() : member.getName();
            long late = loan.daysOverdue(today);
            String status = late > 0 ? "OVERDUE " + late + "d" : "On time";
            System.out.printf("%-8s %-30s %-34s %-12s %s%n",
                    loan.getMemberId(),
                    truncate(name, 30),
                    truncate(LIBRARY.getBookTitle(loan.getIsbn()), 34),
                    loan.getDueDate(),
                    status);
        }
    }

    private static void printMyLoans(LibraryMember member) {
        System.out.println("\n--- MY ACTIVE LOANS ---");
        List<Loan> active = LIBRARY.activeLoansFor(member.getMemberId());
        if (active.isEmpty()) {
            System.out.println("You have no active loans.");
            return;
        }
        LocalDate today = LocalDate.now();
        System.out.printf("%-36s %-15s %-12s %-10s %s%n", "TITLE", "ISBN", "DUE", "LATE", "FINE");
        System.out.println("-".repeat(88));
        for (Loan loan : active) {
            long late = loan.daysOverdue(today);
            System.out.printf("%-36s %-15s %-12s %-10s %s%n",
                    truncate(LIBRARY.getBookTitle(loan.getIsbn()), 36),
                    loan.getIsbn(),
                    loan.getDueDate(),
                    late + "d",
                    Library.money(LIBRARY.accruedFine(loan, today)));
        }
        System.out.println("Borrow limit: " + active.size() + "/" + member.getMaxBorrowLimit());
    }

    private static void printMyHistory(LibraryMember member) {
        System.out.println("\n--- MY LOAN HISTORY ---");
        List<Loan> history = LIBRARY.loanHistoryFor(member.getMemberId());
        if (history.isEmpty()) {
            System.out.println("No loan history yet.");
            return;
        }
        System.out.printf("%-36s %-15s %-12s %-12s %s%n", "TITLE", "ISBN", "BORROWED", "DUE", "RETURNED");
        System.out.println("-".repeat(92));
        for (Loan loan : history) {
            String returned = loan.isActive() ? "STILL OUT" : loan.getReturnDate().toString();
            System.out.printf("%-36s %-15s %-12s %-12s %s%n",
                    truncate(LIBRARY.getBookTitle(loan.getIsbn()), 36),
                    loan.getIsbn(),
                    loan.getBorrowDate(),
                    loan.getDueDate(),
                    returned);
        }
    }

    // ------------------------------------------------------------------
    // Input and output helpers
    // ------------------------------------------------------------------
    private static void printResult(Library.Result result) {
        System.out.println(result.getMessage());
    }

    private static void printReceipt(Library.Result result) {
        if (!result.getReceipt().isEmpty()) {
            System.out.println();
            System.out.println(result.getReceipt());
            System.out.println();
            System.out.println("(A copy has been saved to receipts.txt)");
        }
    }

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

    /**
     * Reads a password as a normal, visible line. Console.readPassword() is
     * deliberately not used: in several Windows terminals it hides the typing
     * (or leaves the prompt unresponsive), so the user cannot tell whether the
     * keys are being accepted. Visible input keeps the login usable.
     */
    private static String readPassword(String prompt) {
        return readLine(prompt);
    }

    private static Integer readInt(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (line == null) {
                return null;
            }
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static Double readDouble(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (line == null) {
                return null;
            }
            try {
                double value = Double.parseDouble(line);
                if (!Double.isFinite(value)) {
                    System.out.println("Please enter a valid amount.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid amount.");
            }
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
