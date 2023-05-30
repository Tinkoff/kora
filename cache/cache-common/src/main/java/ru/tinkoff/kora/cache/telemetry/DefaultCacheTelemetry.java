package ru.tinkoff.kora.cache.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class DefaultCacheTelemetry implements CacheTelemetry {

    private static final Logger logger = LoggerFactory.getLogger(CacheTelemetry.class);

    private static final TelemetryContext STUB_CONTEXT = new StubCacheTelemetry();

    @Nullable
    private final CacheMetrics metrics;
    @Nullable
    private final CacheTracer tracer;
    private final boolean isStubTelemetry;

    public DefaultCacheTelemetry(@Nullable CacheMetrics metrics, @Nullable CacheTracer tracer) {
        this.metrics = metrics;
        this.tracer = tracer;
        this.isStubTelemetry = metrics == null && tracer == null;
    }

    record StubCacheTelemetry() implements TelemetryContext {
        @Override
        public void startRecording() {}

        @Override
        public void recordSuccess() {}

        @Override
        public void recordSuccess(@Nullable Object valueFromCache) {}

        @Override
        public void recordFailure(@Nullable Throwable throwable) {}
    }

    class DefaultCacheTelemetryContext implements TelemetryContext {

        private final Operation operation;

        private CacheTracer.CacheSpan span;
        private long startedInNanos = System.nanoTime();

        DefaultCacheTelemetryContext(Operation operation) {
            this.operation = operation;
        }

        @Override
        public void startRecording() {
            logger.trace("Operation '{}' for cache '{}' started", operation.name(), operation.cacheName());
            startedInNanos = System.nanoTime();

            if (tracer != null) {
                span = tracer.trace(operation);
            }
        }

        @Override
        public void recordSuccess() {
            recordSuccess(null);
        }

        @Override
        public void recordSuccess(@Nullable Object valueFromCache) {
            if (metrics != null) {
                final long durationInNanos = System.nanoTime() - startedInNanos;
                metrics.recordSuccess(operation, durationInNanos, valueFromCache);
            }
            if (span != null) {
                span.recordSuccess();
            }

            if (operation.name().startsWith("GET")) {
                if (valueFromCache == null) {
                    logger.trace("Operation '{}' for cache '{}' didn't retried value", operation.name(), operation.cacheName());
                } else {
                    logger.debug("Operation '{}' for cache '{}' retried value", operation.name(), operation.cacheName());
                }
            } else {
                logger.trace("Operation '{}' for cache '{}' completed", operation.name(), operation.cacheName());
            }
        }

        @Override
        public void recordFailure(@Nullable Throwable throwable) {
            if (metrics != null) {
                final long durationInNanos = System.nanoTime() - startedInNanos;
                metrics.recordFailure(operation, durationInNanos, throwable);
            }
            if (span != null) {
                span.recordFailure(throwable);
            }

            if (throwable != null) {
                logger.warn("Operation '{}' failed for cache '{}' with message: {}",
                    operation.name(), operation.cacheName(), throwable.getMessage());
            } else {
                logger.warn("Operation '{}' failed for cache '{}'",
                    operation.name(), operation.cacheName());
            }
        }
    }

    @Nonnull
    @Override
    public TelemetryContext create(@Nonnull String operationName, @Nonnull String cacheName, @Nonnull String origin) {
        if (isStubTelemetry) {
            return STUB_CONTEXT;
        }

        return new DefaultCacheTelemetryContext(new Operation(operationName, cacheName, origin));
    }
}
