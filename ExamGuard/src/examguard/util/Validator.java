package examguard.util;

/** Input validation helpers. Each method returns null when valid, otherwise an error message. */
public final class Validator {
    private Validator() {}

    public static String name(String s) {
        if (s == null || s.trim().length() < 3) return "Full name must be at least 3 characters.";
        if (!s.trim().matches("[\\p{L} .'-]+")) return "Full name contains invalid characters.";
        return null;
    }

    public static String username(String s) {
        if (s == null || !s.trim().matches("[A-Za-z0-9_]{4,20}"))
            return "Username must be 4-20 letters, digits or underscores.";
        return null;
    }

    public static String password(String s) {
        if (s == null || s.length() < 6 || s.length() > 40) return "Password must be 6-40 characters.";
        if (!s.matches(".*[A-Za-z].*") || !s.matches(".*\\d.*")) return "Password must contain a letter and a digit.";
        return null;
    }

    public static String email(String s) {
        if (s == null || !s.trim().matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) return "Enter a valid email address.";
        return null;
    }

    public static String required(String s, String field) {
        return (s == null || s.trim().isEmpty()) ? field + " is required." : null;
    }
}
