package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.annotation.processor.common.MockLifecycle;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.Tag;

@Component
public class MockComponentWithTag3 implements MockLifecycle {

    private final MockComponent mock1;
    private final MockComponent mock2;

    public MockComponentWithTag3(MockComponent mock1,
                                 @Tag(MockComponentWithTag2.class) MockComponent mock2) {
        this.mock1 = mock1;
        this.mock2 = mock2;
    }

    public String get() {
        return mock1.get() + mock2.get() + "3";
    }
}
