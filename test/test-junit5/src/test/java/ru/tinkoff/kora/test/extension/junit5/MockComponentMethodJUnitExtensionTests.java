package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tinkoff.kora.test.extension.junit5.testdata.Component1;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent12;

@KoraAppTest(
    application = LifecycleApplication.class,
    components = {LifecycleComponent12.class})
public class MockComponentMethodJUnitExtensionTests extends Assertions {

    @MockComponent
    private Component1 mocked;
    @TestComponent
    private LifecycleComponent12 lifecycleComponent12;

    @Test
    void mockInjected() {
        assertNull(mocked.get());
        Mockito.when(mocked.get()).thenReturn("?");
        assertEquals("?", mocked.get());
    }

    @Test
    void mockAndComponentWithMockInjected() {
        assertNull(mocked.get());
        Mockito.when(mocked.get()).thenReturn("?");
        assertEquals("?", mocked.get());
        assertEquals("?2", lifecycleComponent12.get());
    }
}
