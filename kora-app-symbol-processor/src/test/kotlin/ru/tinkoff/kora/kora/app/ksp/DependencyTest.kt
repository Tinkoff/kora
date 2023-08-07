package ru.tinkoff.kora.kora.app.ksp

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

open class DependencyTest : AbstractKoraAppProcessorTest() {
    @Test
    open fun testDiscoveredFinalClassDependency() {
        val draw = compile(
            """
            @KoraApp
            interface ExampleApplication {
                class TestClass1
                
                @Root
                fun test(testClass: TestClass1) = ""
            }
            """.trimIndent()
        )
        Assertions.assertThat(draw.nodes).hasSize(2)
        draw.init()
    }

    @Test
    open fun testDiscoveredFinalClassDependencyWithTag() {
        val draw = compile(
            """
            @KoraApp
            interface ExampleApplication {
                @Tag(TestClass1::class)
                class TestClass1
                
                @Root
                fun test(@Tag(TestClass1::class) testClass: TestClass1) = ""
            }
            """.trimIndent()
        )
        Assertions.assertThat(draw.nodes).hasSize(2)
        draw.init()
    }

    @Test
    open fun testDiscoveredFinalClassDependencyTaggedDependencyNoTagOnClass() {
        Assertions.assertThatThrownBy {
            compile(
                """
            @KoraApp
            interface ExampleApplication {
                class TestClass1
                
                @Root
                fun test(@Tag(TestClass1::class) testClass: TestClass1) = ""
            }
            """.trimIndent()
            )
        }
        Assertions.assertThat(compileResult.isFailed()).isTrue()
//        Assertions.assertThat<Diagnostic<out JavaFileObject?>>(compileResult.errors()).hasSize(1)
//        Assertions.assertThat(compileResult.errors().get(0).getMessage(Locale.ENGLISH)).startsWith(
//            "Required dependency was not found: " +
//                "@Tag(ru.tinkoff.kora.kora.app.annotation.processor.packageForDependencyTest.testDiscoveredFinalClassDependencyTaggedDependencyNoTagOnClass.ExampleApplication.TestClass1) " +
//                "ru.tinkoff.kora.kora.app.annotation.processor.packageForDependencyTest.testDiscoveredFinalClassDependencyTaggedDependencyNoTagOnClass.ExampleApplication.TestClass1"
//        )
    }

}
