package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent2;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent23;

import java.util.List;

@KoraAppTest(
    application = LifecycleApplication.class,
    components = {LifecycleComponent23.class, LifecycleComponent2.class})
public class MockComponentAnyTagViaReplaceJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .replaceComponent(LifecycleComponent2.class, List.of(LifecycleComponent.class), () -> {
                var mock = Mockito.mock(LifecycleComponent2.class);
                Mockito.when(mock.get()).thenReturn("?");
                Mockito.when(mock.init()).thenReturn(Mono.empty());
                Mockito.when(mock.release()).thenReturn(Mono.empty());
                return mock;
            });
    }

    @Test
    void mockWithTag2(@Tag(LifecycleComponent.class) @TestComponent LifecycleComponent component) {
        assertEquals("?", component.get());
    }

    @Test
    void beanWithMocks(@TestComponent LifecycleComponent23 simpleComponent23) {
        assertEquals("?3", simpleComponent23.get());
    }
}
