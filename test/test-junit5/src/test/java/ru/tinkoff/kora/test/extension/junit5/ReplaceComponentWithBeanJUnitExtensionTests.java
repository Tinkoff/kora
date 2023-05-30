package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.Component1;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.LifecycleComponent;

@KoraAppTest(
    application = LifecycleApplication.class,
    components = {LifecycleComponent.class, Component1.class})
public class ReplaceComponentWithBeanJUnitExtensionTests extends Assertions implements KoraAppTestGraphModifier {

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.create()
            .replaceComponent(LifecycleComponent.class, g -> {
                final Component1 first = g.getFirst(Component1.class);
                return (LifecycleComponent) () -> "?" + first.get();
            });
    }

    @Test
    void replacedWithBeanFromGraphInjected(@TestComponent Component1 component1,
                                           @TestComponent LifecycleComponent replace12) {
        assertEquals("1", component1.get());
        assertEquals("?1", replace12.get());
    }
}
