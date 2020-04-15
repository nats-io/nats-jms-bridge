package io.nats.bridge.integration.a;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.integration.TestUtils;
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
        clientMessageNatsBus = TestUtils.getMessageBusNats("A");
        serverMessageJMSBus = TestUtils.getMessageBusJms("A");
        resultSignal = new CountDownLatch(1);
        serverStopped = new CountDownLatch(1);
        bridgeStopped = new CountDownLatch(1);

        bridgeMessageBusNatsSource = TestUtils.getMessageBusNats("A");
        bridgeMessageBusJMSDestination = TestUtils.getMessageBusJms("A");

        messageBridgeFomrNatsToJMS = new MessageBridge(bridgeMessageBusNatsSource, bridgeMessageBusJMSDestination, true);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws Exception {

        runServerLoop();
        runBridgeLoop();


        clientMessageNatsBus.request("RICK", s ->  {
            responseFromServer.set(s);
            resultSignal.countDown();
        });

        resultSignal.await(10, TimeUnit.SECONDS);

        assertEquals("Hello RICK", responseFromServer.get());


        stopServerAndBridgeLoops();
    }

    private void runBridgeLoop() {
        TestUtils.runBridgeLoop(messageBridgeFomrNatsToJMS, stop, bridgeStopped);
    }

    private void stopServerAndBridgeLoops() throws Exception{
        TestUtils.stopServerAndBridgeLoops(stop, serverStopped, bridgeStopped);
    }

    private void runServerLoop() {
        TestUtils.runServerLoop(stop, serverMessageJMSBus, serverStopped);
    }
}