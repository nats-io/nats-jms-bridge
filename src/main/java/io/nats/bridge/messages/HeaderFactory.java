package io.nats.bridge.messages;

import java.util.Map;

public interface HeaderFactory {
    Map<String, Object> readHeader(byte [] bytes);
}
