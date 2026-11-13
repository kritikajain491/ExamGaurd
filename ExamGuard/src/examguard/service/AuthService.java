package examguard.service;

import examguard.model.*;
import examguard.util.Validator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Authentication, registration, lockout policy and role checks. */
public class AuthService {
    public static final int MAX_ATTEMPTS = 3;
    public static final int LOCK_MINUTES = 5;
    private static User current;

    public static class AuthException extends Exception {
        public AuthException(String m) { super(m); }
    }

    private AuthService() {}

    public static User current() { return current; }
    public static void logout() {
        if (current != null) DataStore.get().log(current.getUsername(), "Logged out");
        current = null;
    }

    /** Throws SecurityException unless the logged-in user has the required role. */
    public static void requireRole(Role r) {
        if (current == null || current.getRole() != r) throw new SecurityException("Access denied: " + r + " role required.");
    }

    public static String hash(String username, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(("ExamGuard::" + username.toLowerCase() + "::" + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public static User login(String username, String password, Role role) throws AuthException {
        DataStore ds = DataStore.get();
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty())
            throw new AuthException("Enter username and password.");
        User u = ds.findUser(username);
        if (u == null) {
            ds.log(username.trim(), "Login failed (unknown user)");
            throw new AuthException("Invalid username or password.");
        }
        if (u.getRole() != role) {
            ds.log(u.getUsername(), "Login blocked: wrong role selected");
            throw new AuthException("This is not a " + role.name().toLowerCase() + " account.");
        }
        if (!u.isActive()) throw new AuthException("Account is disabled. Contact the administrator.");
        if (u.isLocked()) {
            long mins = (u.getLockedUntil() - System.currentTimeMillis()) / 60000 + 1;
            throw new AuthException("Account locked. Try again in " + mins + " minute(s).");
        }
        if (!hash(u.getUsername(), password).equals(u.getPasswordHash())) {
            int n = u.getFailedAttempts() + 1;
            if (n >= MAX_ATTEMPTS) {
                u.setFailedAttempts(0);
                u.setLockedUntil(System.currentTimeMillis() + LOCK_MINUTES * 60000L);
                ds.saveUsers();
                ds.log(u.getUsername(), "Account locked after " + MAX_ATTEMPTS + " failed attempts");
                throw new AuthException("Too many failed attempts. Locked for " + LOCK_MINUTES + " minutes.");
            }
            u.setFailedAttempts(n);
            ds.saveUsers();
            ds.log(u.getUsername(), "Login failed (wrong password)");
            throw new AuthException("Invalid username or password. " + (MAX_ATTEMPTS - n) + " attempt(s) left.");
        }
        u.setFailedAttempts(0);
        u.setLockedUntil(0);
        ds.saveUsers();
        current = u;
        ds.log(u.getUsername(), "Logged in as " + role);
        return u;
    }

    public static Student register(String name, String roll, String email, String username, String pass, String confirm)
            throws AuthException {
        DataStore ds = DataStore.get();
        String err = Validator.name(name);
        if (err == null) err = Validator.required(roll, "Roll number");
        if (err == null) err = Validator.email(email);
        if (err == null) err = Validator.username(username);
        if (err == null && ds.findUser(username) != null) err = "Username already taken.";
        if (err == null) err = Validator.password(pass);
        if (err == null && !pass.equals(confirm)) err = "Passwords do not match.";
        if (err != null) throw new AuthException(err);
        Student s = new Student(DataStore.newId("S"), username.trim(), hash(username.trim(), pass), name.trim(),
                email.trim(), roll.trim());
        ds.users.add(s);
        ds.saveUsers();
        ds.log(s.getUsername(), "Registered new student account");
        return s;
    }

    public static void changePassword(User u, String oldPass, String newPass, String confirm) throws AuthException {
        if (!hash(u.getUsername(), oldPass).equals(u.getPasswordHash())) throw new AuthException("Current password is incorrect.");
        String err = Validator.password(newPass);
        if (err == null && !newPass.equals(confirm)) err = "Passwords do not match.";
        if (err != null) throw new AuthException(err);
        u.setPasswordHash(hash(u.getUsername(), newPass));
        DataStore.get().saveUsers();
        DataStore.get().log(u.getUsername(), "Changed password");
    }
}
