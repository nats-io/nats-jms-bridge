package io.nats.bridge.example.service.a;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;

import java.util.concurrent.atomic.AtomicBoolean;


//TODO turn this into a test.
// See https://github.com/nats-io/nats-jms-mq-bridge/issues/16
public class BridgeManagerProtoMain {

    public static void main(String... args) {
        try {
            final MessageBus messageBusSource = ServiceAUtil.getMessageBusNats();
            final MessageBus messageBusDestination = ServiceAUtil.getMessageBusJms();
            final MessageBridge messageBridge = new MessageBridge(messageBusSource, messageBusDestination);

            final AtomicBoolean stop = new AtomicBoolean(false);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> stop.set(true)));

            while (true) {
                if (stop.get()) break;
                messageBridge.process();
            }
            messageBridge.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
