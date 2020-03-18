package io.nats.bridge.example;

import io.nats.bridge.MessageBus;
import io.nats.bridge.jms.support.JMSMessageBusBuilder;
import io.nats.bridge.nats.NatsMessageBus;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

import java.util.concurrent.atomic.AtomicBoolean;


//TODO turn this into a test.
public class NatsMain {

    public static void main(String... args) {
        try {

            final AtomicBoolean stop = new AtomicBoolean(false);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                stop.set(true);
            }));

            final Options options = new Options.Builder().
                    server("nats://localhost:4222").
                    noReconnect(). // Disable reconnect attempts
                    build();

            final MessageBus messageBus = new NatsMessageBus("test", Nats.connect(options));


            while (true) {
                Thread.sleep(1000);
                if (stop.get()) {
                    messageBus.close();
                    break;
                }

                messageBus.request("some message", System.out::println);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
