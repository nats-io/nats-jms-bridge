package io.nats.bridge.support;

import io.nats.bridge.MessageBus;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.transform.TransformMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MessageBridgeDynamicForward extends MessageBridgeBase {


    private final Function<String, MessageBus> createMessageBusFunction;
    private final Map<String, MessageBus> busMap = new HashMap<>();
    private final String forwardHeaderName;


    public MessageBridgeDynamicForward(final String name, final MessageBus sourceBus, final MessageBus destinationBus,
                                       final List<String> inputTransforms,
                                       final List<String> outputTransforms,
                                       final Map<String, TransformMessage> transformers,
                                       final String forwardHeaderName,
                                       final Function<String, MessageBus> createMessageBus) {
        super(name, sourceBus, destinationBus, inputTransforms, outputTransforms, transformers);

        this.createMessageBusFunction = createMessageBus;
        this.forwardHeaderName = forwardHeaderName;
    }

    @Override
    protected void processMessage(Message receiveMessageFromSource) {
        //Forward transforms.
        final Message currentMessageFinal = transformMessageIfNeeded(receiveMessageFromSource, transforms);

        if (currentMessageFinal == null) {
            return;
        }

        final String destination = (String) currentMessageFinal.headers().get(forwardHeaderName);

        if (destination == null) {
            try {
                destinationBus.publish(currentMessageFinal);
            } catch (Exception ex) {
                restartMessageBus(ex, destinationBus);
                destinationBus.publish(currentMessageFinal);
            }
        } else {
            final MessageBus dynamicDestinationBus =
                    busMap.computeIfAbsent(destination, createMessageBusFunction);

            try {
                dynamicDestinationBus.publish(currentMessageFinal);
            } catch (Exception ex) {
                restartMessageBus(ex, dynamicDestinationBus);
                dynamicDestinationBus.publish(currentMessageFinal);
            }
        }
    }


    @Override
    protected int otherProcess() {

        int count = 0;

        for (MessageBus mb : busMap.values()) {
            count += processBus(mb);
        }

        return count;
    }

}
