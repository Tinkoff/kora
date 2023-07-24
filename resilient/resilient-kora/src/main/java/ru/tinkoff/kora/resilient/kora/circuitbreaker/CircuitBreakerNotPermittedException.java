package ru.tinkoff.kora.resilient.kora.circuitbreaker;

import ru.tinkoff.kora.resilient.kora.ResilientException;

public final class CircuitBreakerNotPermittedException extends ResilientException {

    public CircuitBreakerNotPermittedException(CircuitBreaker.State state, String name) {
        super(name, "Call Is Not Permitted due to CircuitBreaker '" + name + "' been in " + state + " state");
    }
}
