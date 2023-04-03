package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent2;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent23;

import java.util.List;

@KoraAppTest(
    application = LifecycleApplication.class,
    components = {LifecycleComponent2.class})
public class AddComponentWithBeanJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .addComponent(
                LifecycleComponent.class, List.of(LifecycleComponent23.class),
                g -> {
                    final LifecycleComponent2 simpleComponent2 = g.getFirst(LifecycleComponent2.class, LifecycleComponent.class);
                    return (LifecycleComponent) () -> "?" + simpleComponent2.get();
                });
    }

    @Test
    void parameterOriginalInjected(@Tag(LifecycleComponent23.class) @TestComponent LifecycleComponent lifecycleComponent23) {
        assertEquals("?2", lifecycleComponent23.get());
    }
}
