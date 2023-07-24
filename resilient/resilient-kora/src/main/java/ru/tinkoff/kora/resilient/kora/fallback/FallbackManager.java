package ru.tinkoff.kora.resilient.kora.fallback;


import javax.annotation.Nonnull;

public interface FallbackManager {

    @Nonnull
    Fallback get(@Nonnull String name);
}
