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


import io.nats.bridge.MessageBus;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.transform.TransformMessage;

import java.util.List;
import java.util.Map;
import java.util.Queue;


/**
 * A message bridge connects two MessageBuses.
 * A message bus is a queue or stream messaging system like Nats, Active MQ, SQS, Kinesis, Kafka, IBM MQ, RabbitMQ or JMS.
 * <p>
 * The bridge handles request/reply bridging or plain message forwarding.
 */
public class MessageBridgeForward extends MessageBridgeBase {




    public MessageBridgeForward(final String name, final MessageBus sourceBus, final MessageBus destinationBus,
                                 final List<String> inputTransforms,
                                final List<String> outputTransforms, final Map<String, TransformMessage> transformers) {
        super(name, sourceBus, destinationBus,  inputTransforms, outputTransforms, transformers);


    }

    @Override
    protected void processMessage(Message receiveMessageFromSource) {
        //Forward transforms.
        final Message currentMessageFinal = transformMessageIfNeeded(receiveMessageFromSource, transforms);
        if (currentMessageFinal == null) {
            return;
        }
        try {
            destinationBus.publish(currentMessageFinal);
        } catch (Exception ex) {
            restartDestinationBus(ex);
            destinationBus.publish(currentMessageFinal);
        }
    }



}
