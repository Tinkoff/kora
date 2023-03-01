package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tinkoff.kora.test.extension.junit5.testdata.MockComponentToMock;
import ru.tinkoff.kora.test.extension.junit5.testdata.MockComponentWithMock;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {MockComponentWithMock.class},
    initializeMode = KoraAppTest.InitializeMode.PER_METHOD)
public class MockComponentGraphJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @TestComponent
    private MockComponentToMock mock;
    @TestComponent
    private MockComponentWithMock withMock;

    @Override
    public @NotNull KoraGraphModification graph() {
        return new KoraGraphModification()
            .mockComponent(MockComponentToMock.class);
    }

    @Test
    void singleComponentInjected() {
        assertNull(mock.get());
        Mockito.when(mock.get()).thenReturn("1");
        assertEquals("1", mock.get());
    }

    @Test
    void twoComponentsInjected() {
        assertNull(mock.get());
        Mockito.when(mock.get()).thenReturn("1");
        assertEquals("1", mock.get());
        assertEquals("12", withMock.get());
    }
}
