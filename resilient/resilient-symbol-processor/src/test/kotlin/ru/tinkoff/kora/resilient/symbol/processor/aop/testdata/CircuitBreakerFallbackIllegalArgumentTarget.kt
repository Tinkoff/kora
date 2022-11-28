package ru.tinkoff.kora.resilient.symbol.processor.aop.testdata

import ru.tinkoff.kora.common.Component
import ru.tinkoff.kora.resilient.circuitbreaker.annotation.CircuitBreaker

@Component
open class CircuitBreakerFallbackIllegalArgumentTarget {

    companion object {
        const val VALUE = "OK"
        const val FALLBACK = "FALLBACK"
    }

    var alwaysFail = true

    @CircuitBreaker("custom_fallback1", fallbackMethod = "getFallbackSync(arg3)")
    open fun getValueSync(arg1: String, arg2: String): String {
        check(!alwaysFail) { "Failed" }
        return VALUE
    }

    protected fun getFallbackSync(arg1: String, arg2: String): String {
        return FALLBACK + arg1 + arg2
    }
}
