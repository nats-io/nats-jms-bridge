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

public class MessageBridge implements Closeable {

    private final MessageBus sourceBus;
    private final MessageBus destinationBus;
    private final boolean requestReply;

    public MessageBridge(final MessageBus sourceBus, final MessageBus destinationBus, boolean requestReply) {
        this.sourceBus = sourceBus;
        this.destinationBus = destinationBus;
        this.requestReply = requestReply;
    }

    public void process() {
        final Optional<Message> receiveMessageFromSourceOption = sourceBus.receive();

        if (requestReply) {
            receiveMessageFromSourceOption.ifPresent(receiveMessageFromSource ->
                    destinationBus.request(receiveMessageFromSource, receiveMessageFromSource::reply));
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
