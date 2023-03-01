package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.KoraApp;
import ru.tinkoff.kora.common.Tag;

@KoraApp
public interface ReplaceApplicationWithTag {

    default ReplaceComponent replaceWithTag1() {
        return () -> "1";
    }

    @Tag(ReplaceComponent.class)
    default ReplaceComponent replaceWithTag2() {
        return () -> "2";
    }
}
