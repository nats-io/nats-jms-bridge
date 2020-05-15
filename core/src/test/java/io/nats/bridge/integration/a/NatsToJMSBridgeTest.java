package io.nats.bridge.integration.a;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.TestUtils;
import io.nats.bridge.support.MessageBridgeImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class NatsToJMSBridgeTest {

    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final AtomicReference<String> responseFromServer = new AtomicReference<>();
    private CountDownLatch resultSignal;
    private CountDownLatch serverStopped;
    private CountDownLatch bridgeStopped;

    private MessageBus serverMessageJMSBus;
    private MessageBus clientMessageNatsBus;
    private MessageBus bridgeMessageBusNatsSource;
    private MessageBus bridgeMessageBusJMSDestination;
    private MessageBridge messageBridgeFomrNatsToJMS;

    @Before
    public void setUp() throws Exception {
        clientMessageNatsBus = TestUtils.getMessageBusNats("CLIENT","A");
        serverMessageJMSBus = TestUtils.getMessageBusJms("SERVER","A");
        resultSignal = new CountDownLatch(1);
        serverStopped = new CountDownLatch(1);
        bridgeStopped = new CountDownLatch(1);

        bridgeMessageBusNatsSource = TestUtils.getMessageBusNats("BRIDGE_SRC","A");
        bridgeMessageBusJMSDestination = TestUtils.getMessageBusJms("BRIDGE_DEST","A");

        messageBridgeFomrNatsToJMS = new MessageBridgeImpl("", bridgeMessageBusNatsSource, bridgeMessageBusJMSDestination, true, null);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws Exception {

        runServerLoop();
        runBridgeLoop();


        clientMessageNatsBus.request("RICK", s -> {
            responseFromServer.set(s);
            resultSignal.countDown();
        });

        for (int i = 0; i < 10; i++) {
            resultSignal.await(50, TimeUnit.MILLISECONDS);
            clientMessageNatsBus.process();
            resultSignal.await(1, TimeUnit.SECONDS);
        }

        assertEquals("Hello RICK", responseFromServer.get());


        stopServerAndBridgeLoops();
    }

    private void runBridgeLoop() {
        TestUtils.runBridgeLoop(messageBridgeFomrNatsToJMS, stop, bridgeStopped);
    }

    private void stopServerAndBridgeLoops() throws Exception {
        TestUtils.stopServerAndBridgeLoops(stop, serverStopped, bridgeStopped);
    }

    private void runServerLoop() {
        TestUtils.runServerLoop(stop, serverMessageJMSBus, serverStopped);
    }
}