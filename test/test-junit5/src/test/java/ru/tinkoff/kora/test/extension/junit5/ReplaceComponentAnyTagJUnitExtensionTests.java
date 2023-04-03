package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent12;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent2;

import java.util.List;

@KoraAppTest(
    application = LifecycleApplication.class,
    components = {LifecycleComponent12.class, LifecycleComponent2.class})
public class ReplaceComponentAnyTagJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @TestComponent
    private LifecycleComponent replaced;

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .replaceComponent(LifecycleComponent.class, List.of(Tag.Any.class), () -> (LifecycleComponent) () -> "?");
    }

    @Test
    void replacedInjected() {
        assertEquals("?", replaced.get());
    }
}
