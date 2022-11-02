package ru.tinkoff.kora.validation.symbol.processor.testdata

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import ru.tinkoff.kora.common.KoraApp
import ru.tinkoff.kora.config.common.ConfigModule
import ru.tinkoff.kora.validation.Validator
import ru.tinkoff.kora.validation.constraint.ValidationModule

@KoraApp
interface AppWithConfig : ValidationModule, ConfigModule {

    override fun config(): Config {
        return ConfigFactory.parseString(
            """
            """.trimIndent()
        ).resolve()
    }

    fun lifecycle(
        babyValidator: Validator<Baby>,
        yodaValidator: Validator<Yoda>
    ): ValidationLifecycle {
        return ValidationLifecycle(babyValidator, yodaValidator)
    }
}
