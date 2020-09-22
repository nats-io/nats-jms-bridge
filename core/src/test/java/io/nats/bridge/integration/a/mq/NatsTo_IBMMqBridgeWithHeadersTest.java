package io.nats.bridge.integration.a.mq;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.TestUtils;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;
import io.nats.bridge.support.MessageBridgeImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class NatsTo_IBMMqBridgeWithHeadersTest {

    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final AtomicReference<String> responseFromServer = new AtomicReference<>();
    private final AtomicReference<String> responseHeaderFromServer = new AtomicReference<>();
    private CountDownLatch resultSignal;
    private CountDownLatch serverStopped;
    private CountDownLatch bridgeStopped;

    private MessageBus serverJMSMessageBus;
    private MessageBus clientMessageNatsBus;
    private MessageBus bridgeMessageBusNatsSource;
    private MessageBus bridgeMessageBusJmsDestination;
    private MessageBridge messageBridge;

    @Before
    public void setUp() throws Exception {
        clientMessageNatsBus = TestUtils.getMessageBusNats("CLIENT","A");
        serverJMSMessageBus = TestUtils.getMessageBusIbmMQWithHeaders4("SERVER",true);
        //serverJMSMessageBus = TestUtils.getMessageBusIbmMQWithHeaders2("SERVER");
        resultSignal = new CountDownLatch(1);
        serverStopped = new CountDownLatch(1);
        bridgeStopped = new CountDownLatch(1);

        bridgeMessageBusNatsSource = TestUtils.getMessageBusNats("BRIDGE_SOURCE","A");
        bridgeMessageBusJmsDestination = TestUtils.getMessageBusIbmMQWithHeaders4("BRIDGE_DEST",false);
        //bridgeMessageBusJmsDestination = TestUtils.getMessageBusIbmMQWithHeaders2("BRIDGE_DEST");
        messageBridge = new MessageBridgeImpl("", bridgeMessageBusNatsSource, bridgeMessageBusJmsDestination, true, null, Collections.emptyList(), Collections.emptyList());

    }


    @After
    public void tearDown() throws Exception {
    }

    private void drainClientLoop() throws Exception {
        TestUtils.drainBus(clientMessageNatsBus);
    }

    @Test
    public void test() throws Exception {

        TestUtils.drainBus(serverJMSMessageBus);
        drainClientLoop();
        runServerLoop();
        runBridgeLoop();

        final Message message = MessageBuilder.builder().withHeader("MY_HEADER", "MY_VALUE").withBody("RICK").build();

        clientMessageNatsBus.request(message, reply -> {

            responseHeaderFromServer.set((String) reply.headers().get("MY_HEADER"));
            responseFromServer.set(reply.bodyAsString());
            resultSignal.countDown();
        });


        runClientLoop();
        assertEquals("Hello RICK MY_HEADER MY_VALUE", responseFromServer.get());

        System.out.println(responseFromServer.get());


        stopServerAndBridgeLoops();
    }

    private void runClientLoop() throws Exception {

        for (int index = 0; index < 100; index++) {

            resultSignal.await(10, TimeUnit.MILLISECONDS);
            clientMessageNatsBus.process();

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
        TestUtils.runServerLoop(stop, serverJMSMessageBus, serverStopped);
    }
}