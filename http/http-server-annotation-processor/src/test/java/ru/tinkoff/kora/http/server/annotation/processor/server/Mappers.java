package ru.tinkoff.kora.http.server.annotation.processor.server;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import ru.tinkoff.kora.application.graph.TypeRef;
import ru.tinkoff.kora.common.util.ReactorUtils;
import ru.tinkoff.kora.http.common.HttpHeaders;
import ru.tinkoff.kora.http.server.annotation.processor.controller.ReadableEntity;
import ru.tinkoff.kora.http.server.annotation.processor.controller.SomeEntity;
import ru.tinkoff.kora.http.server.annotation.processor.controller.TestEnum;
import ru.tinkoff.kora.http.server.common.HttpServerRequest;
import ru.tinkoff.kora.http.server.common.HttpServerResponse;
import ru.tinkoff.kora.http.server.common.HttpServerResponseEntity;
import ru.tinkoff.kora.http.server.common.handler.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class Mappers {
    public static Tuple2<Class<?>, Object> lookupParameter(Type type) {
        if (type instanceof Class clazz) {
            if (BlockingRequestExecutor.class.isAssignableFrom(clazz)) {
                return Tuples.of(BlockingRequestExecutor.class, new BlockingRequestExecutor.Default(ForkJoinPool.commonPool()));
            }
            try {
                return Tuples.of(clazz, clazz.getConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        var parameter = (ParameterizedType) type;
        if (parameter.getRawType() instanceof Class<?> clazz && HttpServerResponseEntityMapper.class.isAssignableFrom(clazz)) {
            return Tuples.of(HttpServerResponseEntityMapper.class, new HttpServerResponseEntityMapper<>(lookupResponseMapper(parameter)));
        }
        if (parameter.getRawType() instanceof Class clazz && HttpServerRequestMapper.class.isAssignableFrom(clazz)) {
            return Tuples.of(HttpServerRequestMapper.class, lookupRequestMapper(parameter));
        }
        if (parameter.getRawType() instanceof Class clazz && HttpServerResponseMapper.class.isAssignableFrom(clazz)) {
            return Tuples.of(HttpServerResponseMapper.class, lookupResponseMapper(parameter));
        }
        if (parameter.getRawType() instanceof Class clazz && StringParameterReader.class.isAssignableFrom(clazz)) {
            return Tuples.of(StringParameterReader.class, lookupStringReader(parameter));
        }
        throw new IllegalStateException();
    }


    public static HttpServerRequestMapper<?> lookupRequestMapper(Type type) {
        var parameter = (ParameterizedType) type;
        var requestType = parameter.getActualTypeArguments()[0];
        if (requestType.equals(HttpServerRequest.class)) {
            return noopRequestMapper();
        }
        if (requestType.equals(String.class)) {
            return stringRequestMapper();
        }
        if (requestType.equals(SomeEntity.class)) {
            return jsonRequestMapper(SomeEntity.class);
        }
        if (requestType.equals(byte[].class)) {
            return byteArrayRequestMapper();
        }
        if (requestType.equals(Integer.class)) {
            return integerRequestMapper();
        }
        var typeRef = (TypeRef<?>) TypeRef.of(
            List.class,
            TypeRef.of(
                List.class,
                TypeRef.of(
                    Tuple2.class,
                    TypeRef.of(
                        List.class,
                        TypeRef.of(String.class)
                    ),
                    TypeRef.of(
                        List.class,
                        TypeRef.of(SomeEntity.class)
                    )
                )
            )
        );
        if (requestType.equals(typeRef)) {
            return jsonRequestMapper(typeRef);
        }
        typeRef = TypeRef.of(Publisher.class, TypeRef.of(ByteBuffer.class));
        if (requestType.equals(typeRef)) {
            return byteBufferPublisherRequestMapper();
        }

        throw new RuntimeException("Unknown test request mapper: " + requestType);
    }


    public static HttpServerResponseMapper<?> lookupResponseMapper(Type type) {
        var parameter = (ParameterizedType) type;
        var responseType = parameter.getActualTypeArguments()[0];
        if (responseType.equals(Void.class)) {
            return voidResponseMapper();
        }
        if (responseType.equals(String.class)) {
            return stringResponseMapper();
        }
        if (responseType.equals(Integer.class)) {
            return integerResponseMapper();
        }
        if (responseType.equals(HttpServerResponse.class)) {
            return noopResponseMapper();
        }
        if (responseType.equals(byte[].class)) {
            return byteArrayResponseMapper();
        }
        if (responseType.equals(SomeEntity.class)) {
            return jsonResponseMapper(TypeRef.of(SomeEntity.class));
        }
        if (responseType.equals(TypeRef.of(HttpServerResponseEntity.class, TypeRef.of(String.class)))) {
            return new HttpServerResponseEntityMapper<>(stringResponseMapper());
        }

        throw new RuntimeException("Unknown test response mapper: " + responseType);
    }

    private static <T> HttpServerRequestMapper<T> jsonRequestMapper(Type someEntityClass) {
        return request -> Mono.just(null);
    }

    private static <T> HttpServerResponseMapper<T> jsonResponseMapper(TypeRef<T> someEntityClass) {
        return result -> Mono.just(HttpServerResponse.of(200, "text/plain", result.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private static HttpServerResponseMapper<HttpServerResponse> noopResponseMapper() {
        return Mono::just;
    }

    private static HttpServerRequestMapper<HttpServerRequest> noopRequestMapper() {
        return Mono::just;
    }

    private static HttpServerRequestMapper<String> stringRequestMapper() {
        return r -> Mono.from(r.body()).map(StandardCharsets.UTF_8::decode).map(CharBuffer::toString);
    }

    private static HttpServerResponseMapper<Void> voidResponseMapper() {
        return result -> Mono.just(HttpServerResponse.of(200, "text/plain"));
    }

    private static HttpServerResponseMapper<String> stringResponseMapper() {
        return result -> Mono.just(HttpServerResponse.of(200, "text/plain", StandardCharsets.UTF_8.encode(result != null ? result : "null")));
    }

    private static HttpServerRequestMapper<Integer> integerRequestMapper() {
        return r -> Mono.from(stringRequestMapper().apply(r)).map(Integer::parseInt);
    }

    private static HttpServerResponseMapper<Integer> integerResponseMapper() {
        return r -> Mono.just(HttpServerResponse.of(200, "text/plain", StandardCharsets.UTF_8.encode(r.toString())));
    }

    private static HttpServerRequestMapper<ByteBuffer> byteBufferPublisherRequestMapper() {
        return r -> ReactorUtils.toByteBufferMono(r.body());
    }

    private static HttpServerRequestMapper<byte[]> byteArrayRequestMapper() {
        return request -> ReactorUtils.toByteArrayMono(request.body());
    }

    private static HttpServerResponseMapper<byte[]> byteArrayResponseMapper() {
        return r -> Mono.just(HttpServerResponse.of(200, "text/plain", r));
    }

    private static StringParameterReader<ReadableEntity> readableEntityStringReader() {
        return ReadableEntity::new;
    }

    private static StringParameterReader<List<ReadableEntity>> readableEntityListStringReader() {
        return r -> Arrays.stream(r.split(",")).map(ReadableEntity::new).toList();
    }

    public static StringParameterReader<?> lookupStringReader(Type type) {
        var parameter = (ParameterizedType) type;
        var requestType = parameter.getActualTypeArguments()[0];
        if (requestType.equals(ReadableEntity.class)) {
            return readableEntityStringReader();
        } else if (requestType.equals(TestEnum.class)) {
            return new EnumStringParameterReader<>(TestEnum.values(), TestEnum::name);
        }

        throw new RuntimeException("Unknown test string parameter reader: " + requestType);
    }
}
