package io.nats.bridge.integration.b;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.TestUtils;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.support.MessageBridgeBuilder;
import io.nats.bridge.support.MessageBusBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class JMSToNatsDynamicDestinationMessagesTest {

    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final AtomicReference<String> responseFromServer = new AtomicReference<>();
    private CountDownLatch resultSignal;
    private CountDownLatch serverStopped;
    private CountDownLatch bridgeStopped;

    private MessageBus serverMessageBus;
    private MessageBus serverMessageBus2;
    private MessageBus clientMessageBus;
    private MessageBus bridgeMessageBusSource;
    private MessageBus bridgeMessageBusDestination;

    private MessageBus responseBusServer;
    private MessageBus responseBusClient;
    private MessageBridge messageBridge;

    public static void runServerLoop(final AtomicBoolean stop, final MessageBus serverMessageBus, final MessageBus responseBusServer,
                                     final CountDownLatch serverStopped) {
        final Thread thread = new Thread(() -> {
            while (true) {
                if (stop.get()) {
                    serverMessageBus.close();
                    break;
                }
                final Optional<Message> receive = serverMessageBus.receive();

                if (!receive.isPresent()) {
                }
                receive.ifPresent(message -> {

                    System.out.println("Handle message " + message.bodyAsString() + "....................");
                    responseBusServer.publish(MessageBuilder.builder().withBody("Hello " + message.bodyAsString()).build());

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

    final String busName = "DynamicMessagesB";

    @Before
    public void setUp() throws Exception {


        final String responseName = "DynamicRESPONSEB";
        clientMessageBus = TestUtils.getMessageBusJmsBuilder("CLIENT", busName).build();
        serverMessageBus = TestUtils.getMessageBusNats("SERVER", busName);
        serverMessageBus2 = TestUtils.getMessageBusNats("SERVER", busName  + "_1");
        resultSignal = new CountDownLatch(1);
        serverStopped = new CountDownLatch(1);
        bridgeStopped = new CountDownLatch(1);

        MessageBusBuilder bridgeMessageBusSourceBuilder = TestUtils.getMessageBusJmsBuilder("BRIDGE_SOURCE", busName);
        MessageBusBuilder bridgeMessageBusDestinationBuilder = TestUtils.getMessageBusNatsBuilder("BRIDGE_DEST", busName);

        MessageBridgeBuilder messageBridgeBuilder = MessageBridgeBuilder.builder().withName("DynamicTest").withSourceBusBuilder(bridgeMessageBusSourceBuilder)
                .withDestinationBusBuilder(bridgeMessageBusDestinationBuilder)
                .withDynamicHeaderName("DYNAMIC_HEADER");

        messageBridge = messageBridgeBuilder.build();
        bridgeMessageBusDestination = messageBridgeBuilder.getDestinationBus();
        bridgeMessageBusSource = messageBridgeBuilder.getSourceBus();

        responseBusServer = TestUtils.getMessageBusNats("SERVER_RESPONSE", responseName);
        responseBusClient = TestUtils.getMessageBusNats("CLIENT_RESPONSE", responseName);

    }

    //@Test
    public void test() throws Exception {
        runServerLoop();
        runServerLoop2();
        runBridgeLoop();
        runClientLoop();
        //clientMessageBus.publish(MessageBuilder.builder().withBody("Rick").build());

        clientMessageBus.publish(MessageBuilder.builder().withBody("Rick").withHeader("DYNAMIC_HEADER", "dynamicQueues/message-only-" + busName + "_1").build());

        resultSignal.await(10, TimeUnit.SECONDS);

        assertEquals("Hello Rick", responseFromServer.get());
        stopServerAndBridgeLoops();
    }

    private void runClientLoop() {

        Thread th = new Thread(() -> {

            Optional<Message> receive;
            while (true) {
                receive = responseBusClient.receive();
                if (!receive.isPresent()) {
                    //System.out.println("No Client Message");
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

    private void runBridgeLoop() {
        TestUtils.runBridgeLoop(messageBridge, stop, bridgeStopped);
    }

    private void stopServerAndBridgeLoops() throws Exception {
        TestUtils.stopServerAndBridgeLoops(stop, serverStopped, bridgeStopped);
    }

    private void runServerLoop() {
        runServerLoop(stop, serverMessageBus, responseBusServer, serverStopped);
    }

    private void runServerLoop2() {
        runServerLoop(stop, serverMessageBus2, responseBusServer, serverStopped);
    }
}
