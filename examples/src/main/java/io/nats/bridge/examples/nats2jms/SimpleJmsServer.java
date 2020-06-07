package io.nats.bridge.examples.nats2jms;

import io.nats.bridge.examples.JmsBuildUtils;

import javax.jms.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class SimpleJmsServer {
    public static void main(final String[] args) {

        final Duration waitForMessage = Duration.ofSeconds(20);

        try {
            final JmsBuildUtils jmsBuildUtils = new JmsBuildUtils().withDestinationName("dynamicQueues/requests");

            final Session session = jmsBuildUtils.getSession();

            final MessageConsumer messageConsumer = jmsBuildUtils.getConsumerSupplier().get();

            int count = 0;

            while (count < 100) {
                System.out.println("About to get Message");
                final Message messageFromClient = messageConsumer.receive(waitForMessage.toMillis());
                System.out.println("Attempted to get Message");

                if (messageFromClient == null) {
                    System.out.println("No message found");
                    count ++;
                } else {

                    System.out.println("Got the message now respond " + messageFromClient.getJMSReplyTo()
                            + " " + messageFromClient.getJMSCorrelationID());
                    final MessageProducer producer = session.createProducer(messageFromClient.getJMSReplyTo());

                    System.out.println("Got the producer now respond ");

                    if (messageFromClient instanceof BytesMessage) {
                        final BytesMessage requestMessage = (BytesMessage) messageFromClient;

                        final int length =  (int) requestMessage.getBodyLength();

                        final byte buffer[] = new byte[length];

                        requestMessage.readBytes(buffer);


                        final String message = new String(buffer, StandardCharsets.UTF_8);
                        final String responseText = "Server Got: " + message + " thank you";
                        final BytesMessage replyMessage = session.createBytesMessage();

                        replyMessage.setJMSCorrelationID(messageFromClient.getJMSCorrelationID());
                        replyMessage.writeBytes(responseText.getBytes(StandardCharsets.UTF_8));
                        producer.send(replyMessage);

                        System.out.println("SENT: " + responseText);
                    } else {
                        System.out.println("Message was not a bytes message " + messageFromClient);
                    }

                }
            }


        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
