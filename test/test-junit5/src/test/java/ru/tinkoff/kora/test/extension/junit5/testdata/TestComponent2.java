package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.Tag;

@Tag(LifecycleComponent.class)
@Component
public final class TestComponent2 implements LifecycleComponent {

    public String get() {
        return "2";
    }
}
