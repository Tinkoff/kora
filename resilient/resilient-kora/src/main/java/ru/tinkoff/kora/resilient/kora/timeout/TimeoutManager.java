package ru.tinkoff.kora.resilient.kora.timeout;


import javax.annotation.Nonnull;

/**
 * Manages state of all {@link Timeout} in system
 */
public interface TimeoutManager {

    @Nonnull
    Timeout get(@Nonnull String name);
}
