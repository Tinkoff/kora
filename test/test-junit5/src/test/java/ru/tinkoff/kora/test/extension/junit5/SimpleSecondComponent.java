package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.annotation.processor.common.MockLifecycle;
import ru.tinkoff.kora.common.Component;

@Component
public class SimpleSecondComponent {

    private final SimpleFirstComponent firstComponent;

    public SimpleSecondComponent(SimpleFirstComponent firstComponent) {
        this.firstComponent = firstComponent;
    }

    public String get() {
        return firstComponent.get();
    }
}
