package lab3.exemplar;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typical AI-generated user manager for Lab 3 review.
 * The vulnerabilities marked below are intentional.
 */
public class UserManager implements AutoCloseable {
    private final Connection connection;

    // VULNERABLE: Hardcoded secret
    private final String secretKey = "secret123";

    public UserManager() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:users.db");
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public String registerUser(String email, String password, String personalInfo) {
        // VULNERABLE: No input validation, SQL injection possible
        String hashedPassword = hashPassword(password); // VULNERABLE: Weak hashing
        String query = "INSERT INTO users (email, password, info) VALUES ('"
                + email + "', '" + hashedPassword + "', '" + personalInfo + "')";
        executeUpdate(query);
        return "User registered";
    }

    public Map<String, Object> authenticateUser(String email, String password) {
        // VULNERABLE: SQL injection, timing attack
        String hashedPassword = hashPassword(password);
        String query = "SELECT * FROM users WHERE email = '" + email + "' AND password = '" + hashedPassword + "'";
        Map<String, Object> row = queryOne(query);
        if (row == null) {
            return null;
        }

        String token = createToken(email); // VULNERABLE: No expiration
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", row);
        return result;
    }

    public String resetPassword(String email, String newPassword, String resetToken) {
        // VULNERABLE: No token validation
        String hashedPassword = hashPassword(newPassword);
        String query = "UPDATE users SET password = '" + hashedPassword + "' WHERE email = '" + email + "'";
        executeUpdate(query);
        return "Password reset";
    }

    public Map<String, Object> getUserData(String userId, String requestingUserId) {
        // VULNERABLE: No authorization check
        String query = "SELECT * FROM users WHERE id = " + userId;
        return queryOne(query);
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    private String createToken(String email) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.create().withClaim("email", email).sign(algorithm);
    }

    // VULNERABLE: Weak hashing
    private static String hashPassword(String password) {
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

    private void executeUpdate(String query) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(query);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Map<String, Object> queryOne(String query) {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(query)) {
            if (!results.next()) {
                return null;
            }
            ResultSetMetaData metadata = results.getMetaData();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int index = 1; index <= metadata.getColumnCount(); index++) {
                row.put(metadata.getColumnLabel(index), results.getObject(index));
            }
            return row;
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
