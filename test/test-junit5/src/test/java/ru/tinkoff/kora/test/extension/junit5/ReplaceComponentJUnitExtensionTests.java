package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.Component1;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {Component1.class})
public class ReplaceComponentJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @TestComponent
    private Component1 replace1;

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .replaceComponent(SimpleComponent.class, () -> (SimpleComponent) () -> "?");
    }

    @Test
    void replacedAndOriginalInjected() {
        assertEquals("?", replace1.get());
    }
}
