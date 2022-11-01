package ru.tinkoff.kora.validation.annotation.processor.testdata;

import org.jetbrains.annotations.Nullable;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.validation.annotation.NotEmpty;
import ru.tinkoff.kora.validation.annotation.Validated;

import java.util.List;
import java.util.Queue;

@Validated
public record Yoda(@Nullable @NotEmpty String id,
                   @NotEmpty List<Integer> codes,
                   @Validated List<Baby> babies) {}
