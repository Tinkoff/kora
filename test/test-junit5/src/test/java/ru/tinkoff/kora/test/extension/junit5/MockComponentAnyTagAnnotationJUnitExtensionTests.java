package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.testdata.MockApplicationWithTag;
import ru.tinkoff.kora.test.extension.junit5.testdata.MockComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.MockComponentWithTag2;
import ru.tinkoff.kora.test.extension.junit5.testdata.MockComponentWithTag3;

@KoraAppTest(
    application = MockApplicationWithTag.class,
    components = {MockComponentWithTag3.class},
    mocks = {MockComponent.class},
    initializeMode = KoraAppTest.InitializeMode.PER_METHOD)
public class MockComponentAnyTagAnnotationJUnitExtensionTests extends Assertions {

    @Test
    void mockWithTag1(@TestComponent MockComponent mockWithTag1) {
        assertNull(mockWithTag1.get());
        Mockito.when(mockWithTag1.get()).thenReturn("?");
        assertEquals("?", mockWithTag1.get());
    }

    @Test
    void mockWithTag2(@Tag(MockComponentWithTag2.class) @TestComponent MockComponent mockWithTag2) {
        assertNull(mockWithTag2.get());
        Mockito.when(mockWithTag2.get()).thenReturn("?");
        assertEquals("?", mockWithTag2.get());
    }

    @Test
    void beanWithMocks(@TestComponent MockComponent mock1,
                       @Tag(MockComponentWithTag2.class) @TestComponent MockComponent mock2,
                       @TestComponent MockComponentWithTag3 mock3) {
        assertNull(mock1.get());
        Mockito.when(mock1.get()).thenReturn("?");
        assertEquals("?", mock1.get());

        assertNull(mock2.get());
        Mockito.when(mock2.get()).thenReturn("?");
        assertEquals("?", mock2.get());

        assertEquals("??3", mock3.get());
    }
}
