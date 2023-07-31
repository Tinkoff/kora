package ru.tinkoff.kora.resilient.kora.fallback;

import javax.annotation.Nonnull;

public interface FallbackMetrics {

    void recordExecute(@Nonnull String name, @Nonnull Throwable throwable);
}
