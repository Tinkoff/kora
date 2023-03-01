package ru.tinkoff.kora.test.extension.junit5.testdata;

import ru.tinkoff.kora.annotation.processor.common.MockLifecycle;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.Tag;

@Component
public final class ReplaceComponentWithTag3 implements MockLifecycle {

    private final ReplaceComponent replace1;
    private final ReplaceComponent replace2;

    public ReplaceComponentWithTag3(ReplaceComponent replace1,
                                    @Tag(ReplaceComponent.class) ReplaceComponent replace2) {
        this.replace1 = replace1;
        this.replace2 = replace2;
    }

    public String get() {
        return replace1.get() + replace2.get() + "3";
    }
}
