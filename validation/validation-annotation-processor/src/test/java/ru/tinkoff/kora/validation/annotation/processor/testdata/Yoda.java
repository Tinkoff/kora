package ru.tinkoff.kora.validation.annotation.processor.testdata;

import org.jetbrains.annotations.Nullable;
import ru.tinkoff.kora.validation.annotation.NotEmpty;
import ru.tinkoff.kora.validation.annotation.Validated;

import java.util.List;

@Validated
public record Yoda(@Nullable @NotEmpty String id,
                   @NotEmpty List<Integer> codes,
                   List<Baby> babies) {}
