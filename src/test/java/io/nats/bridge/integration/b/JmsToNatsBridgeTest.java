package io.nats.bridge.integration.b;

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

public class JmsToNatsBridgeTest {

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
        clientMessageBus = TestUtils.getMessageBusNats("B");
        serverMessageBus = TestUtils.getMessageBusJms("B");
        resultSignal = new CountDownLatch(1);
        serverStopped = new CountDownLatch(1);
        bridgeStopped = new CountDownLatch(1);

        bridgeMessageBusSource = TestUtils.getMessageBusNats("B");
        bridgeMessageBusDestination = TestUtils.getMessageBusJms("B");
        messageBridge = new MessageBridge(bridgeMessageBusSource, bridgeMessageBusDestination, true);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws Exception {


        runServerLoop();
        runBridgeLoop();


        clientMessageBus.request("RICK", s ->  {
            responseFromServer.set(s);
            resultSignal.countDown();
        });


        runClientLoop();
        System.out.println(responseFromServer.get());


        stopServerAndBridgeLoops();
    }

    private void runClientLoop() throws Exception {

        for (int index = 0; index < 100; index++) {

            resultSignal.await(10, TimeUnit.MILLISECONDS);
            clientMessageBus.process();

            if (responseFromServer.get()!=null) break;
        }



    }

    private void runBridgeLoop() {
        TestUtils.runBridgeLoop(messageBridge, stop, bridgeStopped);
    }

    private void stopServerAndBridgeLoops() throws Exception{
        TestUtils.stopServerAndBridgeLoops(stop, serverStopped, bridgeStopped);
    }

    private void runServerLoop() {
        TestUtils.runServerLoop(stop, serverMessageBus, serverStopped);
    }
}