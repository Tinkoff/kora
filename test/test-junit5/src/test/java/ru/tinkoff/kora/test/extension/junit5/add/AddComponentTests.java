package ru.tinkoff.kora.test.extension.junit5.add;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTest;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTestGraphModifier;
import ru.tinkoff.kora.test.extension.junit5.KoraGraphModification;
import ru.tinkoff.kora.test.extension.junit5.TestComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestComponent2;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KoraAppTest(
    value = TestApplication.class,
    components = {TestComponent2.class})
public class AddComponentTests implements KoraAppTestGraphModifier {

    @TestComponent
    private TestComponent2 original;
    @TestComponent
    private LifecycleComponent added;

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.create()
            .addComponent(LifecycleComponent.class, () -> (LifecycleComponent) () -> "?");
    }

    @Test
    void addedBean() {
        assertEquals("2", original.get());
        assertEquals("?", added.get());
    }
}
