package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KoraAppTest(
    application = SimpleReplaceApplication.class,
    components = { SimpleComponent1.class })
public class ReplaceComponentFromGraphJUnitExtensionTests extends Assertions implements KoraAppTestGraphModifier {

    @Override
    public @NotNull KoraGraphModifier graph() {
        return new KoraGraphModifier()
            .replaceComponent(g -> new ReplaceComponent3(g.get(SimpleComponent1.class)), ReplaceComponent.class);
    }

    @Test
    void singleComponentInjected(SimpleComponent1 firstComponent) {
        assertEquals("1", firstComponent.get());
    }

    @Test
    void twoComponentsInjected(SimpleComponent1 firstComponent, ReplaceComponent replaceComponent) {
        assertEquals("1", firstComponent.get());
        assertEquals("13", replaceComponent.get());
    }
}
