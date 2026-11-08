import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Text-file persistence layer. Each row is pipe-delimited so the files stay
 * human-readable and easy to inspect:
 *
 * members.txt : TYPE|name|memberId|password|penaltyBalance
 * staff.txt   : name|staffId|password
 * books.txt   : title|author|isbn|availableCopies
 * loans.txt   : memberId|isbn|borrowDate|dueDate|returnDate("-" = still out)
 */
public class FileHandler {
    private static final String MEMBERS_FILE = "members.txt";
    private static final String STAFF_FILE = "staff.txt";
    private static final String BOOKS_FILE = "books.txt";
    private static final String LOANS_FILE = "loans.txt";
    private static final String RECEIPTS_FILE = "receipts.txt";
    private static final String DELIMITER = "\\|";

    // --- MEMBERS PERSISTENCE ---
    public static void saveMembers(List<LibraryMember> members) {
        try (PrintWriter out = new PrintWriter(MEMBERS_FILE, StandardCharsets.UTF_8)) {
            for (LibraryMember member : members) {
                String type;
                if (member instanceof StudentMember) {
                    type = "STUDENT";
                } else if (member instanceof FacultyMember) {
                    type = "FACULTY";
                } else {
                    type = "MEMBER";
                }
                out.println(String.join("|",
                        type,
                        safe(member.getName()),
                        safe(member.getMemberId()),
                        safe(member.getPassword()),
                        String.valueOf(member.getPenaltyBalance())));
            }
        } catch (IOException e) {
            System.out.println("[File Error] Could not save members: " + e.getMessage());
        }
    }

    public static List<LibraryMember> loadMembers() {
        List<LibraryMember> members = new ArrayList<>();
        File file = new File(MEMBERS_FILE);
        if (!file.exists()) {
            return members;
        }
        try (Scanner in = new Scanner(file, StandardCharsets.UTF_8)) {
            while (in.hasNextLine()) {
                String line = in.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 5) {
                    System.out.println("[File Warning] Skipping malformed member row: " + line);
                    continue;
                }
                LibraryMember member;
                switch (parts[0].toUpperCase()) {
                    case "STUDENT" -> member = new StudentMember(parts[1], parts[2], parts[3]);
                    case "FACULTY" -> member = new FacultyMember(parts[1], parts[2], parts[3]);
                    default -> member = new LibraryMember(parts[1], parts[2], parts[3]);
                }
                member.setPenaltyBalance(parseDouble(parts[4]));
                members.add(member);
            }
        } catch (IOException e) {
            System.out.println("[File Error] Could not load members: " + e.getMessage());
        }
        return members;
    }

    // --- STAFF PERSISTENCE ---
    public static void saveStaff(List<StaffMember> staff) {
        try (PrintWriter out = new PrintWriter(STAFF_FILE, StandardCharsets.UTF_8)) {
            for (StaffMember member : staff) {
                out.println(String.join("|",
                        safe(member.getName()),
                        safe(member.getStaffId()),
                        safe(member.getPassword())));
            }
        } catch (IOException e) {
            System.out.println("[File Error] Could not save staff: " + e.getMessage());
        }
    }

    public static List<StaffMember> loadStaff() {
        List<StaffMember> staff = new ArrayList<>();
        File file = new File(STAFF_FILE);
        if (!file.exists()) {
            return staff;
        }
        try (Scanner in = new Scanner(file, StandardCharsets.UTF_8)) {
            while (in.hasNextLine()) {
                String line = in.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 3) {
                    System.out.println("[File Warning] Skipping malformed staff row: " + line);
                    continue;
                }
                staff.add(new StaffMember(parts[0], parts[1], parts[2]));
            }
        } catch (IOException e) {
            System.out.println("[File Error] Could not load staff: " + e.getMessage());
        }
        return staff;
    }

    // --- BOOK PERSISTENCE ---
    public static void saveBooks(List<Book> books) {
        try (PrintWriter out = new PrintWriter(BOOKS_FILE, StandardCharsets.UTF_8)) {
            for (Book book : books) {
                out.println(String.join("|",
                        safe(book.getTitle()),
                        safe(book.getAuthor()),
                        safe(book.getIsbn()),
                        String.valueOf(book.getAvailableCopies())));
            }
        } catch (IOException e) {
            System.out.println("[File Error] Could not save books: " + e.getMessage());
        }
    }

    public static List<Book> loadBooks() {
        List<Book> books = new ArrayList<>();
        File file = new File(BOOKS_FILE);
        if (!file.exists()) {
            return books;
        }
        try (Scanner in = new Scanner(file, StandardCharsets.UTF_8)) {
            while (in.hasNextLine()) {
                String line = in.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 4) {
                    System.out.println("[File Warning] Skipping malformed book row: " + line);
                    continue;
                }
                books.add(new Book(parts[0], parts[1], parts[2], parseInt(parts[3])));
            }
        } catch (IOException e) {
            System.out.println("[File Error] Could not load books: " + e.getMessage());
        }
        return books;
    }

    // --- LOAN PERSISTENCE ---
    public static void saveLoans(List<Loan> loans) {
        try (PrintWriter out = new PrintWriter(LOANS_FILE, StandardCharsets.UTF_8)) {
            for (Loan loan : loans) {
                out.println(String.join("|",
                        safe(loan.getMemberId()),
                        safe(loan.getIsbn()),
                        loan.getBorrowDate().toString(),
                        loan.getDueDate().toString(),
                        loan.getReturnDate() == null ? "-" : loan.getReturnDate().toString()));
            }
        } catch (IOException e) {
            System.out.println("[File Error] Could not save loans: " + e.getMessage());
        }
    }

    public static List<Loan> loadLoans() {
        List<Loan> loans = new ArrayList<>();
        File file = new File(LOANS_FILE);
        if (!file.exists()) {
            return loans;
        }
        try (Scanner in = new Scanner(file, StandardCharsets.UTF_8)) {
            while (in.hasNextLine()) {
                String line = in.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 5) {
                    System.out.println("[File Warning] Skipping malformed loan row: " + line);
                    continue;
                }
                try {
                    LocalDate borrowDate = LocalDate.parse(parts[2]);
                    LocalDate dueDate = LocalDate.parse(parts[3]);
                    LocalDate returnDate = "-".equals(parts[4]) || parts[4].isEmpty()
                            ? null
                            : LocalDate.parse(parts[4]);
                    loans.add(new Loan(parts[0], parts[1], borrowDate, dueDate, returnDate));
                } catch (DateTimeParseException e) {
                    System.out.println("[File Warning] Skipping loan with bad date: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("[File Error] Could not load loans: " + e.getMessage());
        }
        return loans;
    }

    // --- RECEIPTS ---
    /** Appends a printable receipt to receipts.txt (the console copy is separate). */
    public static void appendReceipt(String receipt) {
        try (PrintWriter out = new PrintWriter(
                new FileWriter(RECEIPTS_FILE, StandardCharsets.UTF_8, true))) {
            out.println(receipt);
            out.println();
        } catch (IOException e) {
            System.out.println("[File Error] Could not write receipt: " + e.getMessage());
        }
    }

    // --- HELPERS ---
    /** Keeps the pipe delimiter and line breaks out of persisted values. */
    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", "/").replace("\r", " ").replace("\n", " ").trim();
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
