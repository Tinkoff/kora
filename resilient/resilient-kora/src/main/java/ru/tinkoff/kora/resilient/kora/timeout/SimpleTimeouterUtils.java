package ru.tinkoff.kora.resilient.kora.timeout;

final class SimpleTimeouterUtils {

    private SimpleTimeouterUtils() {}

    static void doThrow(Throwable e) {
        SimpleTimeouterUtils.doThrow0(e);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Exception> void doThrow0(Throwable e) throws E {
        throw (E) e;
    }
}
