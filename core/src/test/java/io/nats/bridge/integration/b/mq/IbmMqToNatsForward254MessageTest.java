package io.nats.bridge.integration.b.mq;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.TestUtils;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.support.MessageBridgeImpl;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class IbmMqToNatsForward254MessageTest {

    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final AtomicReference<String> responseFromServer = new AtomicReference<>();
    private final AtomicReference<String> fooHeaderRef = new AtomicReference<>();
    private CountDownLatch resultSignal;
    private CountDownLatch serverStopped;
    private CountDownLatch bridgeStopped;

    private MessageBus serverMessageBusForNats;
    private MessageBus clientMessageBusForIbmMQ;
    private MessageBus bridgeMessageBusSourceForIbmMQ;
    private MessageBus bridgeMessageBusDestinationForNats;

    private MessageBus responseBusServer;
    private MessageBus responseBusClient;
    private MessageBridge messageBridge;

    public void runServerLoop(final AtomicBoolean stop, final MessageBus serverMessageBus, final MessageBus responseBusServer,
                                     final CountDownLatch serverStopped) {
        final Thread thread = new Thread(() -> {
            while (true) {
                if (stop.get()) {
                    serverMessageBus.close();
                    break;
                }
                final Optional<Message> receive = serverMessageBus.receive(Duration.ofSeconds(10));
                receive.ifPresent(message -> {
                    fooHeaderRef.set((String) message.headers().get("foo"));
                    System.out.println("Handle message " + message.bodyAsString());
                    responseBusServer.publish(MessageBuilder.builder().withBody("Hello " + message.bodyAsString()).build());
                });
                try {
                    Thread.sleep(1000);
                    System.out.println("SERVER LOOP");
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

        final String busName = "MessagesOnlyB254B";
        final String responseName = "RESPONSEB";
        clientMessageBusForIbmMQ = TestUtils.getMessageBusIbmMQ("", true);
        serverMessageBusForNats = TestUtils.getMessageBusNats("",busName);
        resultSignal = new CountDownLatch(1);
        serverStopped = new CountDownLatch(1);
        bridgeStopped = new CountDownLatch(1);

        bridgeMessageBusSourceForIbmMQ = TestUtils.getMessageBusIbmMQ("", true);
        bridgeMessageBusDestinationForNats = TestUtils.getMessageBusNats("",busName);

        responseBusServer = TestUtils.getMessageBusNats("",responseName);
        responseBusClient = TestUtils.getMessageBusNats("",responseName);
        messageBridge = new MessageBridgeImpl("", bridgeMessageBusSourceForIbmMQ, bridgeMessageBusDestinationForNats, false, null, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());

    }

    @Test
    public void test() throws Exception {
        TestUtils.drainBus(serverMessageBusForNats);
        drainClientLoop();
        runServerLoop();
        runBridgeLoop();
        runClientLoop();

        final Message  message = MessageBuilder.builder().withHeader("foo", "bar").withBody("Rick").build();
        clientMessageBusForIbmMQ.publish(message);

        for (int index = 0 ; index < 20; index++) {
            resultSignal.await(1, TimeUnit.SECONDS);
            if (responseFromServer.get()!=null) break;
        }
        resultSignal.await(10, TimeUnit.SECONDS);

        assertEquals("Hello Rick", responseFromServer.get());
        assertEquals("bar", fooHeaderRef.get());

        stopServerAndBridgeLoops();
    }

    private void runClientLoop() {

        Thread th = new Thread(() -> {

            Optional<Message> receive;
            while (true) {
                receive = responseBusClient.receive();
                if (receive.isPresent()) {
                    Message message = receive.get();

                    System.out.println("CLIENT MESSAGE FOUND IN RUN CLIENT LOOP");
                    responseFromServer.set(message.bodyAsString());
                    resultSignal.countDown();
                    break;
                }
                try {

                    Thread.sleep(10);
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
        runServerLoop(stop, serverMessageBusForNats, responseBusServer, serverStopped);
    }
}