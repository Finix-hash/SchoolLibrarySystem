import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Links one member to one book for one borrowing period.
 * A loan is active until {@code returnDate} is set.
 */
public class Loan {
    private final String memberId;
    private final String isbn;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;

    public Loan(String memberId, String isbn, LocalDate borrowDate, LocalDate dueDate) {
        this(memberId, isbn, borrowDate, dueDate, null);
    }

    public Loan(String memberId, String isbn, LocalDate borrowDate,
                LocalDate dueDate, LocalDate returnDate) {
        this.memberId = memberId;
        this.isbn = isbn;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    public String getMemberId() { return memberId; }
    public String getIsbn() { return isbn; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }

    public boolean isActive() {
        return returnDate == null;
    }

    public void markReturned(LocalDate date) {
        this.returnDate = date;
    }

    /** Whole days late as of {@code onDate}, or as of the actual return date. */
    public long daysOverdue(LocalDate onDate) {
        LocalDate end = (returnDate != null) ? returnDate : onDate;
        if (!end.isAfter(dueDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, end);
    }
}
