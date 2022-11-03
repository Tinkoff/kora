package ru.tinkoff.kora.validation;

import org.jetbrains.annotations.NotNull;
import ru.tinkoff.kora.validation.ValidationContext.Path;

/**
 * Indicates validation failure
 */
public interface Violation {

    /**
     * @return failure message
     */
    @NotNull
    String message();

    /**
     * @return path for value where failure occurred
     */
    @NotNull
    Path path();
}
