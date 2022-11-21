package ru.tinkoff.kora.resilient.fallback.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.kora.resilient.fallback.FallbackFailurePredicate;
import ru.tinkoff.kora.resilient.fallback.Fallbacker;
import ru.tinkoff.kora.resilient.fallback.telemetry.FallbackMetrics;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

final class SimpleFallbacker implements Fallbacker {

    private static final Logger logger = LoggerFactory.getLogger(SimpleFallbacker.class);

    private final String name;
    private final FallbackMetrics metrics;
    private final FallbackFailurePredicate failurePredicate;

    SimpleFallbacker(String name, FallbackMetrics metrics, FallbackFailurePredicate failurePredicate) {
        this.name = name;
        this.metrics = metrics;
        this.failurePredicate = failurePredicate;
    }

    @Override
    public boolean canFallback(Throwable throwable) {
        if(failurePredicate.test(throwable)) {
            logger.trace("Recorded possible Fallback named: {}", name);
            metrics.recordFallback(name, throwable);
            return true;
        } else {
            logger.trace("Recorded possible Fallback named '{}' failure predicate didn't pass exception for: {}", name, throwable);
            metrics.recordSkip(name, throwable);
            return false;
        }
    }

    @Override
    public void fallback(@Nonnull Runnable runnable, @Nonnull Runnable fallback) {
        try {
            runnable.run();
        } catch (Throwable e) {
            if (failurePredicate.test(e)) {
                logger.trace("Executing Fallback named: {}", name);
                fallback.run();
                metrics.recordFallback(name, e);
            } else {
                logger.trace("Fallback named '{}' failure predicate didn't pass exception for: {}", name, e);
                metrics.recordSkip(name, e);
                throw e;
            }
        }
    }

    @Override
    public <T> T fallback(@Nonnull Supplier<T> supplier, @Nonnull Supplier<T> fallback) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            if (failurePredicate.test(e)) {
                logger.trace("Executing Fallback named: {}", name);
                final T fallbackValue = fallback.get();
                metrics.recordFallback(name, e);
                return fallbackValue;
            } else {
                logger.trace("Fallback named '{}' failure predicate didn't pass exception for: {}", name, e);
                metrics.recordSkip(name, e);
                throw e;
            }
        }
    }
}
