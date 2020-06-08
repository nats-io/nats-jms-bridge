package io.nats.bridge.examples.ibmmq.mq2nats;

import io.nats.bridge.examples.JmsBuildUtils;
import io.nats.bridge.examples.ibmmq.IbmMqUtils;

import javax.jms.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class SimpleJmsClient {

    public static void main (String[] args) {
        try {


            final JmsBuildUtils jmsBuildUtils = new JmsBuildUtils()
                    .withConnectionFactory(IbmMqUtils.createJmsConnectionFactoryWithQModel());


            final Session session = jmsBuildUtils.getSession();


            jmsBuildUtils.withDestination(session.createQueue("DEV.QUEUE.2"));


            final MessageProducer messageProducer = jmsBuildUtils.getProducerSupplier().get();
            final Destination replyQueue = session.createTemporaryQueue();
            final Duration replyTimeoutDuration = Duration.ofSeconds(30);



            final BytesMessage requestMessage = session.createBytesMessage();
            requestMessage.writeBytes("Hello World!".getBytes(StandardCharsets.UTF_8));
            requestMessage.setJMSCorrelationID(UUID.randomUUID().toString());
            requestMessage.setJMSReplyTo(replyQueue);

            messageProducer.send(requestMessage);

            final MessageConsumer replyConsumer = session.createConsumer(replyQueue);

            final Message reply = replyConsumer.receive(replyTimeoutDuration.toMillis());

            if (reply instanceof BytesMessage) {
                final BytesMessage bytesReply = (BytesMessage) reply;
                final byte[] buffer = new  byte[(int)bytesReply.getBodyLength()];
                bytesReply.readBytes(buffer);

                System.out.println("REPLY: " + new String(buffer, StandardCharsets.UTF_8));
            } else {
                System.out.println("No reply message came back or wrong type");
            }


        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
