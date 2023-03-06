package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceComponent2;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceComponent3;

@KoraAppTest(
    application = ReplaceApplication.class,
    components = {ReplaceComponent2.class})
public class ReplaceComponentWithBeanJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .replaceComponent(g -> new ReplaceComponent3(g.getFirst(ReplaceComponent2.class)), ReplaceComponent.class);
    }

    @Test
    void replacedWithBeanFromGraphInjected(@TestComponent ReplaceComponent2 replace2,
                                           @TestComponent ReplaceComponent replace3) {
        assertEquals("2", replace2.get());
        assertEquals("23", replace3.get());
    }
}
