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

package io.nats.bridge.example.b;

import io.nats.bridge.MessageBus;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


//TODO turn this into a test.
public class ServiceBNatsServer {

    public static void main(String... args) {
        try {

            final AtomicBoolean stop = new AtomicBoolean(false);
            final MessageBus messageBus = ServiceBUtil.getMessageBusNats();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> stop.set(true)));

            while (true) {
                if (stop.get()) {
                    messageBus.close();
                    break;
                }
                final Optional<Message> receive = messageBus.receive();
                receive.ifPresent(message -> {
                    System.out.println("Handle message " + message.bodyAsString());
                    message.reply(MessageBuilder.builder().withBody("Hello message " + message.bodyAsString()).build());
                });


                Thread.sleep(10);
                messageBus.process();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
