// Copyright 2020 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.bridge;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.jms.support.JMSMessageBusBuilder;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.nats.support.NatsMessageBusBuilder;

import javax.jms.*;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestUtils {
    public static MessageBus getMessageBusJms(final String name, final String topicPostFix) {
        final String queueName = "dynamicQueues/message-only-" + topicPostFix;
        final JMSMessageBusBuilder jmsMessageBusBuilder = new JMSMessageBusBuilder().withDestinationName(queueName).withName("JMS_" + name);

        return jmsMessageBusBuilder.withUserNameConnection("cloudurable").withPasswordConnection("cloudurable").build();
    }

    public static MessageBus getMessageBusIbmMQ(final String name, boolean src) {
        try {
            final JMSMessageBusBuilder jmsMessageBusBuilder = new JMSMessageBusBuilder()
                    .withName("IBM_MQ_" + name).useIBMMQ().withDestinationName("DEV.QUEUE.1")
                    .withResponseDestinationName("DEV.QUEUE.2");
             jmsMessageBusBuilder.withUserNameConnection("app").withPasswordConnection("passw0rd");
            if (src) {
                jmsMessageBusBuilder.asSource();
            }
            return jmsMessageBusBuilder.build();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public static MessageBus getMessageBusIbmMQWithHeaders(final String name, boolean src) {
        try {
            final JMSMessageBusBuilder jmsMessageBusBuilder = new JMSMessageBusBuilder()
                    .withName("IBM_MQ_" + name).useIBMMQ().withDestinationName("DEV.QUEUE.1")
                    .withResponseDestinationName("DEV.QUEUE.2");
            jmsMessageBusBuilder.withUserNameConnection("app").turnOnCopyHeaders().withPasswordConnection("passw0rd");

            System.out.println("JNDI " + jmsMessageBusBuilder.getJndiProperties());
            if (src) {
                jmsMessageBusBuilder.asSource();
            }
            return jmsMessageBusBuilder.build();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public static MessageBus getMessageBusIbmMQWithHeaders3(final String name, boolean src) {
        try {
            final JMSMessageBusBuilder jmsMessageBusBuilder = new JMSMessageBusBuilder()
                    .withName("IBM_MQ_" + name).useIBMMQ().withDestinationName("DEV.QUEUE.1")
                    .withResponseDestinationName("DEV.QUEUE.2");
            jmsMessageBusBuilder.withUserNameConnection("app").turnOnCopyHeaders().withPasswordConnection("passw0rd");

            System.out.println("JNDI " + jmsMessageBusBuilder.getJndiProperties());
            if (src) {
                jmsMessageBusBuilder.asSource();
            }
            return jmsMessageBusBuilder.build();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public static MessageBus getMessageBusIbmMQWithHeaders4(final String name, boolean src) {
        try {
//            final String host ="localhost";
//            final int port = 1414;
//            final String user= "app";
//            final int timeout = 15000; // in ms or 15 seconds
//            final String channel = "DEV.APP.SVRCONN";
//            final String password = "passw0rd";
//            final String queueManagerName = "QM1";
//            final String destinationName= "DEV.QUEUE.1";
//            final JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
//            final JmsConnectionFactory cf = ff.createConnectionFactory();
//            // Set the properties
//            cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, host);
//            cf.setIntProperty(WMQConstants.WMQ_PORT, port);
//            cf.setStringProperty(WMQConstants.WMQ_CHANNEL, channel);
//            cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
//            cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManagerName);
//            cf.setStringProperty(WMQConstants.USERID, user);
//            cf.setStringProperty(WMQConstants.PASSWORD, password);
//            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
            // Create JMS objects
//            final Connection connection = cf.createConnection();
//            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//            final Destination destination = session.createQueue(destinationName);
//            final Destination responseDestination = session.createTemporaryQueue();

            final JMSMessageBusBuilder jmsMessageBusBuilder = new JMSMessageBusBuilder()
                    //.withConnection(connection).withDestination(destination).withResponseDestination(responseDestination)
                    .withName("IBM_MQ_" + name).useIBMMQ().withDestinationName("DEV.QUEUE.1");
            //.withDestinationName("DEV.QUEUE.1");
                    //.withResponseDestinationName("DEV.QUEUE.2");
            jmsMessageBusBuilder.withUserNameConnection("app").turnOnCopyHeaders().withPasswordConnection("passw0rd");

            System.out.println("JNDI " + jmsMessageBusBuilder.getJndiProperties());
            if (src) {
                jmsMessageBusBuilder.asSource();
            }
            return jmsMessageBusBuilder.build();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public static MessageBus getMessageBusIbmMQWithHeaders2(final String name) {
        try {

            final String host ="localhost";
            final int port = 1414;
            final String user= "app";
            final int timeout = 15000; // in ms or 15 seconds
            final String channel = "DEV.APP.SVRCONN";
            final String password = "passw0rd";
            final String queueManagerName = "QM1";
            final String destinationName= "DEV.QUEUE.1";
//            final JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
//            final JmsConnectionFactory cf = ff.createConnectionFactory();
//            // Set the properties
//            cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, host);
//            cf.setIntProperty(WMQConstants.WMQ_PORT, port);
//            cf.setStringProperty(WMQConstants.WMQ_CHANNEL, channel);
//            cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
//            cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManagerName);
//            cf.setStringProperty(WMQConstants.USERID, user);
//            cf.setStringProperty(WMQConstants.PASSWORD, password);
//            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
            // Create JMS objects
            //Connection connection = cf.createConnection();
            //Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //Destination destination = session.createQueue(destinationName);
            //Destination responseDestination = session.createTemporaryQueue();
            //MessageConsumer consumer = session.createConsumer(destination);
            // Start the connection
            //connection.start();

            final JMSMessageBusBuilder jmsMessageBusBuilder = new JMSMessageBusBuilder().useIBMMQ()
                    .withName("IBM_MQ_" + name).withDestinationName(destinationName);
            jmsMessageBusBuilder.withUserNameConnection("app").turnOnCopyHeaders().withPasswordConnection("passw0rd");
            System.out.println("JNDI " + jmsMessageBusBuilder.getJndiProperties());
            return jmsMessageBusBuilder.build();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }



    public static MessageBus getMessageBusJmsWithHeaders(final String name, final String topicPostFix) {
        final String queueName = "dynamicQueues/headers-message-only-" + topicPostFix;
        final JMSMessageBusBuilder jmsMessageBusBuilder = new JMSMessageBusBuilder()
                .withName("JMS_W_HEADERS_" + name).turnOnCopyHeaders().withDestinationName(queueName);
        return jmsMessageBusBuilder.withUserNameConnection("cloudurable").withPasswordConnection("cloudurable").build();
    }

    public static MessageBus getMessageBusNats(final String name, final String topicPostFix) throws IOException, InterruptedException {

        final String subject = topicPostFix + "NatsMessageBus";

        final NatsMessageBusBuilder natsMessageBusBuilder = NatsMessageBusBuilder.builder().withName("NATS_" + name).withSubject(subject);
        natsMessageBusBuilder.getOptionsBuilder().noReconnect();
        return natsMessageBusBuilder.build();
    }


    public static void runBridgeLoop(final MessageBridge messageBridge, final AtomicBoolean stop, final CountDownLatch countDownLatch) {

        final Thread thread = new Thread(() -> {
            try {
                while (!stop.get()) {
                    int process = messageBridge.process(Duration.ofMillis(10));
                    if (process > 0) {
                        //System.out.println("Bridge Loop: Messages sent or received " + process);
                    }
                }
                messageBridge.close();
                countDownLatch.countDown();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        thread.start();
    }

    public static void stopServerAndBridgeLoops(final AtomicBoolean stop, final CountDownLatch countDownLatch1, final CountDownLatch countDownLatch2) throws Exception {
        stop.set(true);
        countDownLatch1.await(100, TimeUnit.MILLISECONDS);
        countDownLatch2.await(100, TimeUnit.MILLISECONDS);
    }


    public static void runServerLoop(final AtomicBoolean stop, final MessageBus serverMessageBus, final CountDownLatch serverStopped) {

        final Thread thread = new Thread(() -> {
            while (true) {
                if (stop.get()) {
                    serverMessageBus.close();
                    break;
                }
                final Optional<Message> receive = serverMessageBus.receive(Duration.ofMillis(10));
                receive.ifPresent(message -> {
                    //System.out.println("Server Loop: Handle message " + message.bodyAsString());
                    //System.out.println("Server Loop: Handle message headers " + message.headers());


                    final String myHeader = (String) message.headers().get("MY_HEADER");
                    if (myHeader == null) {
                        final Message reply = MessageBuilder.builder().withBody("Hello " + message.bodyAsString()).build();
                        message.reply(reply);
                    } else {
                        final Message reply = MessageBuilder.builder().withBody("Hello " + message.bodyAsString() + " MY_HEADER " + myHeader).build();
                        message.reply(reply);
                    }

                });
                serverMessageBus.process();
            }
            serverStopped.countDown();
        });

        thread.start();

    }
}
