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
import io.nats.bridge.metrics.Metrics;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.Queue;
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

    private final Queue<MessageBridgeRequestReply> replyMessageQueue;


    public MessageBridgeImpl(final String name, final MessageBus sourceBus, final MessageBus destinationBus, boolean requestReply,
                             final Queue<MessageBridgeRequestReply> replyMessageQueue) {
        this.sourceBus = sourceBus;
        this.destinationBus = destinationBus;
        this.requestReply = requestReply;
        this.replyMessageQueue = (replyMessageQueue != null) ? replyMessageQueue : new LinkedTransferQueue<>();
        this.name = "bridge-" + name.toLowerCase().replace(".", "-").replace(" ", "-");
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

    private int doProcess(Optional<Message> receiveMessageFromSourceOption) {
        int count = 0;

        if (requestReply) {
            if (receiveMessageFromSourceOption.isPresent()) count++;

            receiveMessageFromSourceOption.ifPresent(receiveMessageFromSource -> {
                        //ystem.out.println("GOT MESSAGE " + receiveMessageFromSource.bodyAsString());
                        destinationBus.request(receiveMessageFromSource, replyMessage -> {
                            replyMessageQueue.add(new MessageBridgeRequestReply(receiveMessageFromSource, replyMessage));
                        });
                    }
            );
        } else {
            receiveMessageFromSourceOption.ifPresent(destinationBus::publish);
        }
        count += sourceBus.process();
        count += destinationBus.process();
        count += processReplies();
        return count;
    }

    @Override
    public int process(final Duration duration) {
        final Optional<Message> receiveMessageFromSourceOption = sourceBus.receive(duration);
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
    public void close() throws IOException {
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
