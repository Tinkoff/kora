package ru.tinkoff.kora.cache.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface Cache {

    /**
     * @return path for cache config (cache name)
     */
    String value();
}
