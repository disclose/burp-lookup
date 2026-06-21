package io.disclose.burplookup;

/**
 * Raised when a lookup cannot be completed (network failure, non-2xx response,
 * malformed JSON, timeout). Carries a human-readable, UI-safe message.
 */
public class LookupException extends Exception {
    public LookupException(String message) {
        super(message);
    }

    public LookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
