package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent2;

@KoraAppTest(
    application = LifecycleApplication.class,
    components = {LifecycleComponent2.class})
public class AddComponentJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @TestComponent
    private LifecycleComponent2 lifecycleComponent2;
    @TestComponent
    private LifecycleComponent added;

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .addComponent(LifecycleComponent.class, () -> (LifecycleComponent) () -> "?");
    }

    @Test
    void fieldOriginalInjected() {
        assertEquals("2", lifecycleComponent2.get());
    }

    @Test
    void fieldAddedAndOriginalInjected() {
        assertEquals("2", lifecycleComponent2.get());
        assertEquals("?", added.get());
    }
}
