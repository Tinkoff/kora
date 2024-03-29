package ru.tinkoff.kora.kafka.common.annotation;

import ru.tinkoff.kora.kafka.common.config.KafkaConsumerConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method level annotation used to specify which topic method should be subscribed to by Kafka Consumer.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface KafkaListener {

    /**
     * @return config path
     * @see KafkaConsumerConfig
     */
    String value();
}
