package ru.tinkoff.kora.test.extension.junit5.mock;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTest;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTestGraphModifier;
import ru.tinkoff.kora.test.extension.junit5.KoraGraphModification;
import ru.tinkoff.kora.test.extension.junit5.TestComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestComponent1;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestComponent23;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@KoraAppTest(TestApplication.class)
public class MockGraphModificationTests implements KoraAppTestGraphModifier {

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.create()
            .mockComponent(TestComponent1.class)
            .mockComponent(LifecycleComponent.class, List.of(LifecycleComponent.class));
    }

    @Test
    void mockFromGraph(@TestComponent TestComponent1 component1) {
        assertNull(component1.get());
        Mockito.when(component1.get()).thenReturn("?");
        assertEquals("?", component1.get());
    }

    @Test
    void mockFromGraphWithTag(@Tag(LifecycleComponent.class) @TestComponent LifecycleComponent component) {
        assertNull(component.get());
        Mockito.when(component.get()).thenReturn("?");
        assertEquals("?", component.get());
    }

    @Test
    void mockBeanDependency(@Tag(LifecycleComponent.class) @TestComponent LifecycleComponent component,
                            @TestComponent TestComponent23 component23) {
        assertNull(component.get());
        Mockito.when(component.get()).thenReturn("?");
        assertEquals("?", component.get());

        assertEquals("?3", component23.get());
    }
}
