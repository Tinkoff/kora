package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.Component;

@Component
public class MockComponentWithMock {

    private final MockComponentToMock mock;

    public MockComponentWithMock(MockComponentToMock mock) {
        this.mock = mock;
    }

    public String get() {
        return mock.get() + "2";
    }
}
