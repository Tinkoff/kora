package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.Component;

@Component
public final class SimpleComponent12 implements SimpleComponent {

    private final Component1 component1;

    public SimpleComponent12(Component1 component1) {
        this.component1 = component1;
    }

    public String get() {
        return component1.get() + "2";
    }
}
