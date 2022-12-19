package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.annotation.processor.common.MockLifecycle;
import ru.tinkoff.kora.common.Component;

@Component
public class TestFirstComponent implements MockLifecycle {

    public String get() {
        return "1";
    }
}
