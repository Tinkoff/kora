package ru.tinkoff.kora.validation.annotation.processor.testdata;

import org.jetbrains.annotations.Nullable;
import ru.tinkoff.kora.validation.annotation.Size;
import ru.tinkoff.kora.validation.annotation.Validated;

import java.util.List;

@Validated
public class Bar {
    @Nullable
    private String id;
    @Size(min = 1, max = 5)
    private List<Integer> codes;
    @Validated
    @Nullable
    private List<Taz> tazs;

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

    public List<Taz> getTazs() {
        return tazs;
    }

    public Bar setTazs(List<Taz> tazs) {
        this.tazs = tazs;
        return this;
    }
}
