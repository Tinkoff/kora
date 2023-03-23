package ru.tinkoff.kora.resilient.retry;

/**
 * Exception that indicates all Retry attempts exceeded
 */
public class RetryAttemptException extends RuntimeException {

    public RetryAttemptException(String message) {
        super(message);
    }
}
