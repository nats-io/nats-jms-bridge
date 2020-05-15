package io.nats.bridge.support;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.TestUtils;
import io.nats.bridge.messages.Message;
import io.nats.bridge.mock.MockMessageBus;
import io.nats.bridge.mock.MockMessageBusBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class MessageBridgeImplTest {



    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final AtomicReference<String> responseFromServer = new AtomicReference<>();
    private CountDownLatch resultSignal;
    private CountDownLatch serverStopped;
    private CountDownLatch bridgeStopped;

    private MockMessageBus serverMessageBus;
    private MockMessageBus clientMessageBus;
    private MockMessageBus bridgeMessageBusSource;
    private MockMessageBus bridgeMessageBusDestination;
    private MessageBridge messageBridge;

    @Before
    public void setUp() throws Exception {

        final BlockingQueue<Message> serverReplyOutQueue = new LinkedTransferQueue<>();

        clientMessageBus = MockMessageBusBuilder.builder().withName("CLIENT").build();
        bridgeMessageBusSource = MockMessageBusBuilder.builder().withReceiveQueue(clientMessageBus.getPublishQueue()).withName("SOURCE").build();
        bridgeMessageBusDestination = MockMessageBusBuilder.builder().withReceiveQueue(bridgeMessageBusSource.getPublishQueue()).withName("DESTINATION").build();

        serverMessageBus = MockMessageBusBuilder.builder()
                .withReceiveQueue(bridgeMessageBusDestination.getPublishQueue())
                .withReplyOutQueue(serverReplyOutQueue)
                .withName("SERVER").build();





        messageBridge =  MessageBridgeBuilder.builder().withName ("bridge").withDestinationBus(bridgeMessageBusDestination)
                .withSourceBus(bridgeMessageBusSource).withRequestReply(true).build();

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
            resultSignal.await(50, TimeUnit.MILLISECONDS);
            clientMessageBus.process();
            resultSignal.await(1, TimeUnit.SECONDS);
        }

        assertEquals("Hello RICK", responseFromServer.get());


        stopServerAndBridgeLoops();
    }



    @Test
    public void testMore() throws Exception {

        resultSignal = new CountDownLatch(100);
        serverStopped = new CountDownLatch(100);
        bridgeStopped = new CountDownLatch(100);

        AtomicInteger counter = new AtomicInteger();

        runServerLoop();
        runBridgeLoop();

        for (int i =0; i < 100; i++) {
            clientMessageBus.request("RICK", s -> {
                responseFromServer.set(s);
                resultSignal.countDown();
                counter.incrementAndGet();
            });
        }
        for (int i = 0; i < 10; i++) {
            resultSignal.await(50, TimeUnit.MILLISECONDS);
            clientMessageBus.process();
            resultSignal.await(1, TimeUnit.SECONDS);
        }

        assertEquals(100, clientMessageBus.getReqCount().getValue());
        assertEquals(100, counter.get());
        stopServerAndBridgeLoops();
    }


    @Test
    public void testEvenMore() throws Exception {

        resultSignal = new CountDownLatch(10_000);
        serverStopped = new CountDownLatch(10_000);
        bridgeStopped = new CountDownLatch(10_000);

        AtomicInteger counter = new AtomicInteger();

        runServerLoop();
        runBridgeLoop();

        for (int i =0; i < 10_000; i++) {
            clientMessageBus.request("RICK", s -> {
                responseFromServer.set(s);
                resultSignal.countDown();
                counter.incrementAndGet();
            });
        }
        for (int i = 0; i < 10_000; i++) {
            resultSignal.await(50, TimeUnit.MILLISECONDS);
            clientMessageBus.process();
        }
        resultSignal.await(1, TimeUnit.SECONDS);
        assertEquals(10_000, clientMessageBus.getReqCount().getValue());
        assertEquals(10_000, counter.get());
        stopServerAndBridgeLoops();
    }

    @Test
    public void testALotMore() throws Exception {

        resultSignal = new CountDownLatch(100_000);
        serverStopped = new CountDownLatch(100_000);
        bridgeStopped = new CountDownLatch(100_000);

        AtomicInteger counter = new AtomicInteger();

        runServerLoop();
        runBridgeLoop();

        for (int i =0; i < 100_000; i++) {
            clientMessageBus.request("RICK", s -> {
                responseFromServer.set(s);
                resultSignal.countDown();
                counter.incrementAndGet();
            });
        }
        for (int i = 0; i < 100_000; i++) {
            resultSignal.await(5, TimeUnit.MILLISECONDS);
            clientMessageBus.process();
        }
        resultSignal.await(1, TimeUnit.SECONDS);
        assertEquals(100_000, clientMessageBus.getReqCount().getValue());
        assertEquals(100_000, counter.get());
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

    @Test
    public void name() {

        MessageBridgeBuilder builder = MessageBridgeBuilder.builder();



    }

    @Test
    public void process() {
    }

    @Test
    public void testProcess() {
    }

    @Test
    public void close() {
    }

    @Test
    public void sourceMetrics() {
    }

    @Test
    public void destinationMetrics() {
    }
}