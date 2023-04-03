package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent2;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {SimpleComponent2.class})
public class AddComponentJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @TestComponent
    private SimpleComponent2 simpleComponent2;
    @TestComponent
    private SimpleComponent simpleComponent;

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .addComponent(SimpleComponent.class, () -> (SimpleComponent) () -> "?");
    }

    @Test
    void fieldOriginalInjected() {
        assertEquals("2", simpleComponent2.get());
    }

    @Test
    void fieldAddedAndOriginalInjected() {
        assertEquals("2", simpleComponent2.get());
        assertEquals("?", simpleComponent.get());
    }
}
