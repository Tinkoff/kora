package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponentToMock;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponentWithMock;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {SimpleComponentWithMock.class},
    initializeMode = KoraAppTest.InitializeMode.PER_METHOD)
public class MockComponentGraphJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @TestComponent
    private SimpleComponentToMock mock;
    @TestComponent
    private SimpleComponentWithMock withMock;

    @Override
    public @NotNull KoraGraphModification graph() {
        return new KoraGraphModification()
            .mockComponent(SimpleComponentToMock.class);
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
