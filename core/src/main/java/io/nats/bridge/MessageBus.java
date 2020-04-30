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

package io.nats.bridge;

import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.metrics.Metrics;

import java.io.Closeable;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;


/**
 * Message Bus wraps a Nats.io and a JMS message context to send/recieve messages to the messaging systems.
 * It also encapsulates how request/reply is DONE.
 * A message bus is a queue or stream messaging system like Nats, Active MQ, SQS, Kinesis, Kafka, IBM MQ, RabbitMQ or JMS.
 */
public interface MessageBus extends Closeable {

    String name();


    Metrics metrics();

    /**
     * Publish a message.
     *
     * @param message message bus message.
     */
    void publish(Message message);

    /**
     * Publish a string message
     *
     * @param message string message
     */
    default void publish(String message) {
        publish(MessageBuilder.builder().withBody(message).withCreator(name()).withNoReplyHandler("DEFAULT MESSAGE BUS PUBLISH").build());
    }

    /**
     * Perform a request/reply over nats or JMS.
     *
     * @param message       message to send
     * @param replyCallback callback.
     */
    void request(final Message message, Consumer<Message> replyCallback);

    /**
     * Perform a request/reply with strings
     *
     * @param message message string
     * @param reply   callback for reply string
     */
    default void request(final String message, final Consumer<String> reply) {
        request(MessageBuilder.builder().withBody(message).withCreator(name()).withNoReplyHandler("DEFAULT METHOD MESSAGE BUS REQUEST").build(),
                replyMessage -> reply.accept(replyMessage.bodyAsString()));
    }

    /**
     * Receives a message. The optional is none if the message is not received.
     *
     * @return a possible message.
     */
    Optional<Message> receive();

    /**
     * Receives a message. The optional is none if the message is not received.
     *
     * @return a possible message.
     */
    Optional<Message> receive(Duration duration);

    void close();

    int process();


}
