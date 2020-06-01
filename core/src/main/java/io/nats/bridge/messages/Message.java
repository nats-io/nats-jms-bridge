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

package io.nats.bridge.messages;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Represents a message in the NATS MQ/JMS Bridge
 */
public interface Message {


    String NO_TYPE = "NO_TYPE";

    /**
     * Timestamp if set this value will not be -1.
     * If set, then there will be headers sent.
     * @return timestamp.
     */
    default long timestamp() {
        return -1L;
    }

    /** TTL plus timestamp
     *
     * @return expirationTime
     */
    default long expirationTime() {
        return -1L;
    }


    /**
     * Delivery time is not instant
     * @return deliveryTime
     */
    default long deliveryTime() {
        return -1L;
    }

    /**
     * Delivery Mode in the JMS sense.
     * @return deliveryMode
     */
    default int deliveryMode() {
        return -1;
    }

    /**
     * The type of message in the JMS sense.
     * @return type
     */
    default String type() {
        return NO_TYPE;
    }


    default boolean redelivered() {
        return false;
    }

    default int priority() {
        return -1;
    }

    default String correlationID() {
        return "";
    }

    default Map<String, Object> headers() {
        return Collections.emptyMap();
    }

    default void reply(Message reply) {
    }

    default byte[] getBodyBytes() {
        return new byte[0];
    }

    default String bodyAsString() {
        return new String(getBodyBytes(), StandardCharsets.UTF_8);
    }

    default byte[] getMessageBytes() {
        return getBodyBytes();
    }
}
