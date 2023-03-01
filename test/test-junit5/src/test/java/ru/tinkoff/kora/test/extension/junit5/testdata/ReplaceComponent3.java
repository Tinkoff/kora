package ru.tinkoff.kora.test.extension.junit5.testdata;

public class ReplaceComponent3 implements ReplaceComponent {

    private final ReplaceComponent2 replace2;

    public ReplaceComponent3(ReplaceComponent2 replace2) {
        this.replace2 = replace2;
    }

    @Override
    public String get() {
        return replace2.get() + "3";
    }
}
