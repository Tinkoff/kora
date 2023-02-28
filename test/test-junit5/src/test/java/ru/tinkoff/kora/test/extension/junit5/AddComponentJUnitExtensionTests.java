package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KoraAppTest(
    application = SimpleApplication.class,
    components = { SimpleComponent1.class })
public class AddComponentJUnitExtensionTests extends Assertions implements KoraAppTestGraphModifier {

    @Override
    public @NotNull KoraGraphModifier graph() {
        return new KoraGraphModifier()
            .addComponent(SimpleComponent3::new);
    }

    @Test
    void singleComponentInjected(SimpleComponent1 firstComponent) {
        assertEquals("1", firstComponent.get());
    }

    @Test
    void twoComponentsInjected(SimpleComponent1 firstComponent, SimpleComponent3 thirdComponent) {
        assertEquals("1", firstComponent.get());
        assertEquals("3", thirdComponent.get());
    }
}
