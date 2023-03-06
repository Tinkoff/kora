package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceComponent2;

@KoraAppTest(
    application = ReplaceApplication.class,
    components = {ReplaceComponent2.class})
public class ReplaceComponentJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @TestComponent
    private ReplaceComponent replace1;
    @TestComponent
    private ReplaceComponent2 replace2;

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .replaceComponent(() -> (ReplaceComponent) () -> "?", ReplaceComponent.class);
    }

    @Test
    void replacedAndOriginalInjected() {
        assertEquals("?", replace1.get());
        assertEquals("2", replace2.get());
    }
}
