package lab3.improved;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.mindrot.jbcrypt.BCrypt;
import redis.clients.jedis.Jedis;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Reference user manager for Lab 3: parameterized SQL, bcrypt, authorization, and expiring tokens.
 */
public class UserManager {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    private final String dbPath;
    private final String secretKey;
    private final Jedis redis;
    private final int maxLoginAttempts = 5;
    private final Duration lockoutDuration = Duration.ofMinutes(30);
    private final Duration tokenExpiry = Duration.ofHours(1);
    private final Duration resetTokenExpiry = Duration.ofMinutes(15);

    public UserManager(String dbPath, String secretKey, Jedis redis) {
        this.dbPath = dbPath;
        this.secretKey = secretKey; // Should be supplied from configuration, not hard-coded by callers of this lab
        this.redis = redis;
        initializeDatabase();
    }

    public Map<String, Object> registerUser(String email, String password, Map<String, Object> personalInfo) {
        if (email == null || email.isEmpty()) {
            throw new SecurityError("Invalid email");
        }
        if (!isValidEmail(email)) {
            throw new SecurityError("Invalid email format");
        }
        if (password == null || password.isEmpty()) {
            throw new SecurityError("Invalid password");
        }
        if (!isStrongPassword(password)) {
            throw new SecurityError("Password does not meet security requirements");
        }
        if (!checkRateLimit("register:" + email, 3, 3600)) {
            throw new SecurityError("Too many registration attempts");
        }

        String normalizedEmail = email.toLowerCase().trim();
        String passwordHash = hashPassword(password);
        long[] userId = new long[1];

        withConnection(connection -> {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO users (email, password_hash) VALUES (?, ?)")) {
                insert.setString(1, normalizedEmail);
                insert.setString(2, passwordHash);
                insert.executeUpdate();
            }
            try (PreparedStatement idQuery = connection.prepareStatement("SELECT last_insert_rowid()")) {
                try (ResultSet keys = idQuery.executeQuery()) {
                    if (keys.next()) {
                        userId[0] = keys.getLong(1);
                    }
                }
            }
        });

        logInfo("User registered successfully: " + normalizedEmail);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user_id", userId[0]);
        result.put("email", normalizedEmail);
        result.put("message", "User registered successfully");
        return result;
    }

    public Map<String, Object> authenticateUser(String email, String password) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            pause();
            return null;
        }
        if (!checkRateLimit("login:" + email, 10, 900)) {
            throw new SecurityError("Too many login attempts");
        }

        String normalizedEmail = email.toLowerCase().trim();
        UserRow[] userHolder = new UserRow[1];
        boolean[] passwordMatched = new boolean[1];

        withConnection(connection -> {
            UserRow userRow = findUserByEmail(connection, normalizedEmail);
            userHolder[0] = userRow;
            if (userRow == null) {
                pause();
                return;
            }
            if (isUserLocked(userRow)) {
                throw new SecurityError("Account is temporarily locked");
            }
            if (!verifyPassword(password, userRow.passwordHash)) {
                int newAttempts = userRow.failedLoginAttempts + 1;
                String lockedUntil = null;
                if (newAttempts >= maxLoginAttempts) {
                    lockedUntil = formatTimestamp(LocalDateTime.now().plus(lockoutDuration));
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE users SET failed_login_attempts = ?, locked_until = ? WHERE id = ?")) {
                    update.setInt(1, newAttempts);
                    update.setString(2, lockedUntil);
                    update.setLong(3, userRow.id);
                    update.executeUpdate();
                }
                logWarning("Failed login attempt for " + normalizedEmail + ". Attempts: " + newAttempts);
                return;
            }

            passwordMatched[0] = true;
            try (PreparedStatement success = connection.prepareStatement(
                    "UPDATE users SET failed_login_attempts = 0, locked_until = NULL, last_login = ? WHERE id = ?")) {
                success.setString(1, formatTimestamp(LocalDateTime.now()));
                success.setLong(2, userRow.id);
                success.executeUpdate();
            }
        });

        UserRow userRow = userHolder[0];
        if (userRow == null || !passwordMatched[0]) {
            return null;
        }

        Date issuedAt = new Date();
        Date expiresAt = Date.from(Instant.now().plus(tokenExpiry));
        String token = JWT.create()
                .withClaim("user_id", userRow.id)
                .withClaim("email", userRow.email)
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .sign(algorithm());

        logInfo("Successful login for " + normalizedEmail);
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", userRow.id);
        user.put("email", userRow.email);
        user.put("last_login", userRow.lastLogin);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    public String generateResetToken(String email) {
        if (!isValidEmail(email)) {
            throw new SecurityError("Invalid email");
        }
        if (!checkRateLimit("reset:" + email, 3, 3600)) {
            throw new SecurityError("Too many reset attempts");
        }

        String normalizedEmail = email.toLowerCase().trim();
        String[] tokenHolder = new String[1];
        boolean[] userExists = new boolean[1];

        withConnection(connection -> {
            try (PreparedStatement lookup = connection.prepareStatement("SELECT id FROM users WHERE email = ?")) {
                lookup.setString(1, normalizedEmail);
                try (ResultSet results = lookup.executeQuery()) {
                    if (!results.next()) {
                        logWarning("Password reset requested for non-existent email: " + normalizedEmail);
                        return;
                    }
                    long userId = results.getLong(1);
                    userExists[0] = true;
                    tokenHolder[0] = generateUrlSafeToken();
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO reset_tokens (token, user_id) VALUES (?, ?)")) {
                        insert.setString(1, tokenHolder[0]);
                        insert.setLong(2, userId);
                        insert.executeUpdate();
                    }
                }
            }
        });

        if (!userExists[0] || tokenHolder[0] == null) {
            return "If the email exists, a reset link has been sent";
        }
        logInfo("Password reset token generated for " + normalizedEmail);
        return tokenHolder[0];
    }

    public Map<String, String> resetPassword(String resetToken, String newPassword) {
        if (resetToken == null || resetToken.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            throw new SecurityError("Invalid reset request");
        }
        if (!isStrongPassword(newPassword)) {
            throw new SecurityError("Password does not meet security requirements");
        }

        long[] userId = new long[1];
        withConnection(connection -> {
            try (PreparedStatement lookup = connection.prepareStatement(
                    "SELECT rt.user_id, rt.created_at, rt.used, u.email "
                            + "FROM reset_tokens rt JOIN users u ON rt.user_id = u.id WHERE rt.token = ?")) {
                lookup.setString(1, resetToken);
                try (ResultSet results = lookup.executeQuery()) {
                    if (!results.next()) {
                        throw new SecurityError("Invalid reset token");
                    }
                    boolean used = results.getInt(3) != 0;
                    if (used) {
                        throw new SecurityError("Reset token already used");
                    }
                    LocalDateTime createdAt = parseTimestamp(results.getString(2));
                    if (Duration.between(createdAt, LocalDateTime.now()).compareTo(resetTokenExpiry) > 0) {
                        throw new SecurityError("Reset token expired");
                    }
                    userId[0] = results.getLong(1);
                }
            }

            try (PreparedStatement updateUser = connection.prepareStatement(
                    "UPDATE users SET password_hash = ?, failed_login_attempts = 0, locked_until = NULL WHERE id = ?")) {
                updateUser.setString(1, hashPassword(newPassword));
                updateUser.setLong(2, userId[0]);
                updateUser.executeUpdate();
            }
            try (PreparedStatement updateToken = connection.prepareStatement(
                    "UPDATE reset_tokens SET used = 1 WHERE token = ?")) {
                updateToken.setString(1, resetToken);
                updateToken.executeUpdate();
            }
        });

        logInfo("Password reset successful for user " + userId[0]);
        return Map.of("message", "Password reset successful");
    }

    public Map<String, Object> getUserData(long userId, long requestingUserId) {
        if (userId != requestingUserId) {
            logWarning("Unauthorized access attempt: user " + requestingUserId + " tried to access user " + userId);
            throw new SecurityError("Unauthorized access");
        }

        AtomicReference<Map<String, Object>> result = new AtomicReference<>();
        withConnection(connection -> {
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT id, email, created_at, last_login FROM users WHERE id = ?")) {
                query.setLong(1, userId);
                try (ResultSet results = query.executeQuery()) {
                    if (!results.next()) {
                        return;
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", results.getLong(1));
                    row.put("email", results.getString(2));
                    row.put("created_at", results.getString(3));
                    row.put("last_login", results.getString(4));
                    result.set(row);
                }
            }
        });
        return result.get();
    }

    public Map<String, Object> verifyToken(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm()).acceptLeeway(0).build().verify(token);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("user_id", jwt.getClaim("user_id").asLong());
            payload.put("email", jwt.getClaim("email").asString());
            payload.put("iat", jwt.getIssuedAt());
            payload.put("exp", jwt.getExpiresAt());
            return payload;
        } catch (TokenExpiredException exception) {
            throw new SecurityError("Token expired");
        } catch (JWTVerificationException exception) {
            throw new SecurityError("Invalid token");
        }
    }

    private void initializeDatabase() {
        withConnection(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS users ("
                                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                                + "email TEXT UNIQUE NOT NULL, "
                                + "password_hash TEXT NOT NULL, "
                                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                                + "last_login TIMESTAMP, "
                                + "failed_login_attempts INTEGER DEFAULT 0, "
                                + "locked_until TIMESTAMP)");
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS reset_tokens ("
                                + "token TEXT PRIMARY KEY, "
                                + "user_id INTEGER NOT NULL, "
                                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                                + "used INTEGER DEFAULT 0, "
                                + "FOREIGN KEY (user_id) REFERENCES users (id))");
            }
        });
    }

    private void withConnection(ConnectionWork work) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            connection.setAutoCommit(false);
            try {
                work.run(connection);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                if (isUniqueViolation(exception)) {
                    throw new SecurityError("Email already exists");
                }
                if (exception instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException(exception);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private static boolean isStrongPassword(String password) {
        return password.length() >= 8
                && UPPER.matcher(password).find()
                && LOWER.matcher(password).find()
                && DIGIT.matcher(password).find()
                && SPECIAL.matcher(password).find();
    }

    private static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    private static boolean verifyPassword(String password, String passwordHash) {
        try {
            return BCrypt.checkpw(password, passwordHash);
        } catch (Exception exception) {
            try {
                BCrypt.checkpw("dummy", BCrypt.hashpw("dummy", BCrypt.gensalt()));
            } catch (Exception ignored) {
                // Keep the failure path from surfacing a different error.
            }
            return false;
        }
    }

    private static boolean isUserLocked(UserRow user) {
        if (user.lockedUntil == null || user.lockedUntil.isEmpty()) {
            return false;
        }
        return LocalDateTime.now().isBefore(parseTimestamp(user.lockedUntil));
    }

    private boolean checkRateLimit(String identifier, int maxAttempts, int windowSeconds) {
        String key = "rate_limit:" + identifier;
        String currentAttempts = redis.get(key);
        if (currentAttempts == null) {
            redis.setex(key, windowSeconds, "1");
            return true;
        }
        if (Integer.parseInt(currentAttempts) >= maxAttempts) {
            return false;
        }
        redis.incr(key);
        return true;
    }

    private Algorithm algorithm() {
        return Algorithm.HMAC256(secretKey);
    }

    private static UserRow findUserByEmail(Connection connection, String email) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT * FROM users WHERE email = ?")) {
            query.setString(1, email);
            try (ResultSet results = query.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                return new UserRow(
                        results.getLong("id"),
                        results.getString("email"),
                        results.getString("password_hash"),
                        results.getString("created_at"),
                        results.getString("last_login"),
                        results.getInt("failed_login_attempts"),
                        results.getString("locked_until"));
            }
        }
    }

    private static boolean isUniqueViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sql) {
                if (sql.getErrorCode() == 19 || (sql.getMessage() != null && sql.getMessage().toUpperCase().contains("UNIQUE"))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static String formatTimestamp(LocalDateTime value) {
        return value.format(TIMESTAMP);
    }

    private static LocalDateTime parseTimestamp(String value) {
        String normalized = value.replace('T', ' ');
        if (normalized.length() > 19) {
            normalized = normalized.substring(0, 19);
        }
        return LocalDateTime.parse(normalized, TIMESTAMP);
    }

    private static String generateUrlSafeToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void pause() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static void logInfo(String message) {
        System.err.println("INFO: " + message);
    }

    private static void logWarning(String message) {
        System.err.println("WARNING: " + message);
    }

    @FunctionalInterface
    private interface ConnectionWork {
        void run(Connection connection) throws Exception;
    }

    private static final class UserRow {
        private final long id;
        private final String email;
        private final String passwordHash;
        private final String createdAt;
        private final String lastLogin;
        private final int failedLoginAttempts;
        private final String lockedUntil;

        private UserRow(
                long id,
                String email,
                String passwordHash,
                String createdAt,
                String lastLogin,
                int failedLoginAttempts,
                String lockedUntil) {
            this.id = id;
            this.email = email;
            this.passwordHash = passwordHash;
            this.createdAt = createdAt;
            this.lastLogin = lastLogin;
            this.failedLoginAttempts = failedLoginAttempts;
            this.lockedUntil = lockedUntil;
        }
    }
}
