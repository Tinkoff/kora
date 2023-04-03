package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent2;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent23;

import java.util.List;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {SimpleComponent23.class, SimpleComponent2.class})
public class MockComponentAnyTagViaReplaceJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .replaceComponent(SimpleComponent2.class, List.of(SimpleComponent.class), () -> {
                var mock = Mockito.mock(SimpleComponent2.class);
                Mockito.when(mock.get()).thenReturn("?");
                Mockito.when(mock.init()).thenReturn(Mono.empty());
                Mockito.when(mock.release()).thenReturn(Mono.empty());
                return mock;
            });
    }

    @Test
    void mockWithTag2(@Tag(SimpleComponent.class) @TestComponent SimpleComponent component) {
        assertEquals("?", component.get());
    }

    @Test
    void beanWithMocks(@TestComponent SimpleComponent23 simpleComponent23) {
        assertEquals("?3", simpleComponent23.get());
    }
}
