package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.Component1;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent12;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {Component1.class})
public class ReplaceComponentWithBeanJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .replaceComponent(SimpleComponent.class, g -> new SimpleComponent12(g.getFirst(Component1.class)));
    }

    @Test
    void replacedWithBeanFromGraphInjected(@TestComponent Component1 component1,
                                           @TestComponent SimpleComponent replace12) {
        assertEquals("1", component1.get());
        assertEquals("?2", replace12.get());
    }
}
