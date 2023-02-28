package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent1;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent2;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {SimpleComponent1.class})
public class AddComponentWithBeanJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @Override
    public @NotNull KoraGraphModification graph() {
        return new KoraGraphModification()
            .addComponent(g -> new SimpleComponent2(g.get(SimpleComponent1.class)), SimpleComponent2.class);
    }

    @Test
    void singleComponentInjected(@TestComponent SimpleComponent1 firstComponent) {
        assertEquals("1", firstComponent.get());
    }

    @Test
    void twoComponentsInjected(@TestComponent SimpleComponent1 firstComponent,
                               @TestComponent SimpleComponent2 secondComponent) {
        assertEquals("1", firstComponent.get());
        assertEquals("12", secondComponent.get());
    }
}