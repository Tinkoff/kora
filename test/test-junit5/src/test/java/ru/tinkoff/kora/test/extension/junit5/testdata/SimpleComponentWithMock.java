package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.Component;

@Component
public class SimpleComponentWithMock {

    private final SimpleComponentToMock mock;

    public SimpleComponentWithMock(SimpleComponentToMock mock) {
        this.mock = mock;
    }

    public String get() {
        return mock.get() + "2";
    }
}
