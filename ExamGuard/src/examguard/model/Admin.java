package examguard.model;

public class Admin extends User {
    public Admin(String id, String username, String hash, String fullName, String email, String designation) {
        super(id, username, hash, fullName, email, designation);
    }
    @Override public Role getRole() { return Role.ADMIN; }
    public String getDesignation() { return getExtra(); }
}
