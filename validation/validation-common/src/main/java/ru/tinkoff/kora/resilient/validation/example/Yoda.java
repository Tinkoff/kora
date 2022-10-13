package ru.tinkoff.kora.resilient.validation.example;

import ru.tinkoff.kora.resilient.validation.annotation.NotEmpty;
import ru.tinkoff.kora.resilient.validation.annotation.NotNull;
import ru.tinkoff.kora.resilient.validation.annotation.Validated;

import java.util.List;

/**
 * Please add Description Here.
 */
@Validated
public record Yoda(@NotNull @NotEmpty String id, @NotEmpty List<Integer> codes, List<Baby> babies) { }
