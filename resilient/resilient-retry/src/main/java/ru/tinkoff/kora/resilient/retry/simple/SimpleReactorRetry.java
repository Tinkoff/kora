package ru.tinkoff.kora.resilient.retry.simple;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import ru.tinkoff.kora.resilient.retry.RetrierFailurePredicate;
import ru.tinkoff.kora.resilient.retry.RetryAttemptException;
import ru.tinkoff.kora.resilient.retry.RetryTimeoutException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

final class SimpleReactorRetry extends Retry {

    private final long delayNanos;
    private final long delayMaxNanos;
    private final long delayStepNanos;
    private final long attempts;
    private final RetrierFailurePredicate failurePredicate;

    SimpleReactorRetry(long delayNanos, long delayMaxNanos, long delayStepNanos, long attempts, RetrierFailurePredicate failurePredicate) {
        this.delayNanos = delayNanos;
        this.delayMaxNanos = delayMaxNanos;
        this.delayStepNanos = delayStepNanos;
        this.attempts = attempts;
        this.failurePredicate = failurePredicate;
    }

    @Override
    public Publisher<?> generateCompanion(Flux<RetrySignal> retrySignals) {
        final AtomicLong started = new AtomicLong();
        return retrySignals
            .doOnSubscribe(e -> started.set(System.nanoTime()))
            .concatMap(retryWhenState -> {
                //capture the state immediately
                final RetrySignal signal = retryWhenState.copy();
                final Throwable currentFailure = signal.failure();
                if (currentFailure == null) {
                    return Mono.error(new IllegalStateException("Retry.RetrySignal#failure() not expected to be null"));
                }

                if (!failurePredicate.test(currentFailure)) {
                    return Mono.error(currentFailure);
                }

                if (signal.totalRetries() >= attempts) {
                    return Mono.error(new RetryAttemptException("All '" + attempts + "' attempts elapsed during retry"));
                }

                if (delayMaxNanos != 0) {
                    final long spent = System.nanoTime() - started.get();
                    if (spent > delayMaxNanos) {
                        return Mono.error(new RetryTimeoutException("Max Delay '" + Duration.ofNanos(delayNanos) + "' elapsed during retry"));
                    }

                    long nextDelayNanos = delayNanos + (delayStepNanos * (signal.totalRetries() - 1));
                    final long diff = delayMaxNanos - spent;
                    if (nextDelayNanos > diff) {
                        nextDelayNanos = diff;
                    }

                    return Mono.delay(Duration.ofNanos(nextDelayNanos), Schedulers.parallel());
                } else {
                    final long nextDelayNanos = delayNanos + (delayStepNanos * (signal.totalRetries() - 1));
                    return Mono.delay(Duration.ofNanos(nextDelayNanos), Schedulers.parallel());
                }
            });
    }
}
