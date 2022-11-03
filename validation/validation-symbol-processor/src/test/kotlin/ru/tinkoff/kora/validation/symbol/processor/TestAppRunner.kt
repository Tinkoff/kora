package ru.tinkoff.kora.validation.symbol.processor

import com.google.devtools.ksp.KspExperimental
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import org.junit.jupiter.api.Assertions
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw
import ru.tinkoff.kora.application.graph.RefreshableGraph
import ru.tinkoff.kora.config.ksp.processor.ConfigRootSymbolProcessorProvider
import ru.tinkoff.kora.config.ksp.processor.ConfigSourceSymbolProcessorProvider
import ru.tinkoff.kora.kora.app.ksp.KoraAppProcessorProvider
import ru.tinkoff.kora.ksp.common.symbolProcess
import ru.tinkoff.kora.validation.symbol.processor.testdata.AppWithConfig
import ru.tinkoff.kora.validation.symbol.processor.testdata.Bar
import ru.tinkoff.kora.validation.symbol.processor.testdata.Foo
import ru.tinkoff.kora.validation.symbol.processor.testdata.ValidationLifecycle
import java.util.function.Supplier

@KspExperimental
@KotlinPoetKspPreview
open class TestAppRunner : Assertions() {

    private var graph: InitializedGraph? = null

    data class InitializedGraph(val graphDraw: ApplicationGraphDraw, val refreshableGraph: RefreshableGraph)

    fun getService(): ValidationLifecycle {
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

    fun getGraph(): InitializedGraph {
        if (graph != null) {
            return graph as InitializedGraph
        }

        return try {
            val app = AppWithConfig::class
            val classLoader = symbolProcess(
                listOf(app, Foo::class, Bar::class),
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
}
