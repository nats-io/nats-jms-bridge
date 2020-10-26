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

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.support.MessageBridgeImpl;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;


//TODO turn this into a test.
// See https://github.com/nats-io/nats-jms-mq-bridge/issues/16
public class BridgeManagerBProtoMain {

    public static void main(String... args) {
        try {
            final MessageBus messageBusSource = ServiceBUtil.getMessageBusJms();
            final MessageBus messageBusDestination = ServiceBUtil.getMessageBusNats();
            final MessageBridge messageBridge = new MessageBridgeImpl("", messageBusSource, messageBusDestination,
                    true, null, Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyMap());

            final AtomicBoolean stop = new AtomicBoolean(false);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> stop.set(true)));

            while (true) {
                if (stop.get()) break;
                Thread.sleep(10);
                messageBridge.process();
            }
            messageBridge.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
