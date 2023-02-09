package ru.tinkoff.kora.validation.annotation.processor.testdata;

import reactor.core.publisher.Mono;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.validation.common.annotation.*;

import javax.annotation.Nullable;

@Component
public class ValidateMono {

    @ValidateInput
    public Mono<Integer> validatedInput(@Size(min = 1, max = 5) int c1,
                                        @NotEmpty String c2,
                                        @Valid @Nullable ValidTaz c3) {
        return Mono.just(c1);
    }

    @Valid
    @ValidateOutput
    public Mono<ValidTaz> validatedOutput(@Nullable ValidTaz c3) {
        return (c3 == null) ? Mono.empty() : Mono.just(c3);
    }
}
