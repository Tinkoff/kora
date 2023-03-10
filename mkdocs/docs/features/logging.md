# Логирование

Модуль который позволяет добавить поддержку структурированных логов, конфигурации логов в `hocon`.

### Dependency

```groovy
implementation "ru.tinkoff.kora:logging-logback"
implementation "ch.qos.logback:logback-classic:1.4.5"
```

### Modules

```java
@KoraApp
public interface ApplicationModules extends LoggingModule { }
```

### Description

Передать структурированные данные в запись лога можно двумя способами:

- Через Marker:

```java
var marker = StructuredArgument.marker("key", gen -> {
   gen.writeString("value");
});

log.info(marker, "message");
```


- Через параметры:

```java
var parameter = StructuredArgument.arg("key", gen -> {
 gen.writeString("value");
});
log.info("message", parameter);
```

Методы `marker` и `arg` также принимают в качестве шорткатов Long, Integer, String, Boolean и Map<String, String>.

Структурные данные можно прикреплять ко всем записям в рамках контекста с помощью класса `ru.tinkoff.kora.logging.common.MDC`:

```java
MDC.put("key",gen->gen.writeString("value"));
```

Если вы используете `AsyncAppender` для отправки логов, то для корректной передачи MDC параметров нужно воспользоваться `ru.tinkoff.kora.logging.logback.KoraAsyncAppender`,
который передаст делегату `ru.tinkoff.kora.logging.logback.KoraLoggingEvent`, содержащий, в том числе, структурный MDC.
