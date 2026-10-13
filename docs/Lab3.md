# Lab 3 - Spot and fix vulnerabilities

**Goal:** strengthen security awareness in AI-generated code.

**Scenario:** Create a user management system with registration, login, password reset, and profile management.

## Phase 1: AI code generation

Prompt AI for a user manager with:

1. `registerUser(email, password, personalInfo)`
2. `authenticateUser(email, password)`
3. `resetPassword(email, newPassword, resetToken)`
4. `updateProfile(userId, profileData)`
5. `deleteUser(userId, confirmPassword)`
6. `getUserData(userId, requestingUserId)`

## Phase 2: Deliberate vulnerability introduction

`Lab 3/OWASP` contains the pre-seeded snippets for this phase. The weaknesses are intentional and are the review exercise.

**A01 - Broken access control**

```java
public static UserRecord getUserData(String userId, String requestingUserId) {
    // Vulnerable: No authorization check
    return DATABASE.query("SELECT * FROM users WHERE id = " + userId);
}
```

**A02 - Cryptographic failures**

```java
public static String hashPassword(String password) {
    // Vulnerable: Weak hashing, no salt
    MessageDigest digest = MessageDigest.getInstance("MD5");
    byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
    // ...hex encoding...
}
```

**A03 - Injection**

```java
public static UserRecord findUserByEmail(String email) {
    // Vulnerable: SQL injection
    String query = "SELECT * FROM users WHERE email = '" + email + "'";
    return DATABASE.execute(query);
}
```

**A07 - Identification and authentication failures**

```java
public static String resetPassword(String email, String newPassword, String resetToken) {
    // Vulnerable: No token validation, weak session management
    UserRecord user = findUserByEmail(email);
    user.password = hashPassword(newPassword);
    return "Password reset successful";
}
```

`Lab 3/Exemplar` is a fuller AI-generated user manager with the same class of issues: string-built SQL, MD5 passwords, a hard-coded signing secret, tokens with no expiry, and no check that the caller may read the requested user.

## Phase 3: Security analysis

Review the exemplar by hand, then run a scanner such as SonarQube or SpotBugs. Compare what you found with what the tool found.

## Phase 4: Secure implementation

`Lab 3/Improved` is the reference fix:

- Parameterized SQL
- bcrypt password hashes
- Authorization on user reads
- Signed tokens with an expiry
- Password strength checks
- Redis-backed rate limiting and account lockout

The signing key is a constructor argument. Supply a long random value from configuration. Use at least 32 characters.

## Phase 5: Security testing

Write tests that show the exemplar behaviour is no longer possible: injected SQL is treated as data, a caller cannot read another user's record, and a reset without a valid unused token does not change the password.
