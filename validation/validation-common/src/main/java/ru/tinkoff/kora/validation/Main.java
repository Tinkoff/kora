package ru.tinkoff.kora.validation;

import ru.tinkoff.kora.validation.example.Baby;
import ru.tinkoff.kora.validation.example.Yoda;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Please add Description Here.
 */
public class Main {

    public static void main(String[] args) {
        Yoda yodaInnerInnerNull = new Yoda("1", List.of(1),
            List.of(new Baby("1", 2L, OffsetDateTime.now(),
                new Yoda("2", List.of(2),
                    List.of(new Baby("1", 2L, OffsetDateTime.now(), null),
                        new Baby(null, 2L, null, null))))));
    }
}
