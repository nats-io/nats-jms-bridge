package io.nats.bridge.support;


import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.transform.TransformMessage;
import io.nats.bridge.messages.transform.TransformResult;
import io.nats.bridge.messages.transform.Transformers;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class MessageBridgeUtil {

    public static Message transformMessageIfNeeded(final Message receiveMessageFromSource,
                                                   final List<String> transforms,
                                                   final boolean transformMessage,
                                                   final Map<String, TransformMessage> transformers,
                                                   final Logger logger,
                                                   final Logger runtimeLogger) {
        if (transforms.isEmpty()) return receiveMessageFromSource;

        Message currentMessage = receiveMessageFromSource;
        if (transformMessage) {
            TransformResult result = Transformers.runTransforms(transformers, transforms, currentMessage);
            switch (result.getResult()) {
                case SKIP:
                    if (runtimeLogger.isTraceEnabled())
                        runtimeLogger.trace("Message was skipped");
                    return null;
                case SYSTEM_ERROR:
                case ERROR:
                    if (result.getStatusMessage().isPresent()) {
                        logger.error(result.getStatusMessage().get(), result.getError());
                    } else {
                        logger.error("Error handling transform ", result.getError());
                    }
                    return null;
                case TRANSFORMED:
                    if (runtimeLogger.isTraceEnabled()) {
                        if (!result.getStatusMessage().isPresent()) {
                            runtimeLogger.trace("Message was transformed");
                        } else {
                            runtimeLogger.trace("Message was transformed " + result.getStatusMessage().get());
                        }
                    }
                    currentMessage = result.getTransformedMessage();
                case NOT_TRANSFORMED:
                    //no op
            }
        }
        return currentMessage;

    }


}
