package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.KoraApp;

@KoraApp
public interface ReplaceApplication {

    default ReplaceComponent replaceComponent() {
        return new ReplaceComponent1();
    }
}
