package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.testdata.*;

import java.util.List;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {SimpleComponent23.class},
    mocks = {Component1.class})
public class MockComponentAnyTagAnnotationJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .mockComponent(SimpleComponent.class, List.of(SimpleComponent.class));
    }

    @Test
    void mockWithTag1(@TestComponent Component1 component1) {
        assertNull(component1.get());
        Mockito.when(component1.get()).thenReturn("?");
        assertEquals("?", component1.get());
    }

    @Test
    void mockWithTag2(@Tag(SimpleComponent.class) @TestComponent SimpleComponent component) {
        assertNull(component.get());
        Mockito.when(component.get()).thenReturn("?");
        assertEquals("?", component.get());
    }

    @Test
    void beanWithMocks(@Tag(SimpleComponent.class) @TestComponent SimpleComponent component,
                       @TestComponent SimpleComponent23 component23) {
        assertNull(component.get());
        Mockito.when(component.get()).thenReturn("?");
        assertEquals("?", component.get());

        assertEquals("?3", component23.get());
    }
}
