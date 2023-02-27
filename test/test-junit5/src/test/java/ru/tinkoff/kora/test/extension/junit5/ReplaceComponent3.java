package ru.tinkoff.kora.test.extension.junit5;

public class ReplaceComponent3 implements ReplaceComponent {

    private final SimpleComponent1 simpleComponent1;

    public ReplaceComponent3(SimpleComponent1 simpleComponent1) {
        this.simpleComponent1 = simpleComponent1;
    }

    @Override
    public String get() {
        return simpleComponent1.get() + "3";
    }
}
