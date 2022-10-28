package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.ValidationContext.Path;

/**
 * Please add Description Here.
 */
public interface Violation {

    @NotNull
    String message();

    Path path();
}
