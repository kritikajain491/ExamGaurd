package examguard.model;

public class Student extends User {
    public Student(String id, String username, String hash, String fullName, String email, String rollNo) {
        super(id, username, hash, fullName, email, rollNo);
    }
    @Override public Role getRole() { return Role.STUDENT; }
    public String getRollNo() { return getExtra(); }
    public void setRollNo(String r) { setExtra(r); }
}
