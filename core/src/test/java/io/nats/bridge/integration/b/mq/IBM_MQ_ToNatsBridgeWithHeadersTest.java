package io.nats.bridge.integration.b.mq;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.integration.TestUtils;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.support.MessageBridgeImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class IBM_MQ_ToNatsBridgeWithHeadersTest {

    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final AtomicReference<String> responseFromServer = new AtomicReference<>();
    private final AtomicReference<String> responseHeaderFromServer = new AtomicReference<>();
    private CountDownLatch resultSignal;
    private CountDownLatch serverStopped;
    private CountDownLatch bridgeStopped;
    private MessageBus serverMessageBus;
    private MessageBus clientMessageBus;
    private MessageBus bridgeMessageBusSource;
    private MessageBus bridgeMessageBusDestination;
    private MessageBridge messageBridge;

    @Before
    public void setUp() throws Exception {
        clientMessageBus = TestUtils.getMessageBusIbmMQWithHeaders("CLIENT",false);
        serverMessageBus = TestUtils.getMessageBusNats("SERVER","B");
        resultSignal = new CountDownLatch(1);
        serverStopped = new CountDownLatch(1);
        bridgeStopped = new CountDownLatch(1);

        bridgeMessageBusSource = TestUtils.getMessageBusIbmMQWithHeaders("BRIDGE_SOURCE",true);
        bridgeMessageBusDestination = TestUtils.getMessageBusNats("BRIDGE_DEST","B");
        messageBridge = new MessageBridgeImpl("", bridgeMessageBusSource, bridgeMessageBusDestination, true, null);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws Exception {


        runServerLoop();
        runBridgeLoop();


        final Message message = MessageBuilder.builder().withHeader("MY_HEADER", "MY_VALUE").withBody("RICK").build();

        clientMessageBus.request(message, reply -> {

            responseHeaderFromServer.set((String) reply.headers().get("MY_HEADER"));
            responseFromServer.set(reply.bodyAsString());
            resultSignal.countDown();
        });

        runClientLoop();
        //assertEquals ("MY_VALUE", responseHeaderFromServer.get());
        assertEquals("Hello RICK MY_HEADER MY_VALUE", responseFromServer.get());


        stopServerAndBridgeLoops();
    }

    private void runClientLoop() throws Exception {

        for (int index = 0; index < 100; index++) {

            System.out.println("Waiting");
            resultSignal.await(100, TimeUnit.MILLISECONDS);
            clientMessageBus.process();

            if (responseFromServer.get() != null) break;
        }


    }

    private void runBridgeLoop() {
        TestUtils.runBridgeLoop(messageBridge, stop, bridgeStopped);
    }

    private void stopServerAndBridgeLoops() throws Exception {
        TestUtils.stopServerAndBridgeLoops(stop, serverStopped, bridgeStopped);
    }

    private void runServerLoop() {
        TestUtils.runServerLoop(stop, serverMessageBus, serverStopped);
    }
}