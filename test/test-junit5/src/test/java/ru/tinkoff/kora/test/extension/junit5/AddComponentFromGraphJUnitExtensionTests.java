package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KoraAppTest(
    application = SimpleApplication.class,
    components = { SimpleComponent1.class })
public class AddComponentFromGraphJUnitExtensionTests extends Assertions implements KoraAppTestGraphModifier {

    @Override
    public @NotNull KoraGraphModifier graph() {
        return new KoraGraphModifier()
            .addComponent(g -> new SimpleComponent2(g.get(SimpleComponent1.class)));
    }

    @Test
    void singleComponentInjected(SimpleComponent1 firstComponent) {
        assertEquals("1", firstComponent.get());
    }

    @Test
    void twoComponentsInjected(SimpleComponent1 firstComponent, SimpleComponent2 secondComponent) {
        assertEquals("1", firstComponent.get());
        assertEquals("12", secondComponent.get());
    }
}
