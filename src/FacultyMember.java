// Faculty Subclass
public class FacultyMember extends LibraryMember {
    public FacultyMember(String name, String memberId, String password) {
        super(name, memberId, password);
    }

    @Override
    public int getMaxBorrowLimit() {
        return 10; // Overridden limit for faculty members
    }
}
