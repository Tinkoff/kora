package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {SimpleComponentWithMock.class},
    initializeMode = KoraAppTest.InitializeMode.PER_METHOD)
public class AddMockModifierJUnitExtensionTests extends Assertions implements KoraAppTestGraphModifier {

    @Override
    public @NotNull KoraGraphModifier graph() {
        return new KoraGraphModifier()
            .mockComponent(SimpleComponentToMock.class);
    }

    @Test
    void singleComponentInjected(SimpleComponentToMock mock) {
        assertNull(mock.get());
        Mockito.when(mock.get()).thenReturn("1");
        assertEquals("1", mock.get());
    }

    @Test
    void twoComponentsInjected(SimpleComponentToMock mock, SimpleComponentWithMock withMock) {
        assertNull(mock.get());
        Mockito.when(mock.get()).thenReturn("1");
        assertEquals("1", mock.get());
        assertEquals("12", withMock.get());
    }
}
