package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.annotation.processor.common.MockLifecycle;
import ru.tinkoff.kora.common.Component;

@Component
public class TestSecondComponent implements MockLifecycle {

    private final TestFirstComponent firstComponent;

    public TestSecondComponent(TestFirstComponent firstComponent) {
        this.firstComponent = firstComponent;
    }

    public String get() {
        return firstComponent.get();
    }
}
