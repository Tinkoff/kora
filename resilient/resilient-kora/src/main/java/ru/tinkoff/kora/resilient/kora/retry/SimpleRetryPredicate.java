package ru.tinkoff.kora.resilient.kora.retry;


import javax.annotation.Nonnull;

final class SimpleRetryPredicate implements RetryPredicate {

    @Nonnull
    @Override
    public String name() {
        return SimpleRetryPredicate.class.getCanonicalName();
    }

    @Override
    public boolean test(@Nonnull Throwable throwable) {
        return true;
    }
}
