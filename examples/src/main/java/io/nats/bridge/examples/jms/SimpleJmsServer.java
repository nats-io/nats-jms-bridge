package io.nats.bridge.examples.jms;

import io.nats.bridge.examples.JmsBuildUtils;

import javax.jms.*;
import java.time.Duration;

public class SimpleJmsServer {
    public static void main(final String[] args)  {

        final Duration waitForMessage = Duration.ofSeconds(20);

        try {
            final JmsBuildUtils jmsBuildUtils = new JmsBuildUtils().withDestinationName("dynamicQueues/requests");

            final Session session = jmsBuildUtils.getSession();

            final MessageConsumer messageConsumer = jmsBuildUtils.getConsumerSupplier().get();


            System.out.println("About to get Message");
            final Message messageFromClient = messageConsumer.receive(waitForMessage.toMillis());
            System.out.println("Attempted to get Message");

            if (messageFromClient == null) {
                System.out.println("No message found");
            } else {

                System.out.println("Got the message now respond");
                final MessageProducer producer = session.createProducer(messageFromClient.getJMSDestination());

                if (messageFromClient instanceof TextMessage) {
                    final TextMessage requestMessage = (TextMessage) messageFromClient;

                    final String responseText = "Server Got: " + requestMessage.getText() + " thank you";
                    final TextMessage replyMessage = session.createTextMessage(responseText);
                    producer.send(replyMessage);
                }

            }


        }catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
