// Copyright 2020 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.bridge.support;


import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;
import io.nats.bridge.messages.transform.Transformers;
import io.nats.bridge.metrics.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.LinkedTransferQueue;


/**
 * A message bridge connects two MessageBuses.
 * A message bus is a queue or stream messaging system like Nats, Active MQ, SQS, Kinesis, Kafka, IBM MQ, RabbitMQ or JMS.
 * <p>
 * The bridge handles request/reply bridging or plain message forwarding.
 */
public class MessageBridgeImpl implements MessageBridge {

    private final MessageBus sourceBus;
    private final MessageBus destinationBus;
    private final boolean requestReply;
    private final String name;
    private final boolean transformMessage;

    private Logger runtimeLogger = LoggerFactory.getLogger("runtime");

    private Logger logger = LoggerFactory.getLogger(MessageBridgeImpl.class);

    private final Queue<MessageBridgeRequestReply> replyMessageQueue;

    private final List<String> transforms;

    private final Map<String, TransformMessage> transformers;

    private final List<String> outputTransforms;




    public MessageBridgeImpl(final String name, final MessageBus sourceBus, final MessageBus destinationBus, boolean requestReply,
                             final Queue<MessageBridgeRequestReply> replyMessageQueue, final List<String> transforms, List<String> outputTransforms) {
        this.sourceBus = sourceBus;
        this.destinationBus = destinationBus;
        this.requestReply = requestReply;
        this.replyMessageQueue = (replyMessageQueue != null) ? replyMessageQueue : new LinkedTransferQueue<>();
        this.name = "bridge-" + name.toLowerCase().replace(".", "-").replace(" ", "-");
        this.transforms = transforms;

        this.transformMessage = transforms != null && transforms.size() > 0;
        this.outputTransforms = outputTransforms;


        transformers = transformMessage ? Transformers.loadTransforms() : Collections.emptyMap();

    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public int process() {
        final Optional<Message> receiveMessageFromSourceOption = sourceBus.receive();
        return doProcess(receiveMessageFromSourceOption);
    }


    private Message transformMessageIfNeeded(final Message receiveMessageFromSource,
                                             final List<String> transforms) {
        Message currentMessage = receiveMessageFromSource;
        if (transformMessage) {
            TransformResult result = Transformers.runTransforms(transformers, transforms, currentMessage);
            switch (result.getResult()) {
                case SKIP:
                    if (runtimeLogger.isTraceEnabled())
                        runtimeLogger.trace("Message was skipped");
                    return null;
                case SYSTEM_ERROR:
                case ERROR:
                    if (result.getStatusMessage().isPresent()) {
                        logger.error(result.getStatusMessage().get(), result.getError());
                    } else {
                        logger.error("Error handling transform ", result.getError());
                    }
                    return null;
                case TRANSFORMED:
                    if (runtimeLogger.isTraceEnabled()) {
                        if (!result.getStatusMessage().isPresent()) {
                            runtimeLogger.trace("Message was transformed");
                        } else {
                            runtimeLogger.trace("Message was transformed " + result.getStatusMessage().get());
                        }
                    }
                    currentMessage = result.getTransformedMessage();
                case NOT_TRANSFORMED:
                    //no op
            }
        }
        return currentMessage;
    }

    private int doProcess(Optional<Message> receiveMessageFromSourceOption) {
        int count = 0;
        if (receiveMessageFromSourceOption.isPresent()) count++;

        if (requestReply) {
            receiveMessageFromSourceOption.ifPresent(receiveMessageFromSource -> {
                        final Message currentMessageFinal = transformMessageIfNeeded(receiveMessageFromSource,  transforms);

                        if (currentMessageFinal == null) {
                            return;
                        }
                        destinationBus.request(currentMessageFinal, replyMessage -> {
                            if (runtimeLogger.isTraceEnabled()) {
                                runtimeLogger.info("The bridge {} got reply message {} \n for request message {} ",
                                        name, replyMessage.bodyAsString(), currentMessageFinal);
                            }
                            final Message replyMessageFinal = transformMessageIfNeeded(replyMessage,  transforms);
                            replyMessageQueue.add(new MessageBridgeRequestReply(currentMessageFinal, replyMessageFinal));
                        });
                    }
            );
        } else {
            receiveMessageFromSourceOption.ifPresent(receiveMessageFromSource -> {
                        final Message currentMessageFinal = transformMessageIfNeeded(receiveMessageFromSource,  transforms);
                        if (currentMessageFinal == null) {
                            return;
                        }
                        destinationBus.publish(currentMessageFinal);
                    }
            );
        }
        count += sourceBus.process();
        count += destinationBus.process();
        count += processReplies();
        return count;
    }

    @Override
    public int process(final Duration duration) {
        final Optional<Message> receiveMessageFromSourceOption = sourceBus.receive(duration);
        if (receiveMessageFromSourceOption.isPresent() && runtimeLogger.isTraceEnabled()) {
            runtimeLogger.trace("The {} bridge received message with body {}", name(), receiveMessageFromSourceOption.get().bodyAsString());
        }
        return doProcess(receiveMessageFromSourceOption);
    }

    private int processReplies() {
        int i = 0;
        MessageBridgeRequestReply requestReply = replyMessageQueue.poll();
        while (requestReply != null) {
            i++;
            requestReply.respond();
            requestReply = replyMessageQueue.poll();
        }
        return i;
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
