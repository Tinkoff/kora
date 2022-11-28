package ru.tinkoff.kora.resilient.timeout;

public class TimeoutException extends RuntimeException {

    public TimeoutException(String message) {
        super(message);
    }
}
