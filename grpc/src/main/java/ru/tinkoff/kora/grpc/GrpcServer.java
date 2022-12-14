package ru.tinkoff.kora.grpc;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.application.graph.Lifecycle;
import ru.tinkoff.kora.application.graph.ValueOf;
import ru.tinkoff.kora.common.readiness.ReadinessProbe;
import ru.tinkoff.kora.common.readiness.ReadinessProbeFailure;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class GrpcServer implements Lifecycle, ReadinessProbe {
    private final ValueOf<NettyServerBuilder> nettyServerBuilder;
    private Server server;

    private final AtomicReference<GrpcServerState> state = new AtomicReference<>(GrpcServerState.INIT);

    public GrpcServer(ValueOf<NettyServerBuilder> nettyServerBuilder) {
        this.nettyServerBuilder = nettyServerBuilder;
    }

    @Override
    public Mono<Void> init() {
        return Mono.create(sink -> {
            var builder = nettyServerBuilder.get();
            this.server = builder.build();
            try {
                this.server.start();
                this.state.set(GrpcServerState.RUN);
            } catch (IOException e) {
                sink.error(e);
            }
            sink.success();
        });

    }

    @Override
    public Mono<Void> release() {
        return Mono.fromRunnable(() -> {
            state.set(GrpcServerState.SHUTDOWN);
            server.shutdown();
        });
    }

    @Override
    public Mono<ReadinessProbeFailure> probe() {
        return switch (this.state.get()) {
            case INIT -> Mono.just(new ReadinessProbeFailure("Grpc server init"));
            case RUN -> Mono.empty();
            case SHUTDOWN -> Mono.just(new ReadinessProbeFailure("Grpc server shutdown"));
        };
    }

    private enum GrpcServerState {
        INIT, RUN, SHUTDOWN
    }
}
