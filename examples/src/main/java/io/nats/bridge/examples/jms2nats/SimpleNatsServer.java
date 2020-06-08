package io.nats.bridge.examples.jms2nats;

import io.nats.client.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class SimpleNatsServer {

    public static void main(final String[] args) {
        try {

            final Options.Builder builder = new Options.Builder().server("nats://localhost:4222");
            final Connection connect = Nats.connect(builder.build());
            final Subscription subscription = connect.subscribe("natsClientRequests");
            final Duration requestTimeoutDuration = Duration.ofSeconds(30);
            int count = 0;

            while (count < 100) {
                System.out.println("About to get Message");
                final Message messageFromClient = subscription.nextMessage(requestTimeoutDuration);
                System.out.println("Attempted to get Message");

                if (messageFromClient != null) {
                    System.out.println("Got the producer now respond ");
                    final byte buffer[] = messageFromClient.getData();
                    final String message = new String(buffer, StandardCharsets.UTF_8);
                    final String responseText = "Server Got: " + message + " thank you";
                    connect.publish(messageFromClient.getReplyTo(), responseText.getBytes(StandardCharsets.UTF_8));
                    System.out.println("SENT: " + responseText);
                } else {
                    System.out.println("No message found");
                    count++;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
