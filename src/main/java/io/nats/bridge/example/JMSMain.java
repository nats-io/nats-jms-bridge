package io.nats.bridge.example;

import io.nats.bridge.MessageBus;
import io.nats.bridge.jms.support.JMSMessageBusBuilder;

import java.util.concurrent.atomic.AtomicBoolean;


//TODO turn this into a test.
public class JMSMain {

    public static void main(String... args) {
        try {

            final AtomicBoolean stop = new AtomicBoolean(false);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                stop.set(true);
            }));

            final JMSMessageBusBuilder jmsMessageBusBuilder = new JMSMessageBusBuilder().withDestinationName("dynamicQueues/testQueue");

            final MessageBus messageBus = jmsMessageBusBuilder.build();

            while (true) {
                Thread.sleep(1000);
                if (stop.get()){
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
