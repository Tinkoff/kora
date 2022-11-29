package ru.tinkoff.kora.resilient.fallback;


import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public interface Fallbacker {

    boolean canFallback(Throwable throwable);

    void fallback(@Nonnull Runnable runnable, @Nonnull Runnable fallback);

    <T> T fallback(@Nonnull Callable<T> supplier, @Nonnull Callable<T> fallback);
}
