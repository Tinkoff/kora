package ru.tinkoff.kora.validation.annotation.processor.testdata;

import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.validation.common.annotation.*;

import javax.annotation.Nullable;

@Component
public class ValidateSync {

    @ValidateInput
    public int validatedInput(@Size(min = 1, max = 5) int c1,
                              @NotEmpty String c2,
                              @Valid @Nullable ValidTaz c3) {
        return c1;
    }

    @ValidateInput
    public void validatedInputVoid(@Size(min = 1, max = 5) int c1,
                                   @NotEmpty String c2,
                                   @Valid @Nullable ValidTaz c3) {

    }

    @Nullable
    @Valid
    @ValidateOutput
    public ValidTaz validatedOutput(@Nullable ValidTaz c3) {
        return c3;
    }
}
