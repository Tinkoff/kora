package ru.tinkoff.kora.kafka.common.annotation;

import ru.tinkoff.kora.kafka.common.producer.ProducerConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface KafkaProducer {
    /**
     * @return config path
     * @see ProducerConfig
     */
    String value();
}
