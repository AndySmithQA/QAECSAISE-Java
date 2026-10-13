package copilotdemo;

import java.util.regex.Pattern;

public final class EmailValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^[a-zA-Z0-9.-]+$");

    private EmailValidator() {
    }

    /**
     * Validates an email address and returns a detailed message.
     * The result is a pair: whether the address is valid, and a message explaining the result.
     */
    public static Result validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            return new Result(false, "Email address is required.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return new Result(false, "Invalid email format. Ensure it follows 'example@domain.com'.");
        }
        if (email.contains("..")) {
            return new Result(false, "Email address contains consecutive dots.");
        }
        if (email.startsWith(".") || email.endsWith(".")) {
            return new Result(false, "Email address cannot start or end with a dot.");
        }
        if (!email.contains("@")) {
            return new Result(false, "Email address must contain an '@' symbol.");
        }

        int separator = email.indexOf('@');
        String localPart = email.substring(0, separator);
        String domainPart = email.substring(separator + 1);

        if (localPart.length() > 64) {
            return new Result(false, "The local part of the email (before '@') exceeds 64 characters.");
        }
        if (domainPart.length() > 255) {
            return new Result(false, "The domain part of the email (after '@') exceeds 255 characters.");
        }
        if (!DOMAIN_PATTERN.matcher(domainPart).matches()) {
            return new Result(false, "The domain part contains invalid characters.");
        }
        if (domainPart.startsWith("-") || domainPart.endsWith("-")) {
            return new Result(false, "The domain part cannot start or end with a hyphen.");
        }
        return new Result(true, "Email address is valid.");
    }

    public static void main(String[] args) {
        String[] testEmails = {
                "example@domain.com",
                "invalid-email@domain..com"
        };
        for (String email : testEmails) {
            Result result = validateEmail(email);
            System.out.println("Email: " + email + " - Valid: " + result.valid() + ", Message: " + result.message());
        }
    }

    public record Result(boolean valid, String message) {
    }
}
