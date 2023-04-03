package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.Tag;

@Tag(SimpleComponent.class)
@Component
public final class SimpleComponent2 implements SimpleComponent {

    public String get() {
        return "2";
    }
}
