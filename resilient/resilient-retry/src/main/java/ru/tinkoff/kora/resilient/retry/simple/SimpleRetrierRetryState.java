package ru.tinkoff.kora.resilient.retry.simple;

import ru.tinkoff.kora.resilient.retry.Retrier;
import ru.tinkoff.kora.resilient.retry.RetrierFailurePredicate;
import ru.tinkoff.kora.resilient.retry.telemetry.RetryMetrics;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

record SimpleRetrierRetryState(
    String name,
    long started,
    long delayNanos,
    long delayStepNanos,
    int attemptsMax,
    RetrierFailurePredicate failurePredicate,
    RetryMetrics metrics,
    AtomicInteger attempts
) implements Retrier.RetryState {

    @Override
    public long getDelayNanos() {
        return delayNanos + delayStepNanos * attempts.get();
    }

    @Override
    public Retrier.RetryState.CanRetryResult canRetry(@Nonnull Throwable throwable) {
        if (!failurePredicate.test(throwable)) {
            return CanRetryResult.CantRetry.INSTANCE;
        }

        var attempts = this.attempts.incrementAndGet();
        if (attempts <= attemptsMax) {
            return CanRetryResult.CanRetry.INSTANCE;
        } else {
            return new CanRetryResult.RetryExhausted(attempts);
        }
    }

    @Override
    public void doDelay() {
        long nextDelayNanos = getDelayNanos();
        metrics.recordAttempt(name, nextDelayNanos);
        sleepUninterruptibly(nextDelayNanos);
    }

    @Override
    public void close() {
        if (attempts.get() > attemptsMax) {
            metrics.recordExhaustedAttempts(name, attemptsMax);
        }
    }

    private static void sleepUninterruptibly(final long sleepForNanos) {
        boolean interrupted = false;

        try {
            long remainingNanos = sleepForNanos;
            long end = System.nanoTime() + remainingNanos;

            while (true) {
                try {
                    TimeUnit.NANOSECONDS.sleep(remainingNanos);
                    return;
                } catch (InterruptedException ex) {
                    interrupted = true;
                    remainingNanos = end - System.nanoTime();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
