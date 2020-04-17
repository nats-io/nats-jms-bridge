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
package io.nats.bridge.example;

import io.nats.bridge.MessageBus;
import io.nats.bridge.nats.support.NatsMessageBusBuilder;
import io.nats.client.Options;

import java.util.concurrent.atomic.AtomicBoolean;


//TODO turn this into a test.
public class NatsMain {

    public static void main(String... args) {
        try {

            final AtomicBoolean stop = new AtomicBoolean(false);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                stop.set(true);
            }));

            final Options options = new Options.Builder().
                    server("nats://localhost:4222").
                    noReconnect(). // Disable reconnect attempts
                    build();


            NatsMessageBusBuilder natsMessageBusBuilder = NatsMessageBusBuilder.builder().withSubject("a1-subject").withQueueGroup("exampleGroup");
            natsMessageBusBuilder.getOptionsBuilder().noReconnect();
            final MessageBus messageBus = natsMessageBusBuilder.build();


            while (true) {
                Thread.sleep(1000);
                if (stop.get()) {
                    messageBus.close();
                    break;
                }

                messageBus.request("some message", System.out::println);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
