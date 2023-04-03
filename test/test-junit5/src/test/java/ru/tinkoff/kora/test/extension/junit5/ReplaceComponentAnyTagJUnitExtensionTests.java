package ru.tinkoff.kora.test.extension.junit5;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleApplication;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent12;
import ru.tinkoff.kora.test.extension.junit5.testdata.SimpleComponent2;

import java.util.List;

@KoraAppTest(
    application = SimpleApplication.class,
    components = {SimpleComponent12.class, SimpleComponent2.class})
public class ReplaceComponentAnyTagJUnitExtensionTests extends Assertions implements KoraAppTestGraph {

    @TestComponent
    private SimpleComponent replace1;
    @Tag(SimpleComponent.class)
    @TestComponent
    private SimpleComponent component2;
    @TestComponent
    private SimpleComponent12 component12;

    @Override
    public @NotNull KoraGraphModification graph() {
        return KoraGraphModification.of()
            .replaceComponent(SimpleComponent.class, List.of(Tag.Any.class), () -> (SimpleComponent) () -> "?");
    }

    @Test
    void replacedInjected() {
        assertEquals("?", replace1.get());
        assertEquals("2", component2.get());
    }

    @Test
    void componentWithReplacementInjected() {
        assertEquals("?2", component12.get());
    }
}
