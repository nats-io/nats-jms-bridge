package io.nats.bridge.support;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.metrics.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.LinkedTransferQueue;

public abstract class MessageBridgeBase implements MessageBridge {
    protected final MessageBus sourceBus;
    protected final MessageBus destinationBus;
    protected final String name;
    protected final boolean transformMessage;

    protected final List<String> transforms;
    protected final Map<String, TransformMessage> transformers;
    protected final List<String> outputTransforms;
    protected final Logger logger = LoggerFactory.getLogger(io.nats.bridge.support.MessageBridgeRequestReply.class);
    protected final Logger runtimeLogger = LoggerFactory.getLogger("runtime");

    protected final MessageBusRestarter messageBusRestarter;

    public MessageBridgeBase(final String name, final MessageBus sourceBus, final MessageBus destinationBus,

                             final List<String> inputTransforms, final List<String> outputTransforms,
                             final Map<String, TransformMessage> transformers) {
        this.sourceBus = sourceBus;
        this.destinationBus = destinationBus;
        this.name = "bridge-" + name.toLowerCase().replace(".", "-").replace(" ", "-");
        this.transforms = inputTransforms;
        this.outputTransforms = outputTransforms;

        boolean inputTransformEnabled = inputTransforms != null && inputTransforms.size() > 0;
        boolean outputTransformEnabled = outputTransforms != null && outputTransforms.size() > 0;

        this.transformMessage = inputTransformEnabled || outputTransformEnabled;

        this.transformers = transformMessage ? transformers : Collections.emptyMap();
        this.messageBusRestarter = new MessageBusRestarter(name, logger);
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public int process() {
        Optional<Message> receiveMessageFromSourceOption;
        try {
            receiveMessageFromSourceOption = sourceBus.receive();

        } catch (Exception ex) {
            receiveMessageFromSourceOption = Optional.empty();
            restartSourceBus(ex);
        }
        return doProcess(receiveMessageFromSourceOption);
    }

    protected Message transformMessageIfNeeded(final Message receiveMessageFromSource,
                                             final List<String> transforms) {
        return MessageBridgeUtil.transformMessageIfNeeded(receiveMessageFromSource, transforms, transformMessage, transformers, logger, runtimeLogger);
    }

    private int doProcess(Optional<Message> receiveMessageFromSourceOption) {

        if (receiveMessageFromSourceOption.isPresent() && runtimeLogger.isTraceEnabled()) {
            runtimeLogger.trace("The {} bridge received message with body {}", name(), receiveMessageFromSourceOption.get().bodyAsString());
        }


            receiveMessageFromSourceOption.ifPresent(this::processMessage);

        return processMessageBusQueues(receiveMessageFromSourceOption);
    }





    protected abstract void processMessage(final Message receiveMessageFromSource) ;

    protected int processBus(MessageBus bus) {

        try {
            return bus.process();
        } catch (Exception ex) {
            restartMessageBus(ex, bus);
            return bus.process();
        }

    }

    private int processMessageBusQueues(Optional<Message> receiveMessageFromSourceOption) {
        int count = 0;
        if (receiveMessageFromSourceOption.isPresent()) count++;


        count += processBus(sourceBus);
        count += processBus(destinationBus);
        count += otherProcess();
        return count;
    }

    protected int otherProcess() {
        return 0;
    }

    private void restartMessageBus(final Exception ex, final MessageBus messageBus) {
        messageBusRestarter.restartMessageBus(ex, messageBus);
    }

    protected void restartDestinationBus(final Exception ex) {
        restartMessageBus(ex, destinationBus);
    }

    private void restartSourceBus(Exception ex) {
        restartMessageBus(ex, sourceBus);
    }

    @Override
    public int process(final Duration duration) {


        Optional<Message> receiveMessageFromSourceOption;
        try {
            receiveMessageFromSourceOption = sourceBus.receive(duration);

        } catch (Exception ex) {
            receiveMessageFromSourceOption = Optional.empty();
            restartSourceBus(ex);
        }


        return doProcess(receiveMessageFromSourceOption);
    }



    @Override
    public void close() {
        sourceBus.close();
        destinationBus.close();
    }

    @Override
    public Metrics sourceMetrics() {
        return sourceBus.metrics();
    }

    @Override
    public Metrics destinationMetrics() {
        return destinationBus.metrics();
    }

    public static class MessageBridgeRequestReply {
        final Message request;
        final Message reply;

        public MessageBridgeRequestReply(Message request, Message reply) {
            this.request = request;
            this.reply = reply;
        }

        public void respond() {
            request.reply(reply);
        }
    }
}
