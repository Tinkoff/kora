package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent;

@KoraAppTest(
    application = LifecycleApplication.class,
    components = {LifecycleComponent.class})
public class ReplaceComponentJUnitExtensionTests extends Assertions implements KoraAppTestGraphModifier {

    @TestComponent
    private LifecycleComponent replaced;

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.create()
            .replaceComponent(LifecycleComponent.class, () -> (LifecycleComponent) () -> "?");
    }

    @Test
    void replacedAndOriginalInjected() {
        assertEquals("?", replaced.get());
    }
}
