package ru.tinkoff.kora.opentelemetry.module.jms.consumer;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import ru.tinkoff.kora.common.Context;
import ru.tinkoff.kora.jms.telemetry.JmsConsumerTracer;
import ru.tinkoff.kora.opentelemetry.common.OpentelemetryContext;

import javax.annotation.Nullable;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;
import java.util.ArrayList;
import java.util.List;

public class OpentelemetryJmsConsumerTracer implements JmsConsumerTracer {
    private final Tracer tracer;

    public OpentelemetryJmsConsumerTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public JmsConsumerSpan get(Message message) throws JMSException {
        var destination = message.getJMSDestination();
        var destinationString = "unknown";
        var destinationKind = "unknown";
        if (destination instanceof Queue queue) {
            destinationString = queue.getQueueName();
            destinationKind = "queue";
        } else if (destination instanceof Topic topic) {
            destinationString = topic.getTopicName();
            destinationKind = "topic";
        }
        var root = io.opentelemetry.context.Context.root();
        var parent = W3CTraceContextPropagator.getInstance().extract(root, message, MessageTextMapGetter.INSTANCE);
        var context = Context.current();

        var recordSpanBuilder = this.tracer
            .spanBuilder(destinationString + " receive")
            .setSpanKind(SpanKind.CONSUMER)
            .setAttribute(SemanticAttributes.MESSAGING_SYSTEM, "jms")
            .setAttribute(SemanticAttributes.MESSAGING_DESTINATION, destinationString)
            .setAttribute(SemanticAttributes.MESSAGING_DESTINATION_KIND, destinationKind)
            .setAttribute(SemanticAttributes.MESSAGING_MESSAGE_ID, message.getJMSMessageID());

        if (parent != root) {
            recordSpanBuilder.setParent(parent);
        }
        var span = recordSpanBuilder.startSpan();
        OpentelemetryContext.set(context, OpentelemetryContext.get(context).add(span));

        return (e) -> {
            if (e != null) {
                span.setStatus(StatusCode.ERROR);
            }
            span.end();
        };
    }

    private enum MessageTextMapGetter implements TextMapGetter<Message> {
        INSTANCE;

        @Override
        public Iterable<String> keys(Message carrier) {
            try {
                var enumeration = carrier.getPropertyNames();
                var headers = new ArrayList<String>();
                while (enumeration.hasMoreElements()) {
                    var nextElement = (String) enumeration.nextElement();
                    headers.add(nextElement);
                }
                return headers;
            } catch (JMSException e) {
                return List.of();
            }
        }

        @Nullable
        @Override
        public String get(@Nullable Message carrier, String key) {
            try {
                return carrier.getStringProperty(key);
            } catch (JMSException e) {
                return null;
            }
        }
    }
}
