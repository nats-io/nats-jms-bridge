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
import io.nats.bridge.nats.support.NatsMessageBusBuilder;

import java.io.IOException;

public class ServiceBUtil {
    static MessageBus getMessageBusJms() {
        final String queueName = "dynamicQueues/B1Queue1";
        final JMSMessageBusBuilder jmsMessageBusBuilder = new JMSMessageBusBuilder().withUserNameConnection("cloudurable").withPasswordConnection("cloudurable")
                .withDestinationName(queueName);
        final MessageBus messageBus = jmsMessageBusBuilder.build();
        return messageBus;
    }

    static MessageBus getMessageBusNats() throws IOException, InterruptedException {

        NatsMessageBusBuilder natsMessageBusBuilder = NatsMessageBusBuilder.builder().withSubject("b1-subject1");
        natsMessageBusBuilder.getOptionsBuilder().noReconnect();
        return natsMessageBusBuilder.build();

    }
}
