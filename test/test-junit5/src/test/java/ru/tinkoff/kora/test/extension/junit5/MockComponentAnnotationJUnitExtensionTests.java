package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tinkoff.kora.test.extension.junit5.testdata.Component1;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent12;

@KoraAppTest(
    application = LifecycleApplication.class,
    components = {LifecycleComponent12.class, Component1.class},
    initializeMode = KoraAppTest.InitializeMode.PER_METHOD)
public class MockComponentAnnotationJUnitExtensionTests extends Assertions {

    @Test
    void singleComponentInjected(@MockComponent Component1 component1) {
        assertNull(component1.get());
        Mockito.when(component1.get()).thenReturn("?");
        assertEquals("?", component1.get());
    }

    @Test
    void twoComponentsInjected(@MockComponent Component1 component1,
                               @TestComponent LifecycleComponent12 component12) {
        assertNull(component1.get());
        Mockito.when(component1.get()).thenReturn("?");
        assertEquals("?", component1.get());
        assertEquals("?2", component12.get());
    }
}
