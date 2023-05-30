package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent12;

import java.util.List;

@KoraAppTest(
    application = LifecycleApplication.class,
    components = {LifecycleComponent12.class})
public class MockComponentAnyTagViaAdditionJUnitExtensionTests extends Assertions implements KoraAppTestGraphModifier {

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.create()
            .addComponent(LifecycleComponent.class, List.of(LifecycleComponent.class), () -> {
                var mock = Mockito.mock(LifecycleComponent.class);
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
}
