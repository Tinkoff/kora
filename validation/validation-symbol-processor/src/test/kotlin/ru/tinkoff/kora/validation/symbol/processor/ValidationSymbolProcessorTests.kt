package ru.tinkoff.kora.validation.symbol.processor

import com.google.devtools.ksp.KspExperimental
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw
import ru.tinkoff.kora.application.graph.RefreshableGraph
import ru.tinkoff.kora.config.ksp.processor.ConfigRootSymbolProcessorProvider
import ru.tinkoff.kora.config.ksp.processor.ConfigSourceSymbolProcessorProvider
import ru.tinkoff.kora.kora.app.ksp.KoraAppProcessorProvider
import ru.tinkoff.kora.ksp.common.symbolProcess
import ru.tinkoff.kora.validation.symbol.processor.testdata.AppWithConfig
import ru.tinkoff.kora.validation.symbol.processor.testdata.Baby
import ru.tinkoff.kora.validation.symbol.processor.testdata.ValidationLifecycle
import ru.tinkoff.kora.validation.symbol.processor.testdata.Yoda
import java.time.OffsetDateTime
import java.util.function.Supplier

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@KspExperimental
@KotlinPoetKspPreview
class ValidationSymbolProcessorTests : Assertions() {

    private var graph: InitializedGraph? = null

    data class InitializedGraph(val graphDraw: ApplicationGraphDraw, val refreshableGraph: RefreshableGraph)

    private fun getService(): ValidationLifecycle {
        val graph = getGraph()
        val values = graph.graphDraw.nodes
            .stream()
            .map { node -> graph.refreshableGraph.get(node) }
            .toList()

        return values.asSequence()
            .filter { a -> a is ValidationLifecycle }
            .map { a -> a as ValidationLifecycle }
            .first()
    }

    private fun getGraph(): InitializedGraph {
        if (graph != null) {
            return graph as InitializedGraph
        }

        return try {
            val app = AppWithConfig::class
            val classLoader = symbolProcess(
                listOf(app, Baby::class, Yoda::class),
                KoraAppProcessorProvider(),
                ConfigRootSymbolProcessorProvider(),
                ConfigSourceSymbolProcessorProvider(),
                ValidationSymbolProcessorProvider()
            )

            val clazz = classLoader.loadClass(app.qualifiedName + "Graph")
            val graphDraw = (clazz.constructors.first().newInstance() as Supplier<ApplicationGraphDraw>).get()
            val graph = graphDraw.init().block()!!
            InitializedGraph(graphDraw, graph)
        } catch (e: Exception) {
            throw e
        }
    }

    @Test
    fun test() {
        val lifecycle = getService()

        val yoda = Yoda()
        yoda.id = "1"
        yoda.codes = listOf(1)
        yoda.babies = listOf(Baby("1", 1, OffsetDateTime.now(), null))
    }
}
