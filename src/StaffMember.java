/**
 * Administrative account. Staff can manage inventory and members.
 */
public class StaffMember {
    private String name;
    private String staffId;
    private String password;

    public StaffMember(String name, String staffId, String password) {
        this.name = name;
        this.staffId = staffId;
        this.password = password;
    }

    public String getName() { return name; }
    public String getStaffId() { return staffId; }

    /** Package-private so FileHandler can persist the credential. */
    String getPassword() { return password; }

    public boolean verifyPassword(String inputPassword) {
        return this.password.equals(inputPassword);
    }
}
