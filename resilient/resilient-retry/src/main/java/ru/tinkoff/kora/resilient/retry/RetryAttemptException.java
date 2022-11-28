package ru.tinkoff.kora.resilient.retry;

public class RetryAttemptException extends RetryException {

    public RetryAttemptException(String message) {
        super(message);
    }
}
