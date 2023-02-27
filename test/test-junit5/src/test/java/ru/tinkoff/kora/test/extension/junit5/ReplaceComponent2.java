package ru.tinkoff.kora.test.extension.junit5;

import ru.tinkoff.kora.common.Component;

public class ReplaceComponent2 implements ReplaceComponent {

    @Override
    public String get() {
        return "2";
    }
}
