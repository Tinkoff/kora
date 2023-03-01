package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.common.Tag;

@KoraApp
public interface MockApplicationWithTag {

    default MockComponent mock1() {
        return new MockComponentWithTag1();
    }

    @Tag(MockComponentWithTag2.class)
    default MockComponent mock2() {
        return new MockComponentWithTag2();
    }
}
