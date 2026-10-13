package lab3.owasp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Pre-seeded vulnerable snippets for Lab 3. The weaknesses are intentional teaching examples.
 */
public final class OwaspExamples {
    private static final Database DATABASE = new Database();

    private OwaspExamples() {
    }

    // A01 - Broken Access Control
    public static UserRecord getUserData(String userId, String requestingUserId) {
        // Vulnerable: No authorization check
        return DATABASE.query("SELECT * FROM users WHERE id = " + userId);
    }

    // A02 - Cryptographic Failures
    public static String hashPassword(String password) {
        // Vulnerable: Weak hashing, no salt
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    // A03 - Injection
    public static UserRecord findUserByEmail(String email) {
        // Vulnerable: SQL injection
        String query = "SELECT * FROM users WHERE email = '" + email + "'";
        return DATABASE.execute(query);
    }

    // A07 - Identification and Authentication Failures
    public static String resetPassword(String email, String newPassword, String resetToken) {
        // Vulnerable: No token validation, weak session management
        UserRecord user = findUserByEmail(email);
        user.password = hashPassword(newPassword);
        return "Password reset successful";
    }

    public static class UserRecord {
        public String password;
    }

    /**
     * Stand-in so the vulnerable snippets compile. It does not represent a safe data API.
     */
    public static class Database {
        public UserRecord query(String sql) {
            return new UserRecord();
        }

        public UserRecord execute(String sql) {
            return new UserRecord();
        }
    }
}
