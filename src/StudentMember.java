// Student Subclass
public class StudentMember extends LibraryMember {
    public StudentMember(String name, String memberId, String password) {
        super(name, memberId, password); // Chains constructor execution to superclass
    }

    @Override
    public int getMaxBorrowLimit() {
        return 3; // Overridden limit for student members
    }
}
