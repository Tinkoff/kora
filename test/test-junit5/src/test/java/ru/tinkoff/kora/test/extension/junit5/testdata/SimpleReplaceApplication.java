package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceComponent1;

@KoraApp
public interface SimpleReplaceApplication {

    default ReplaceComponent replaceComponent() {
        return new ReplaceComponent1();
    }
}
