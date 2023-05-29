package ru.tinkoff.kora.cache.annotation.processor.aop;

import com.squareup.javapoet.CodeBlock;
import reactor.core.publisher.Flux;
import ru.tinkoff.kora.annotation.processor.common.MethodUtils;
import ru.tinkoff.kora.cache.annotation.CachePut;
import ru.tinkoff.kora.cache.annotation.CachePuts;
import ru.tinkoff.kora.cache.annotation.processor.CacheOperation;
import ru.tinkoff.kora.cache.annotation.processor.CacheOperationUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.Set;

public class CachePutAopKoraAspect extends AbstractAopCacheAspect {

    private final ProcessingEnvironment env;

    public CachePutAopKoraAspect(ProcessingEnvironment env) {
        this.env = env;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(CachePut.class.getCanonicalName(), CachePuts.class.getCanonicalName());
    }

    @Override
    public ApplyResult apply(ExecutableElement method, String superCall, AspectContext aspectContext) {
        final CacheOperation operation = CacheOperationUtils.getCacheMeta(method);
        final List<String> cacheFields = getCacheFields(operation, env, aspectContext);

        final CodeBlock body = MethodUtils.isMono(method)
            ? buildBodyMono(method, operation, cacheFields, superCall)
            : buildBodySync(method, operation, cacheFields, superCall);

        return new ApplyResult.MethodBody(body);
    }

    private CodeBlock buildBodySync(ExecutableElement method,
                                    CacheOperation operation,
                                    List<String> cacheFields,
                                    String superCall) {
        final String superMethod = getSuperMethod(method, superCall);

        // cache variables
        final StringBuilder builder = new StringBuilder();

        // cache super method
        builder.append("var _value = ").append(superMethod).append(";\n");

        // cache put
        for (var cache : cacheFields) {
            builder.append(cache).append(".put(_key, _value);\n");
        }
        builder.append("return _value;");

        if (operation.parameters().size() == 1) {
            return CodeBlock.builder()
                .add("""
                        var _key = $L;
                        """,
                    operation.parameters().get(0))
                .add(builder.toString())
                .build();
        } else {
            final String recordParameters = getKeyRecordParameters(operation, method);
            return CodeBlock.builder()
                .add("""
                        var _key = $T.of($L);
                        """,
                    getCacheKey(operation), recordParameters)
                .add(builder.toString())
                .build();
        }
    }

    private CodeBlock buildBodyMono(ExecutableElement method,
                                    CacheOperation operation,
                                    List<String> cacheFields,
                                    String superCall) {
        final String superMethod = getSuperMethod(method, superCall);

        // cache variables
        final CodeBlock.Builder builder = CodeBlock.builder();

        // cache super method

        if (cacheFields.size() > 1) {
            if (operation.parameters().size() == 1) {
                builder.add("""
                    var _key = $L;
                    """, operation.parameters().get(0));
            } else {
                final String recordParameters = getKeyRecordParameters(operation, method);
                builder.add("""
                    var _key = $T.of($L);
                    """, getCacheKey(operation), recordParameters);
            }

            builder.add("return ")
                .add(superMethod)
                .add(".flatMap(_result -> $T.merge($T.of(\n", Flux.class, List.class);

            // cache put
            for (int i = 0; i < cacheFields.size(); i++) {
                final String cache = cacheFields.get(i);
                final String suffix = (i == cacheFields.size() - 1)
                    ? ".putAsync(_key, _result)\n"
                    : ".putAsync(_key, _result),\n";
                builder.add("\t").add(cache).add(suffix);
            }
            builder.add(")).then(Mono.just(_result)));");
        } else {
            builder.add("return ").add(superMethod);
            if (operation.parameters().size() == 1) {
                builder.add("""
                    .doOnSuccess(_result -> {
                        if(_result != null) {
                            $L.put($L, _result);
                        }
                    });
                    """, cacheFields.get(0), operation.parameters().get(0));
            } else {
                final String recordParameters = getKeyRecordParameters(operation, method);
                builder.add("""
                    .doOnSuccess(_result -> {
                        if(_result != null) {
                            $L.put($T.of($L), _result);
                        }
                    });
                    """, cacheFields.get(0), getCacheKey(operation), recordParameters);
            }
        }

        return builder.build();
    }
}
