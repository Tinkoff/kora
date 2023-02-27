package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.common.Component;

@Component
public class SimpleComponent2 {

    private final SimpleComponent1 firstComponent;

    public SimpleComponent2(SimpleComponent1 firstComponent) {
        this.firstComponent = firstComponent;
    }

    public String get() {
        return firstComponent.get() + "2";
    }
}
