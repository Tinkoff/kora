package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.ValidationContext.Path;

public interface Violation {

    @NotNull
    String message();

    @NotNull
    Path path();
}
