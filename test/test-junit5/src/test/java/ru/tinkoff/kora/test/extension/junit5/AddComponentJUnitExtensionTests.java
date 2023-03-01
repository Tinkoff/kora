package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent1;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent3;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {SimpleComponent1.class})
public class AddComponentJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @TestComponent
    private SimpleComponent1 firstComponent;
    @TestComponent
    private SimpleComponent3 thirdComponent;

    @Override
    public @NotNull KoraGraphModification graph() {
        return new KoraGraphModification()
            .addComponent(SimpleComponent3::new, SimpleComponent3.class);
    }

    @Test
    void singleComponentInjected() {
        assertEquals("1", firstComponent.get());
    }

    @Test
    void twoComponentsInjected() {
        assertEquals("1", firstComponent.get());
        assertEquals("3", thirdComponent.get());
    }
}
