package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.common.Component;

@Component
public class SimpleComponentToMock {

    public String get() {
        return "1";
    }
}
