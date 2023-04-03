package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.Component;

@Component
public final class LifecycleComponent12 implements LifecycleComponent {

    private final Component1 component1;

    public LifecycleComponent12(Component1 component1) {
        this.component1 = component1;
    }

    public String get() {
        return component1.get() + "2";
    }
}
