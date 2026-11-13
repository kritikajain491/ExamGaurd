package examguard.model;

import examguard.util.FileUtil;
import java.time.LocalDateTime;

/** Base class for all accounts. Admin and Student specialise it. */
public abstract class User {
    private final String id;
    private String username, passwordHash, fullName, email, createdAt, extra;
    private boolean active = true;
    private int failedAttempts;
    private long lockedUntil;

    protected User(String id, String username, String passwordHash, String fullName, String email, String extra) {
        this.id = id; this.username = username; this.passwordHash = passwordHash;
        this.fullName = fullName; this.email = email; this.extra = extra;
        this.createdAt = LocalDateTime.now().format(FileUtil.DT);
    }

    public abstract Role getRole();

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getCreatedAt() { return createdAt; }
    protected String getExtra() { return extra; }
    public boolean isActive() { return active; }
    public int getFailedAttempts() { return failedAttempts; }
    public long getLockedUntil() { return lockedUntil; }

    public void setPasswordHash(String v) { passwordHash = v; }
    public void setFullName(String v) { fullName = v; }
    public void setEmail(String v) { email = v; }
    public void setExtra(String v) { extra = v; }
    public void setActive(boolean v) { active = v; }
    public void setFailedAttempts(int v) { failedAttempts = v; }
    public void setLockedUntil(long v) { lockedUntil = v; }

    public boolean isLocked() { return lockedUntil > System.currentTimeMillis(); }

    public String status() {
        if (!active) return "Disabled";
        return isLocked() ? "Locked" : "Active";
    }

    public String[] toRecord() {
        return new String[]{id, getRole().name(), username, passwordHash, fullName, email, String.valueOf(active),
                String.valueOf(failedAttempts), String.valueOf(lockedUntil), createdAt, extra};
    }

    public static User fromRecord(String[] f) {
        User u = f[1].equals("ADMIN") ? new Admin(f[0], f[2], f[3], f[4], f[5], f[10])
                                      : new Student(f[0], f[2], f[3], f[4], f[5], f[10]);
        u.active = Boolean.parseBoolean(f[6]);
        u.failedAttempts = Integer.parseInt(f[7]);
        u.lockedUntil = Long.parseLong(f[8]);
        u.createdAt = f[9];
        return u;
    }
}
