package ru.tinkoff.kora.resilient.retry;

import reactor.util.retry.Retry;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public interface Retrier {

    interface RetryState {

        boolean canRetry(@Nonnull Throwable throwable);

        void checkRetry(@Nonnull Throwable throwable) throws RetryException;

        long getDelayNanos();

        void doDelay();
    }

    @Nonnull
    RetryState asState();

    @Nonnull
    Retry asReactor();

    void retry(@Nonnull Runnable runnable) throws RetryException;

    <T> T retry(@Nonnull Supplier<T> supplier) throws RetryException;

    <T> T retry(@Nonnull Supplier<T> supplier, Supplier<T> fallback) throws RetryException;
}
