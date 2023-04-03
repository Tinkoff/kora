package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.testdata.Component1;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent12;

import java.util.List;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {SimpleComponent12.class})
public class MockComponentAnyTagViaAdditionJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .addComponent(SimpleComponent.class, List.of(SimpleComponent.class), () -> {
                var mock = Mockito.mock(SimpleComponent.class);
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
}
