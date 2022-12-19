package ru.tinkoff.kora.test.extension.junit5;

import org.junit.jupiter.api.Test;

@KoraAppTest(application = TestApplication.class, classes = {TestFirstComponent.class, TestSecondComponent.class})
public class TestClass {

    @Test
    void test0() {
        System.out.println("TEST 2");
    }

    @Test
    void test1(TestFirstComponent firstComponent) {
        System.out.println("1 - " + firstComponent);
    }

    @Test
    void test2(TestFirstComponent firstComponent, TestSecondComponent secondComponent) {
        System.out.println("1 -" + firstComponent + ", 2 - " + secondComponent);
    }
}
