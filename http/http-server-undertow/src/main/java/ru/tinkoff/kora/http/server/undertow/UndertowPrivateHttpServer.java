package ru.tinkoff.kora.http.server.undertow;

import io.undertow.Undertow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.XnioWorker;
import reactor.core.publisher.Mono;
import ru.tinkoff.kora.application.graph.ValueOf;
import ru.tinkoff.kora.common.util.ReactorUtils;
import ru.tinkoff.kora.http.server.common.HttpServerConfig;
import ru.tinkoff.kora.http.server.common.PrivateHttpServer;
import ru.tinkoff.kora.logging.common.arg.StructuredArgument;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;

public class UndertowPrivateHttpServer implements PrivateHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(UndertowPrivateHttpServer.class);

    private final ValueOf<HttpServerConfig> config;
    private final ValueOf<UndertowPrivateApiHandler> privateApiHandler;
    private final XnioWorker xnioWorker;
    private volatile Undertow undertow;

    public UndertowPrivateHttpServer(ValueOf<HttpServerConfig> config, ValueOf<UndertowPrivateApiHandler> privateApiHandler, @Nullable XnioWorker xnioWorker) {
        this.config = config;
        this.privateApiHandler = privateApiHandler;
        this.xnioWorker = xnioWorker;
    }

    @Override
    public Mono<Void> release() {
        return Mono.delay(this.config.get().shutdownWait())
            .then(ReactorUtils.ioMono(() -> {
                logger.debug("Private HTTP Server (Undertow) stopping...");
                final long started = System.nanoTime();
                if (this.undertow != null) {
                    this.undertow.stop();
                    this.undertow = null;
                }
                logger.info("Private HTTP Server (Undertow) stopped in {}", Duration.ofNanos(System.nanoTime() - started).toString().substring(2).toLowerCase());
            }));
    }

    @Override
    public Mono<Void> init() {
        return Mono.create(sink -> {
            // dirty hack to start undertow thread as non daemon
            var t = new Thread(() -> {
                logger.debug("Private HTTP Server (Undertow) starting...");
                final long started = System.nanoTime();
                try {
                    this.undertow = this.createServer();
                    this.undertow.start();
                    var data = StructuredArgument.marker( "port", this.port() );
                    logger.info(data, "Private HTTP Server (Undertow) started in {}", Duration.ofNanos(System.nanoTime() - started).toString().substring(2).toLowerCase());
                    sink.success();
                } catch (Throwable e) {
                    sink.error(e);
                }
            }, "undertow-private-init");
            t.setDaemon(false);
            t.start();
        });
    }

    private Undertow createServer() {
        return Undertow.builder()
            .addHttpListener(this.config.get().privateApiHttpPort(), "0.0.0.0", exchange -> this.privateApiHandler.get().handleRequest(exchange))
            .setWorker(this.xnioWorker)
            .build();
    }

    @Override
    public int port() {
        if (this.undertow == null) {
            return -1;
        }
        var info = this.undertow.getListenerInfo().get(0);
        var address = (InetSocketAddress) info.getAddress();
        return address.getPort();
    }
}
