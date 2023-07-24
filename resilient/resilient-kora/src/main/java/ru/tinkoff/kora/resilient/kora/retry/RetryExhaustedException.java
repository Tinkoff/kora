package ru.tinkoff.kora.resilient.kora.retry;

import ru.tinkoff.kora.resilient.kora.ResilientException;

import javax.annotation.Nonnull;

/**
 * Exception that indicates all Retry attempts exhausted
 */
public final class RetryExhaustedException extends ResilientException {

    public RetryExhaustedException(int attempts, @Nonnull Throwable cause) {
        super("All '" + attempts + "' retry attempts exhausted", cause);
    }
}
