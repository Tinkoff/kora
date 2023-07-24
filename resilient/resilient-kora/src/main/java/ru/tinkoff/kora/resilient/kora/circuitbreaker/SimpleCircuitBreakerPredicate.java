package ru.tinkoff.kora.resilient.kora.circuitbreaker;

import javax.annotation.Nonnull;

final class SimpleCircuitBreakerPredicate implements CircuitBreakerPredicate {

    @Nonnull
    @Override
    public String name() {
        return SimpleCircuitBreakerPredicate.class.getCanonicalName();
    }

    @Override
    public boolean test(@Nonnull Throwable throwable) {
        return true;
    }
}
