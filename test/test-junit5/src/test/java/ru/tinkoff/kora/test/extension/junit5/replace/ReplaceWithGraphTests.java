package ru.tinkoff.kora.test.extension.junit5.replace;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTest;
import ru.tinkoff.kora.test.extension.junit5.KoraAppTestGraphModifier;
import ru.tinkoff.kora.test.extension.junit5.KoraGraphModification;
import ru.tinkoff.kora.test.extension.junit5.TestComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestComponent1;
import ru.tinkoff.kora.test.extension.junit5.testdata.TestApplication;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KoraAppTest(
    value = TestApplication.class,
    components = {LifecycleComponent.class, TestComponent1.class})
public class ReplaceWithGraphTests implements KoraAppTestGraphModifier {

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.create()
            .replaceComponent(LifecycleComponent.class, g -> {
                final TestComponent1 first = g.getFirst(TestComponent1.class);
                return (LifecycleComponent) () -> "?" + first.get();
            });
    }

    @Test
    void originalWithReplacedBean(@TestComponent TestComponent1 component1,
                                           @TestComponent LifecycleComponent replace12) {
        assertEquals("1", component1.get());
        assertEquals("?1", replace12.get());
    }
}
