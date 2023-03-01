package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.Component;

@Component
public class MockComponentToMock {

    public String get() {
        return "1";
    }
}
