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

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A message bridge connects two MessageBuses.
 * A message bus is a queue or stream messaging system like Nats, Active MQ, SQS, Kinesis, Kafka, IBM MQ, RabbitMQ or JMS.
 *
 * The bridge handles request/reply bridging or plain message forwarding.
 */
public class MessageBridge implements Closeable {

    private final MessageBus sourceBus;
    private final MessageBus destinationBus;
    private final boolean requestReply;
    private final String name; 

    public MessageBridge(final MessageBus sourceBus, final MessageBus destinationBus, boolean requestReply) {
        this.sourceBus = sourceBus;
        this.destinationBus = destinationBus;
        this.requestReply = requestReply;
        this.name = "TEST"; 
    }

    public MessageBridge(final String name, final MessageBus sourceBus, final MessageBus destinationBus, boolean requestReply) {
        this.sourceBus = sourceBus;
        this.destinationBus = destinationBus;
        this.requestReply = requestReply;
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    public void process() {
        final Optional<Message> receiveMessageFromSourceOption = sourceBus.receive();

//        if (!receiveMessageFromSourceOption.isPresent()) {
//            ystem.out.println("No message bus");
//        } else {
//            ystem.out.println("GOT MESSAGE ON BRIDGE....................");
//        }

        if (requestReply) {
            receiveMessageFromSourceOption.ifPresent(receiveMessageFromSource -> {
                //ystem.out.println("### MESSAGE BRIDGE MESSAGE " + receiveMessageFromSource.bodyAsString() + " HEADERS" + receiveMessageFromSource.headers() );

                destinationBus.request(receiveMessageFromSource, replyMessage -> {
                            //ystem.out.println("GOT REPLY 1");
                            //ystem.out.println("### MESSAGE BRIDGE REPLY " + replyMessage.bodyAsString() + " HEADERS" + replyMessage.headers() );
                            receiveMessageFromSource.reply(replyMessage);
                            //ystem.out.println("GOT REPLY 2");
                        });
                    }
            );
        } else {
            receiveMessageFromSourceOption.ifPresent(destinationBus::publish);
        }

        sourceBus.process();
        destinationBus.process();
    }


    @Override
    public void close() throws IOException {
        sourceBus.close();
        destinationBus.close();
    }
}
