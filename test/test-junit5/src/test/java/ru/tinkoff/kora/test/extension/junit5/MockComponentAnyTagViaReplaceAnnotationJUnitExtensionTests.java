package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.testdata.MockApplicationWithTag;
import ru.tinkoff.kora.test.extension.junit5.testdata.MockComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.MockComponentWithTag2;
import ru.tinkoff.kora.test.extension.junit5.testdata.MockComponentWithTag3;

@KoraAppTest(
    application = MockApplicationWithTag.class,
    components = {MockComponentWithTag3.class},
    initializeMode = KoraAppTest.InitializeMode.PER_METHOD)
public class MockComponentAnyTagViaReplaceAnnotationJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .replaceComponent(() -> {
                var mock = Mockito.mock(MockComponent.class);
                Mockito.when(mock.get()).thenReturn("?");
                Mockito.when(mock.init()).thenReturn(Mono.empty());
                Mockito.when(mock.release()).thenReturn(Mono.empty());
                return mock;
            }, MockComponent.class, Tag.Any.class);
    }

    @Test
    void mockWithTag1(@TestComponent MockComponent mockWithTag1) {
        assertEquals("?", mockWithTag1.get());
    }

    @Test
    void mockWithTag2(@Tag(MockComponentWithTag2.class) @TestComponent MockComponent mockWithTag2) {
        assertEquals("?", mockWithTag2.get());
    }

    @Test
    void beanWithMocks(@TestComponent MockComponent mock1,
                       @Tag(MockComponentWithTag2.class) @TestComponent MockComponent mock2,
                       @TestComponent MockComponentWithTag3 mock3) {
        assertEquals("?", mock1.get());
        assertEquals("?", mock2.get());
        assertEquals("??3", mock3.get());
    }
}
