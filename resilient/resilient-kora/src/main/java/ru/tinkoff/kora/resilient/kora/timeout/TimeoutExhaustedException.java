package ru.tinkoff.kora.resilient.kora.timeout;

import ru.tinkoff.kora.resilient.kora.ResilientException;

public final class TimeoutExhaustedException extends ResilientException {

    public TimeoutExhaustedException(String name, String message) {
        super(name, message);
    }
}
