/**
 * Base class for everyone who can borrow from the library.
 * Holds the shared identity, credential and penalty state.
 */
public class LibraryMember {
    // Encapsulated private fields
    private String name;
    private String memberId;
    private String password;
    private double penaltyBalance;

    // Parameterized constructor initializing superclass fields
    public LibraryMember(String name, String memberId, String password) {
        this.name = name;         // 'this' distinguishes instance fields from parameters
        this.memberId = memberId;
        this.password = password;
        this.penaltyBalance = 0.0;
    }

    // Accessor (Getter) Methods
    public String getName() { return name; }
    public String getMemberId() { return memberId; }
    public String getPassword() { return password; }
    public double getPenaltyBalance() { return penaltyBalance; }

    // Helper method for credential authentication
    public boolean verifyPassword(String inputPassword) {
        return this.password.equals(inputPassword);
    }

    // Mutator (Setter) Method
    public void setPenaltyBalance(double penaltyBalance) {
        this.penaltyBalance = penaltyBalance;
    }

    // Default borrow limit (2 books)
    public int getMaxBorrowLimit() {
        return 2;
    }
}
