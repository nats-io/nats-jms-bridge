package io.nats.bridge.integration.a;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.TestUtils;
import io.nats.bridge.support.MessageBridgeImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class NatsToNatsBridgeTest {

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
        clientMessageBus = TestUtils.getMessageBusNats("CLIENT","A");
        serverMessageBus = TestUtils.getMessageBusNats("SERVER","B");


        bridgeMessageBusSource = TestUtils.getMessageBusNats("BRIDGE_SRC","A");
        bridgeMessageBusDestination = TestUtils.getMessageBusNats("BRIDGE_DEST","B");

        messageBridge = new MessageBridgeImpl("", bridgeMessageBusSource, bridgeMessageBusDestination, true, null, Collections.emptyList(), Collections.emptyList());

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws Exception {
        resultSignal = new CountDownLatch(1);
        serverStopped = new CountDownLatch(1);
        bridgeStopped = new CountDownLatch(1);

        runServerLoop();
        runBridgeLoop();


        clientMessageBus.request("RICK", s -> {
            responseFromServer.set(s);
            resultSignal.countDown();
        });

        for (int i = 0; i < 10; i++) {
            resultSignal.await(1, TimeUnit.MILLISECONDS);
            clientMessageBus.process();
            resultSignal.await(1, TimeUnit.SECONDS);
        }

        assertEquals("Hello RICK", responseFromServer.get());


        stopServerAndBridgeLoops();
    }

    @Test
    public void testMore() throws Exception {
        resultSignal = new CountDownLatch(1_000);
        serverStopped = new CountDownLatch(1_000);
        bridgeStopped = new CountDownLatch(1_000);
        final AtomicInteger counter = new AtomicInteger();

        runServerLoop();
        runBridgeLoop();


        for (int i = 0; i < 1_000; i++) {
            clientMessageBus.request("RICK", s -> {
                responseFromServer.set(s);
                resultSignal.countDown();
                counter.incrementAndGet();
            });
        }

        for (int i = 0; i < 10; i++) {
            resultSignal.await(50, TimeUnit.MILLISECONDS);
            clientMessageBus.process();
        }
        resultSignal.await(5, TimeUnit.SECONDS);

        assertEquals("Hello RICK", responseFromServer.get());

        assertEquals(1_000, counter.get());

        stopServerAndBridgeLoops();
    }


    @Test
    public void testALotMore() throws Exception {
        resultSignal = new CountDownLatch(10_000);
        serverStopped = new CountDownLatch(10_000);
        bridgeStopped = new CountDownLatch(10_000);
        final AtomicInteger counter = new AtomicInteger();

        runServerLoop();
        runBridgeLoop();


        for (int i = 0; i < 10_000; i++) {
            clientMessageBus.request("RICK", s -> {
                responseFromServer.set(s);
                resultSignal.countDown();
                counter.incrementAndGet();
            });
        }

        for (int i = 0; i < 1000; i++) {
            resultSignal.await(100, TimeUnit.MILLISECONDS);
            clientMessageBus.process();
            if (i % 10 == 0) {
                System.out.println("COUNT " + counter.get());
            }
        }
        resultSignal.await(1, TimeUnit.SECONDS);

        assertEquals("Hello RICK", responseFromServer.get());

        assertEquals(10_000, counter.get());

        stopServerAndBridgeLoops();
    }


    @Test
    public void test10KMore() throws Exception {
        resultSignal = new CountDownLatch(10_000);
        serverStopped = new CountDownLatch(10_000);
        bridgeStopped = new CountDownLatch(10_000);
        final AtomicInteger counter = new AtomicInteger();

        runServerLoop();
        runBridgeLoop();


        for (int y = 0; y < 4; y++) {
            for (int i = 0; i < 2_500; i++) {
                clientMessageBus.request("RICK", s -> {
                    responseFromServer.set(s);
                    resultSignal.countDown();
                    counter.incrementAndGet();
                });
            }
            Thread.sleep(200);
        }

        for (int i = 0; i < 1000; i++) {
            resultSignal.await(100, TimeUnit.MILLISECONDS);
            clientMessageBus.process();
            if (i % 100 == 0) {
                System.out.println("COUNT " + counter.get());
                if (counter.get() == 10_000) {
                    break;
                }
            }
        }
        resultSignal.await(1, TimeUnit.SECONDS);

        assertEquals("Hello RICK", responseFromServer.get());

        assertEquals(10_000, counter.get());

        stopServerAndBridgeLoops();
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