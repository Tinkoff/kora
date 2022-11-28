package ru.tinkoff.kora.resilient.retry.simple;

import reactor.util.retry.Retry;
import ru.tinkoff.kora.resilient.retry.Retrier;
import ru.tinkoff.kora.resilient.retry.RetrierFailurePredicate;
import ru.tinkoff.kora.resilient.retry.RetryAttemptException;
import ru.tinkoff.kora.resilient.retry.RetryException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

record SimpleRetrier(long delayNanos,
                     long delayMaxNanos,
                     long delayStepNanos,
                     long attempts,
                     RetrierFailurePredicate failurePredicate,
                     ExecutorService executor) implements Retrier {

    public SimpleRetrier(SimpleRetrierConfig.NamedConfig config, RetrierFailurePredicate failurePredicate, ExecutorService executors) {
        this(config.delay().toNanos(), config.delayMax().toNanos(), config.delayStep().toNanos(), config.attempts(), failurePredicate, executors);
    }

    @Nonnull
    @Override
    public RetryState asState() {
        return new SimpleRetrierRetryState(System.nanoTime(), delayNanos, delayMaxNanos, delayStepNanos, attempts, failurePredicate, new AtomicInteger(0));
    }

    @Nonnull
    @Override
    public Retry asReactor() {
        return new SimpleReactorRetry(delayNanos, delayMaxNanos, delayStepNanos, attempts, failurePredicate);
    }

    @Override
    public void retry(@Nonnull Runnable runnable) throws RetryException {
        internalRetry(e -> e.submit(runnable), null);
    }

    @Override
    public <T> T retry(@Nonnull Supplier<T> supplier) throws RetryException {
        return internalRetry(e -> e.submit(supplier::get), null);
    }

    @Override
    public <T> T retry(@Nonnull Supplier<T> supplier, @Nonnull Supplier<T> fallback) throws RetryException {
        return internalRetry(e -> e.submit(supplier::get), fallback);
    }

    private <T> T internalRetry(Function<ExecutorService, Future<T>> consumer, @Nullable Supplier<T> fallback) throws RetryException {
        var counter = asState();
        for (long i = 0; i < attempts; i++) {
            try {
                return (delayMaxNanos == 0)
                    ? consumer.apply(executor).get()
                    : consumer.apply(executor).get(delayMaxNanos, TimeUnit.NANOSECONDS);
            } catch (Throwable e) {
                counter.checkRetry(e);
                counter.doDelay();
            }
        }

        if (fallback != null) {
            return fallback.get();
        }

        throw new RetryAttemptException("All '" + attempts + "' attempts elapsed during retry");
    }
}
