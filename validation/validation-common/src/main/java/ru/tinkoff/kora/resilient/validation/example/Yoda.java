package ru.tinkoff.kora.resilient.validation.example;

import org.jetbrains.annotations.Nullable;
import ru.tinkoff.kora.resilient.validation.annotation.NotEmpty;
import ru.tinkoff.kora.resilient.validation.annotation.Validated;

import java.util.List;

/**
 * Please add Description Here.
 */
@Validated
public record Yoda(@Nullable @NotEmpty String id, @NotEmpty List<Integer> codes, List<Baby> babies) { }
