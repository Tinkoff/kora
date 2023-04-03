package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.KoraApp;

@KoraApp
public interface LifecycleApplication {

    default LifecycleComponent lifecycleComponent1() {
        return () -> "1";
    }
}
