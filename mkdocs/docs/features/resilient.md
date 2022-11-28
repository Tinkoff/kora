## Resilient

Предоставляет модули позволяющие использовать такие компоненты как: *CircuitBreaker, Fallback*.

### CircuitBreaker

CircuitBreaker – это прокси, который контролирует поток к запросам к конкретному методу, 
может находиться в нескольких состояниях в зависимости от поведения (OPEN, CLOSED, HALF_OPEN).

#### Dependency

```groovy
implementation 'ru.tinkoff.kora:resilient-circuitbreaker'
```

##### Конфигурация

Существует *default* конфигурация, которая применяется к CircuitBreaker при создании 
и затем применяются именованные настройки конкретного CircuitBreaker для переопределения дефолтов.

Можно изменить дефолтные настройки для всех CircuitBreaker одновременно изменив *default* конфигурацию.

Конфигурация *default* CircuitBreaker'а:
```hocon
resilient {
  circuitbreaker { 
    default {
      failureRateThreshold = 50
      waitDurationInOpenState = 25s
      permittedCallsInHalfOpenState = 10
      slidingWindowSize = 100L
      minimumRequiredCalls = 50L
      failurePredicateName = "ru.tinkoff.kora.resilient.circuitbreaker.fast.FastCircuitBreakerFailurePredicate"
    }
  }
}
```

Пример переопределения именованных настроек для определенного CircuitBreaker'а:
```hocon
resilient {
  circuitbreaker { 
    custom {
      waitDurationInOpenState = 1s
    }
  }
}
```

##### Exception Predicate

Для регистрации какие ошибки следует записывать как ошибки со стороны CircuitBreaker, можно переопределить дефолтный *CircuitBreakerFailurePredicate*, 
требуется имплементировать и зарегистрировать свой Bean в контексте и указать в конфигурации CircuitBreaker его имя.

```java
public final class SimpleCircuitBreakerFailurePredicate implements CircuitBreakerFailurePredicate {

    @Override
    public boolean test(Throwable throwable) {
        return true;
    }
}
```

### Fallback

#### Конфигурация

Существует *default* конфигурация, которая применяется к Fallback при создании
и затем применяются именованные настройки конкретного Fallback для переопределения дефолтов.

Можно изменить дефолтные настройки для всех Fallback одновременно изменив *default* конфигурацию.

Конфигурация *default* Fallback'а:
```hocon
resilient {
  fallback { 
    default {
      failurePredicateName = "MyCustomPredicate"
    }
  }
}
```

##### Exception Predicate

Пример имплементации:
```java
final class myFallbackFailurePredicate implements FallbackFailurePredicate {

    @NotNull
    @Override
    public String name() {
        return "MyCustomPredicate";
    }

    @Override
    public boolean test(@NotNull Throwable throwable) {
        return true;
    }
}
```
