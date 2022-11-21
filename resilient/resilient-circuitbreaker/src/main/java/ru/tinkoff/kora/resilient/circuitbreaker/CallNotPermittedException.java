package ru.tinkoff.kora.resilient.circuitbreaker;

public final class CallNotPermittedException extends RuntimeException {

    /**
     * {@link CircuitBreaker} name
     */
    private final String name;

    public CallNotPermittedException(String message, String name) {
        super(message);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
