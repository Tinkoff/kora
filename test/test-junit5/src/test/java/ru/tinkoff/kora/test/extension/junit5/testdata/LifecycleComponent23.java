package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.Tag;

@Component
public final class LifecycleComponent23 implements LifecycleComponent {

    private final LifecycleComponent lifecycleComponent;

    public LifecycleComponent23(@Tag(LifecycleComponent.class) LifecycleComponent lifecycleComponent) {
        this.lifecycleComponent = lifecycleComponent;
    }

    public String get() {
        return lifecycleComponent.get() + "3";
    }
}
