package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.Component;

@Component
public final class TestComponent12 implements LifecycleComponent {

    private final TestComponent1 component1;

    public TestComponent12(TestComponent1 component1) {
        this.component1 = component1;
    }

    public String get() {
        return component1.get() + "2";
    }
}
