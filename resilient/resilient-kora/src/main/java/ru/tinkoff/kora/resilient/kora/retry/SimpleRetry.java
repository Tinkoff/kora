package ru.tinkoff.kora.resilient.kora.retry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

final class SimpleRetry implements Retry {

    private final String name;
    private final long delayNanos;
    private final long delayStepNanos;
    private final int attempts;
    private final RetryPredicate failurePredicate;
    private final RetryMetrics metrics;

    SimpleRetry(String name,
                long delayNanos,
                long delayStepNanos,
                int attempts,
                RetryPredicate failurePredicate,
                RetryMetrics metrics) {
        this.name = name;
        this.delayNanos = delayNanos;
        this.delayStepNanos = delayStepNanos;
        this.attempts = attempts;
        this.failurePredicate = failurePredicate;
        this.metrics = metrics;
    }

    SimpleRetry(String name, RetryConfig.NamedConfig config, RetryPredicate failurePredicate, RetryMetrics metric) {
        this(name, config.delay().toNanos(), config.delayStep().toNanos(), config.attempts(), failurePredicate, metric);
    }

    @Nonnull
    @Override
    public RetryState asState() {
        return new SimpleRetryState(name, System.nanoTime(), delayNanos, delayStepNanos, attempts, failurePredicate, metrics, new AtomicInteger(0));
    }

    @Nonnull
    @Override
    public reactor.util.retry.Retry asReactor() {
        return new SimpleReactorRetry(name, delayNanos, delayStepNanos, attempts, failurePredicate, metrics);
    }

    @Override
    public void retry(@Nonnull Runnable runnable) {
        internalRetry(() -> {
            runnable.run();
            return null;
        }, null);
    }

    @Override
    public <T> T retry(@Nonnull Supplier<T> supplier) {
        return internalRetry(supplier, null);
    }

    @Override
    public <T> T retry(@Nonnull Supplier<T> supplier, @Nonnull Supplier<T> fallback) {
        return internalRetry(supplier, fallback);
    }

    private <T> T internalRetry(Supplier<T> consumer, @Nullable Supplier<T> fallback) {
        final List<Exception> suppressed = new ArrayList<>();
        try (var state = asState()) {
            while (true) {
                try {
                    return consumer.get();
                } catch (Exception e) {
                    var status = state.onException(e);
                    if (status == RetryState.RetryStatus.REJECTED) {
                        for (Exception exception : suppressed) {
                            e.addSuppressed(exception);
                        }

                        throw e;
                    } else if (status == RetryState.RetryStatus.ACCEPTED) {
                        suppressed.add(e);
                        state.doDelay();
                    } else if (status == RetryState.RetryStatus.EXHAUSTED) {
                        if (fallback != null) {
                            try {
                                return fallback.get();
                            } catch (Exception ex) {
                                for (Exception exception : suppressed) {
                                    ex.addSuppressed(exception);
                                }
                                throw ex;
                            }
                        }

                        final RetryExhaustedException exhaustedException = new RetryExhaustedException(attempts, e);
                        for (Exception exception : suppressed) {
                            exhaustedException.addSuppressed(exception);
                        }

                        throw exhaustedException;
                    }
                }
            }
        }
    }
}
