package ru.tinkoff.kora.resilient.retry;

import reactor.util.retry.Retry;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * Retry executor implementation
 */
public interface Retrier {

    /**
     * Retry State implementation for manual retry execution handling
     */
    interface RetryState extends AutoCloseable {
        sealed interface CanRetryResult {
            enum CanRetry implements CanRetryResult {INSTANCE}

            enum CantRetry implements CanRetryResult {INSTANCE}

            record RetryExhausted(int attempts) implements CanRetryResult {
                public RetryAttemptException toException() {
                    return new RetryAttemptException("All '" + this.attempts + "' attempts elapsed during retry");
                }
            }
        }

        CanRetryResult canRetry(@Nonnull Throwable throwable);

        long getDelayNanos();

        void doDelay();

        @Override
        void close();
    }

    /**
     * @return new {@link RetryState}
     */
    @Nonnull
    RetryState asState();

    /**
     * @return new {@link Retry} implementation for Project Reactor retry handling
     */
    @Nonnull
    Retry asReactor();

    void retry(@Nonnull Runnable runnable) throws RetryAttemptException;

    <T> T retry(@Nonnull Supplier<T> supplier) throws RetryAttemptException;

    <T> T retry(@Nonnull Supplier<T> supplier, Supplier<T> fallback) throws RetryAttemptException;
}
