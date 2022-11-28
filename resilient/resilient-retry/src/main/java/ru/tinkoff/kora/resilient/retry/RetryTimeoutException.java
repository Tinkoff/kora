package ru.tinkoff.kora.resilient.retry;

public class RetryTimeoutException extends RetryException {

    public RetryTimeoutException(String message) {
        super(message);
    }
}
