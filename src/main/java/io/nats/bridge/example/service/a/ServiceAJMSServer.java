package io.nats.bridge.example.service.a;

import io.nats.bridge.Message;
import io.nats.bridge.MessageBus;
import io.nats.bridge.StringMessage;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


//TODO turn this into a test.
public class ServiceAJMSServer {

    public static void main(String... args) {
        try {

            final AtomicBoolean stop = new AtomicBoolean(false);
            final MessageBus messageBus = ServiceAUtil.getMessageBusJms();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> stop.set(true)));

            while (true) {
                if (stop.get()) {
                    messageBus.close();
                    break;
                }
                final Optional<Message> receive = messageBus.receive();
                receive.ifPresent(message -> {
                    StringMessage stringMessage = (StringMessage) message;
                    message.reply(new StringMessage("Hello " + stringMessage.getBody()));
                });

                if (!receive.isPresent()) {
                    Thread.sleep(1);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
