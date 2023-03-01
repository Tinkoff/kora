package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tinkoff.kora.test.extension.junit5.testdata.MockComponentToMock;
import ru.tinkoff.kora.test.extension.junit5.testdata.MockComponentWithMock;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {MockComponentWithMock.class},
    mocks = {MockComponentToMock.class},
    initializeMode = KoraAppTest.InitializeMode.PER_METHOD)
public class MockComponentAnnotationJUnitExtensionTests extends Assertions {

    @Test
    void singleComponentInjected(@TestComponent MockComponentToMock mock) {
        assertNull(mock.get());
        Mockito.when(mock.get()).thenReturn("1");
        assertEquals("1", mock.get());
    }

    @Test
    void twoComponentsInjected(@TestComponent MockComponentToMock mock,
                               @TestComponent MockComponentWithMock withMock) {
        assertNull(mock.get());
        Mockito.when(mock.get()).thenReturn("1");
        assertEquals("1", mock.get());
        assertEquals("12", withMock.get());
    }
}
