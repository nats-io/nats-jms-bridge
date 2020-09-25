package io.nats.bridge.integration.a.mq;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.TestUtils;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.support.MessageBridgeBuilder;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class NatsToIBM_MQForwardExpirationDeliveryTime272Test {

    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final AtomicReference<String> responseFromServer = new AtomicReference<>();
    private final AtomicReference<Long> expiration = new AtomicReference<>();
    private final AtomicReference<Long> deliveryTime = new AtomicReference<>();
    private CountDownLatch resultSignal;
    private CountDownLatch serverStopped;
    private CountDownLatch bridgeStopped;

    private MessageBus serverMessageBus;
    private MessageBus clientMessageBus;
    private MessageBus bridgeMessageBusSource;
    private MessageBus bridgeMessageBusDestination;

    private MessageBus responseBusServer;
    private MessageBus responseBusClient;
    private MessageBridge messageBridge;

    public  void runServerLoop(final AtomicBoolean stop, final MessageBus serverMessageBus, final MessageBus responseBusServer,
                                     final CountDownLatch serverStopped) {
        final Thread thread = new Thread(() -> {
            while (true) {
                if (stop.get()) {
                    serverMessageBus.close();
                    break;
                }
                final Optional<Message> receive = serverMessageBus.receive();

                receive.ifPresent(message -> {

                    System.out.println("Handle message " + message.bodyAsString() + "....................");

                    expiration.set(message.expirationTime());
                    deliveryTime.set(message.deliveryTime());
                    if (message.correlationID()!=null) {
                        responseBusServer.publish(MessageBuilder.builder().withBody("Hello " + new String(message.getBodyBytes(), StandardCharsets.UTF_8)).build());

                    } else {
                        responseBusServer.publish(MessageBuilder.builder()
                                .withCorrelationID(message.correlationID()).withBody("Hello " + new String(message.getBodyBytes(), StandardCharsets.UTF_8)).build());

                    }

                });
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                serverMessageBus.process();
            }
            serverStopped.countDown();
        });
        thread.start();

    }

    @Before
    public void setUp() throws Exception {

        final String busName = "MessagesOnly_A_255";
        final String responseName = "RESPONSE_A_255";
        clientMessageBus = TestUtils.getMessageBusNats("CLIENT", busName);
        serverMessageBus = TestUtils.getMessageBusIbmMQCopyHeader("SERVER", true);
        resultSignal = new CountDownLatch(1);
        serverStopped = new CountDownLatch(1);
        bridgeStopped = new CountDownLatch(1);

        bridgeMessageBusSource = TestUtils.getMessageBusNats("BRIDGE_SOURCE", busName);
        bridgeMessageBusDestination = TestUtils.getMessageBusIbmMQCopyHeader("BRIDGE_DEST", false);

        responseBusServer = TestUtils.getMessageBusJms("SERVER_RESPONSE", responseName);
        responseBusClient = TestUtils.getMessageBusJms("CLIENT_RESPONSE", responseName);


        messageBridge = MessageBridgeBuilder.builder().withDestinationBus(bridgeMessageBusDestination)
                .withSourceBus(bridgeMessageBusSource).withRequestReply(false).withName("NatsToIBM_MQForwardCopyHeaders254Test").build();


    }

    @Test
    public void test() throws Exception {
        TestUtils.drainBus(serverMessageBus);
        drainClientLoop();
        runServerLoop();
        runBridgeLoop();
        runClientLoop();

        final long deliveryTime = System.currentTimeMillis();
        final long expirationTime = System.currentTimeMillis() + 100_000;
        final Message  message = MessageBuilder.builder()
                .withDeliveryTime(deliveryTime).withExpirationTime(expirationTime)
                .withBody("Rick").build();

        clientMessageBus.publish(message);

        for (int index = 0 ; index < 20; index++) {
            resultSignal.await(1, TimeUnit.SECONDS);
            if (responseFromServer.get()!=null) break;
        }

        resultSignal.await(10, TimeUnit.SECONDS);

        assertEquals("Hello Rick", responseFromServer.get());


        assertEquals(expirationTime, (long) expiration.get());
        assertEquals(deliveryTime, (long) this.deliveryTime.get());


        stopServerAndBridgeLoops();
    }

    private void runClientLoop() {

        Thread th = new Thread(() -> {

            Optional<Message> receive;
            while (true) {
                receive = responseBusClient.receive();
                if (!receive.isPresent()) {
                    System.out.println("No Client Message");
                }
                if (receive.isPresent()) {
                    Message message = receive.get();
                    responseFromServer.set(message.bodyAsString());

                    resultSignal.countDown();
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        th.start();
    }

    private void drainClientLoop() throws Exception {
        TestUtils.drainBus(responseBusClient);
    }


    private void runBridgeLoop() {
        TestUtils.runBridgeLoop(messageBridge, stop, bridgeStopped);
    }

    private void stopServerAndBridgeLoops() throws Exception {
        TestUtils.stopServerAndBridgeLoops(stop, serverStopped, bridgeStopped);
    }

    private void runServerLoop() {
        runServerLoop(stop, serverMessageBus, responseBusServer, serverStopped);
    }
}
