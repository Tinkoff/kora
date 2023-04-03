package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent2;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent23;

import java.util.List;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {SimpleComponent2.class})
public class AddComponentWithBeanJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .addComponent(
                SimpleComponent.class, List.of(SimpleComponent23.class),
                g -> {
                    final SimpleComponent2 simpleComponent2 = g.getFirst(SimpleComponent2.class, SimpleComponent.class);
                    return (SimpleComponent) () -> "?" + simpleComponent2.get();
                });
    }

    @Test
    void parameterOriginalInjected(@Tag(SimpleComponent23.class) @TestComponent SimpleComponent simpleComponent23) {
        assertEquals("?23", simpleComponent23.get());
    }
}
