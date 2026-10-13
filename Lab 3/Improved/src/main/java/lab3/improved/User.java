package lab3.improved;

import java.time.LocalDateTime;

public class User {
    private final long id;
    private final String email;
    private final String passwordHash;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastLogin;
    private final int failedLoginAttempts;
    private final LocalDateTime lockedUntil;

    public User(
            long id,
            String email,
            String passwordHash,
            LocalDateTime createdAt,
            LocalDateTime lastLogin,
            int failedLoginAttempts,
            LocalDateTime lockedUntil) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockedUntil = lockedUntil;
    }

    public long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
}
