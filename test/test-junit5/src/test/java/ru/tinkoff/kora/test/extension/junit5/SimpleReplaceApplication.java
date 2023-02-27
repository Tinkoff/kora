package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.KoraApp;

@KoraApp
public interface SimpleReplaceApplication {

    default ReplaceComponent replaceComponent() {
        return new ReplaceComponent1();
    }
}
