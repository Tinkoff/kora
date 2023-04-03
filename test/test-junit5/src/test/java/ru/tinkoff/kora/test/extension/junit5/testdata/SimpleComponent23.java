package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.Tag;

@Component
public final class SimpleComponent23 implements SimpleComponent{

    private final SimpleComponent simpleComponent;

    public SimpleComponent23(@Tag(SimpleComponent.class) SimpleComponent simpleComponent) {
        this.simpleComponent = simpleComponent;
    }

    public String get() {
        return simpleComponent.get() + "3";
    }
}
