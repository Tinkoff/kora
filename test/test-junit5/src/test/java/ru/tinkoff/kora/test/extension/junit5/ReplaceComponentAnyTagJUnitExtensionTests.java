package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceApplicationWithTag;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.ReplaceComponentWithTag3;

@KoraAppTest(
    application = ReplaceApplicationWithTag.class,
    components = {ReplaceComponent.class, ReplaceComponentWithTag3.class})
public class ReplaceComponentAnyTagJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @TestComponent
    private ReplaceComponent replace1;
    @Tag(ReplaceComponent.class)
    @TestComponent
    private ReplaceComponent replace2;
    @TestComponent
    private ReplaceComponentWithTag3 replace3;

    @Override
    public @NotNull KoraGraphModification graph() {
        return new KoraGraphModification()
            .replaceComponent(() -> (ReplaceComponent) () -> "?", ReplaceComponent.class, Tag.Any.class);
    }

    @Test
    void injectReplaced() {
        assertEquals("?", replace1.get());
        assertEquals("?", replace2.get());
    }

    @Test
    void injectComponentsWithReplacements() {
        assertEquals("??3", replace3.get());
    }
}
