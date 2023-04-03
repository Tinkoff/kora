package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.Component1;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent12;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {SimpleComponent12.class})
public class MockComponentMethodJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @TestComponent
    private Component1 component1;
    @TestComponent
    private SimpleComponent12 component12;

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .mockComponent(Component1.class);
    }

    @Test
    void mockInjected() {
        assertNull(component1.get());
        Mockito.when(component1.get()).thenReturn("1");
        assertEquals("1", component1.get());
    }

    @Test
    void mockAndComponentWithMockInjected() {
        assertNull(component1.get());
        Mockito.when(component1.get()).thenReturn("1");
        assertEquals("1", component1.get());
        assertEquals("12", component12.get());
    }
}
