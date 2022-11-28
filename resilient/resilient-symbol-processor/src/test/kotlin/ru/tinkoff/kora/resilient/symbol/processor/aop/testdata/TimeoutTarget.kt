package ru.tinkoff.kora.resilient.symbol.processor.aop.testdata

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import ru.tinkoff.kora.annotation.processor.common.MockLifecycle
import ru.tinkoff.kora.common.Component
import ru.tinkoff.kora.resilient.circuitbreaker.annotation.CircuitBreaker
import ru.tinkoff.kora.resilient.timeout.TimeoutException
import ru.tinkoff.kora.resilient.timeout.Timeouter
import ru.tinkoff.kora.resilient.timeout.annotation.Timeout
import java.util.concurrent.atomic.AtomicLong

@Component
open class TimeoutTarget : MockLifecycle {

    @Timeout("custom1")
    open fun getValueSync(): String {
        Thread.sleep(2000)
        return "OK"
    }

    @Timeout("custom2")
    open suspend fun getValueSuspend(): String {
        delay(2000)
        return "OK"
    }

    @Timeout("custom3")
    open fun getValueFLow(): Flow<String> {
        return flow {
            delay(2000)
            emit("OK")
        }
    }
}
