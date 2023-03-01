package ru.tinkoff.kora.test.extension.junit5.testdata;

public class MockComponentWithTag2 implements MockComponent {

    @Override
    public String get() {
        return "2";
    }
}
