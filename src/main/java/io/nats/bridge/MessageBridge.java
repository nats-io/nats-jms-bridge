package io.nats.bridge;


import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

public class MessageBridge implements Closeable {

    private final MessageBus sourceBus;
    private final MessageBus destinationBus;

    public MessageBridge(MessageBus sourceBus, MessageBus destinationBus) {
        this.sourceBus = sourceBus;
        this.destinationBus = destinationBus;
    }

    public void process() {
        final Optional<Message> receiveMessageFromSourceOption = sourceBus.receive();
        receiveMessageFromSourceOption.ifPresent(receiveMessageFromSource -> {
            if (receiveMessageFromSource instanceof StringMessage) {
                System.out.println(((StringMessage) receiveMessageFromSource).getBody());
            }
            destinationBus.request(receiveMessageFromSource, replyFromDestination -> {
                receiveMessageFromSource.reply(replyFromDestination);
            });
        });
    }


    @Override
    public void close() throws IOException {
        sourceBus.close();
        destinationBus.close();
    }
}
