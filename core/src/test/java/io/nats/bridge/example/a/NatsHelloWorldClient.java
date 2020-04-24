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

package io.nats.bridge.example.a;

import io.nats.bridge.MessageBus;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


//TODO turn this into a test.
// See https://github.com/nats-io/nats-jms-mq-bridge/issues/16
public class NatsHelloWorldClient {

    public static void main(String... args) {
        try {
            final AtomicBoolean stop = new AtomicBoolean(false);
            final MessageBus messageBus = ServiceAUtil.getMessageBusNats();
            final List<String> names = Arrays.asList("Rick", "Tom", "Chris", "Paul", "Noah", "Lucas");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stop.set(true)));

            int count = 0;
            while (true) {
                Thread.sleep(1);
                if (stop.get()) {
                    messageBus.close();
                    break;
                }
                final int index = count;
                names.forEach(name -> {
                    System.out.println("Sending: " + name + index);
                    messageBus.request(name + index, s -> System.out.println("Received: " + s));
                });
                count++;
                Thread.sleep(1000);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
