package ru.tinkoff.kora.resilient.fallback.simple;

import ru.tinkoff.kora.resilient.fallback.FallbackFailurePredicate;

import javax.annotation.Nonnull;

final class DefaultFallbackFailurePredicate implements FallbackFailurePredicate {

    @Nonnull
    @Override
    public String name() {
        return DefaultFallbackFailurePredicate.class.getCanonicalName();
    }

    @Override
    public boolean test(@Nonnull Throwable throwable) {
        return true;
    }
}
