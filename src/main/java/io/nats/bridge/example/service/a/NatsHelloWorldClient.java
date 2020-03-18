package io.nats.bridge.example.service.a;

import io.nats.bridge.MessageBus;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


//TODO turn this into a test.
// See https://github.com/nats-io/nats-jms-mq-bridge/issues/16
public class NatsHelloWorldClient {

    public static void main(String... args) {
        try {
            final AtomicBoolean stop = new AtomicBoolean(false);
            final MessageBus messageBus = ServiceAUtil.getMessageBusNats();
            final List<String> names = Arrays.asList("Rick", "Tom", "Chris", "Paul");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stop.set(true)));

            int count = 0;
            while (true) {
                Thread.sleep(1);
                if (stop.get()) {
                    messageBus.close();
                    break;
                }
                final int index = count;
                names.forEach(name -> messageBus.request(name + index, System.out::println));
                count++;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
