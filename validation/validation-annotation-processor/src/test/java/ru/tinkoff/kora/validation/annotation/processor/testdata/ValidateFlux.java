package ru.tinkoff.kora.validation.annotation.processor.testdata;

import reactor.core.publisher.Flux;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.validation.common.annotation.*;

import javax.annotation.Nullable;

@Component
public class ValidateFlux {

    @ValidateInput
    public Flux<Void> validatedInput(@Size(min = 1, max = 5) int c1,
                                     @NotEmpty String c2,
                                     @Valid @Nullable ValidTaz c3) {
        return Flux.empty();
    }

    @Valid
    @ValidateOutput
    public Flux<ValidTaz> validatedOutput(@Nullable ValidTaz c3) {
        return (c3 == null) ? Flux.empty() : Flux.just(c3);
    }
}
