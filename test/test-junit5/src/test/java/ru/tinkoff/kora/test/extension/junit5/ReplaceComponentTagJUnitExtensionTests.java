package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent2;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent23;

import java.util.List;

@KoraAppTest(
    application = LifecycleApplication.class,
    components = {LifecycleComponent2.class, LifecycleComponent23.class})
public class ReplaceComponentTagJUnitExtensionTests extends Assertions implements KoraAppTestGraphModifier {

    @TestComponent
    private LifecycleComponent23 lifecycleComponent23;

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.create()
            .replaceComponent(LifecycleComponent2.class, List.of(LifecycleComponent.class), () -> (LifecycleComponent) () -> "?");
    }

    @Test
    void componentWithReplacementInjected() {
        assertEquals("?3", lifecycleComponent23.get());
    }
}
