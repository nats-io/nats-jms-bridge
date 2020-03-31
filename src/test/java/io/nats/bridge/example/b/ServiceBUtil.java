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
import io.nats.bridge.jms.support.JMSMessageBusBuilder;
import io.nats.bridge.nats.NatsMessageBus;
import io.nats.bridge.util.ExceptionHandler;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public class ServiceBUtil {
    static MessageBus getMessageBusJms() {
        final String queueName = "dynamicQueues/B1Queue1";
        final JMSMessageBusBuilder jmsMessageBusBuilder = new JMSMessageBusBuilder().withDestinationName(queueName);
        final MessageBus messageBus = jmsMessageBusBuilder.build();
        return messageBus;
    }

    static MessageBus getMessageBusNats() throws IOException, InterruptedException {
        final String subject = "b1-subject1";

        final Options options = new Options.Builder().
                server("nats://localhost:4222").
                noReconnect(). // Disable reconnect attempts
                build();
        return new NatsMessageBus(subject, Nats.connect(options), "queueGroup" + UUID.randomUUID().toString() + System.currentTimeMillis(), new ExceptionHandler(LoggerFactory.getLogger("test")));
    }
}
