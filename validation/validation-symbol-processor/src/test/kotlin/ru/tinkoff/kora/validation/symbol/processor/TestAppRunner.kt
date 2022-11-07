package ru.tinkoff.kora.validation.symbol.processor

import com.google.devtools.ksp.KspExperimental
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import org.junit.jupiter.api.Assertions
import ru.tinkoff.kora.application.graph.TypeRef
import ru.tinkoff.kora.ksp.common.symbolProcess
import ru.tinkoff.kora.validation.Validator
import ru.tinkoff.kora.validation.constraint.ValidationModule
import ru.tinkoff.kora.validation.symbol.processor.testdata.Bar
import ru.tinkoff.kora.validation.symbol.processor.testdata.Foo
import ru.tinkoff.kora.validation.symbol.processor.testdata.Taz

@KspExperimental
@KotlinPoetKspPreview
open class TestAppRunner : Assertions(), ValidationModule {

    companion object {
        private var classLoader: ClassLoader? = null
    }

    protected open fun getFooValidator(): Validator<Foo> {
        val classLoader = getClassLoader()
        val clazz = classLoader!!.loadClass("ru.tinkoff.kora.validation.symbol.processor.testdata.\$Validator_Foo")
        return clazz.constructors[0].newInstance(
            patternStringConstraintFactory(),
            rangeLongConstraintFactory(),
            notEmptyStringConstraintFactory(),
            getBarValidator()
        ) as Validator<Foo>
    }

    protected open fun getBarValidator(): Validator<Bar> {
        val classLoader = getClassLoader()
        val clazz = classLoader!!.loadClass("ru.tinkoff.kora.validation.symbol.processor.testdata.\$Validator_Bar")
        return clazz.constructors[0].newInstance(
            sizeListConstraintFactory(TypeRef.of(Int::class.java)),
            notEmptyStringConstraintFactory(),
            listValidator(getTazValidator(), TypeRef.of(Taz::class.java))
        ) as Validator<Bar>
    }

    protected open fun getTazValidator(): Validator<Taz> {
        val classLoader = getClassLoader()
        val clazz = classLoader!!.loadClass("ru.tinkoff.kora.validation.symbol.processor.testdata.\$Validator_Taz")
        return clazz.constructors[0].newInstance(patternStringConstraintFactory()) as Validator<Taz>
    }

    private fun getClassLoader(): ClassLoader {
        return try {
            if (classLoader == null) {
                val classes = listOf(
                    Foo::class,
                    Bar::class,
                    Taz::class
                )
                classLoader = symbolProcess(classes, ValidationSymbolProcessorProvider())
            }
            classLoader!!
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }
}
