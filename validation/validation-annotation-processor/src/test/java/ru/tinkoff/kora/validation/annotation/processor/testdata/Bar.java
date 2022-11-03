package ru.tinkoff.kora.validation.annotation.processor.testdata;

import org.jetbrains.annotations.Nullable;
import ru.tinkoff.kora.validation.annotation.NotEmpty;
import ru.tinkoff.kora.validation.annotation.Size;
import ru.tinkoff.kora.validation.annotation.Validated;

import java.util.List;

@Validated
public class Bar {
    @Nullable
    @NotEmpty
    private String id;
    @Size(min = 1, max = 5)
    private List<Integer> codes;
    @Validated
    private List<Foo> foos;

    @Nullable
    public String getId() {
        return id;
    }

    public Bar setId(@Nullable String id) {
        this.id = id;
        return this;
    }

    public List<Integer> getCodes() {
        return codes;
    }

    public Bar setCodes(List<Integer> codes) {
        this.codes = codes;
        return this;
    }

    public List<Foo> getFoos() {
        return foos;
    }

    public Bar setFoos(List<Foo> foos) {
        this.foos = foos;
        return this;
    }
}
