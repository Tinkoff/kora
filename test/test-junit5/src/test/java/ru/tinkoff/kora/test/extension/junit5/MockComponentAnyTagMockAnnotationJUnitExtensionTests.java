package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.testdata.Component1;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent23;

import java.util.List;

@KoraAppTest(application = LifecycleApplication.class)
public class MockComponentAnyTagMockAnnotationJUnitExtensionTests extends Assertions {

    @Test
    void mockWithTag1(@MockComponent Component1 component1) {
        assertNull(component1.get());
        Mockito.when(component1.get()).thenReturn("?");
        assertEquals("?", component1.get());
    }

    @Test
    void mockWithTag2(@Tag(LifecycleComponent.class) @MockComponent LifecycleComponent component) {
        assertNull(component.get());
        Mockito.when(component.get()).thenReturn("?");
        assertEquals("?", component.get());
    }

    @Test
    void beanWithMocks(@Tag(LifecycleComponent.class) @MockComponent LifecycleComponent component,
                       @TestComponent LifecycleComponent23 component23) {
        assertNull(component.get());
        Mockito.when(component.get()).thenReturn("?");
        assertEquals("?", component.get());

        assertEquals("?3", component23.get());
    }
}
