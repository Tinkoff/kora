package ru.tinkoff.kora.resilient.kora.fallback;


import javax.annotation.Nonnull;

final class SimpleFallbackPredicate implements FallbackPredicate {

    @Nonnull
    @Override
    public String name() {
        return SimpleFallbackPredicate.class.getCanonicalName();
    }

    @Override
    public boolean test(@Nonnull Throwable throwable) {
        return true;
    }
}
