package ru.tinkoff.kora.json.annotation.processor;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tinkoff.kora.application.graph.ApplicationGraphDraw;
import ru.tinkoff.kora.json.common.JsonReader;
import ru.tinkoff.kora.json.common.JsonWriter;
import ru.tinkoff.kora.kora.app.annotation.processor.KoraAppProcessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericsTest extends AbstractJsonAnnotationProcessorTest {
    @Test
    public void testRecordWithTypeParameter() {
        compile("""
            @Json
            public record TestRecord <T>(T value, java.util.List<T> values) {
            }
            """);
        var r1 = Mockito.mock(JsonReader.class);
        var r2 = Mockito.mock(JsonReader.class);
        var w1 = Mockito.mock(JsonWriter.class);
        var w2 = Mockito.mock(JsonWriter.class);
        var m1 = mapper("TestRecord", List.of(r1, r2), List.of(w1, w2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRecordWithTypeParametersFromExtension() throws IOException, ClassNotFoundException {
        compile(List.of(new JsonAnnotationProcessor(), new KoraAppProcessor()), """
                @Json
                public record TestRecord <T>(T value, java.util.List<T> values) {
                }
                """,
            """
                @KoraApp
                public interface TestApp extends ru.tinkoff.kora.json.common.JsonCommonModule {
                    @Root
                    default String root1(ru.tinkoff.kora.json.common.JsonWriter<TestRecord<String>> w, ru.tinkoff.kora.json.common.JsonReader<TestRecord<String>> r) { return ""; }
                    @Root
                    default String root2(ru.tinkoff.kora.json.common.JsonWriter<TestRecord<Integer>> w, ru.tinkoff.kora.json.common.JsonReader<TestRecord<Integer>> r) { return ""; }
                }
                    """);
        compileResult.assertSuccess();
        var supplier = (Supplier<ApplicationGraphDraw>) newObject("TestAppGraph");
        var draw = supplier.get();
        var graph = draw.init().block();
        var rc = compileResult.loadClass("$TestRecordJsonReader");
        var wc = compileResult.loadClass("$TestRecordJsonWriter");
        JsonReader<Object> reader = null;
        JsonWriter<Object> writer = null;
        for (var node : draw.getNodes()) {
            var object = graph.get(node);
            if (reader == null && rc.isInstance(object)) {
                reader = (JsonReader<Object>) object;
            }
            if (writer == null && wc.isInstance(object)) {
                writer = (JsonWriter<Object>) object;
            }
        }

        var o = newObject("TestRecord", "test", List.of("test1", "test2"));
        var json = "{\"value\":\"test\",\"values\":[\"test1\",\"test2\"]}";

        assertThat(reader.read(json.getBytes(StandardCharsets.UTF_8))).isEqualTo(o);
        assertThat(writer.toByteArray(o)).asString(StandardCharsets.UTF_8).isEqualTo(json);
    }
}
