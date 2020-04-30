package io.nats.bridge.integration.b.mq;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.integration.TestUtils;
import io.nats.bridge.support.MessageBridgeImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class JmsIbmMqToNatsBridgeTest {

    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final AtomicReference<String> responseFromServer = new AtomicReference<>();
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
        clientMessageBus = TestUtils.getMessageBusIbmMQ("CLIENT", false);
        serverMessageBus = TestUtils.getMessageBusNats("SERVER", "B");
        resultSignal = new CountDownLatch(1);
        serverStopped = new CountDownLatch(1);
        bridgeStopped = new CountDownLatch(1);

        bridgeMessageBusSource = TestUtils.getMessageBusIbmMQ("BRIDGE_SRC", true);
        bridgeMessageBusDestination = TestUtils.getMessageBusNats("BRIDGE_DST","B");
        messageBridge = new MessageBridgeImpl("", bridgeMessageBusSource, bridgeMessageBusDestination, true, null);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws Exception {
        runServerLoop();
        runBridgeLoop();
        clientMessageBus.request("RICK", s -> {
            responseFromServer.set(s);
            resultSignal.countDown();
        });
        runClientLoop();
        assertEquals("Hello RICK", responseFromServer.get());
        stopServerAndBridgeLoops();
    }

    private void runClientLoop() throws Exception {

        for (int index = 0; index < 10; index++) {

            System.out.println("Waiting");
            resultSignal.await(1, TimeUnit.SECONDS);
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