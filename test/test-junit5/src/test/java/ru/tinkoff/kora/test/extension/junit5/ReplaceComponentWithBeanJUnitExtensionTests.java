package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceComponent3;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent1;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleReplaceApplication;

@KoraAppTest(
    application = SimpleReplaceApplication.class,
    components = {SimpleComponent1.class})
public class ReplaceComponentWithBeanJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @Override
    public @NotNull KoraGraphModification graph() {
        return new KoraGraphModification()
            .replaceComponent(g -> new ReplaceComponent3(g.get(SimpleComponent1.class)), ReplaceComponent.class);
    }

    @Test
    void singleComponentInjected(@TestComponent SimpleComponent1 firstComponent) {
        assertEquals("1", firstComponent.get());
    }

    @Test
    void twoComponentsInjected(@TestComponent SimpleComponent1 firstComponent,
                               @TestComponent ReplaceComponent replaceComponent) {
        assertEquals("1", firstComponent.get());
        assertEquals("13", replaceComponent.get());
    }
}
