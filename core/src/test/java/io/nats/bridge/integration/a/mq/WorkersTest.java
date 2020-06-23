package io.nats.bridge.integration.a.mq;

import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBridgeTasksManager;
import io.nats.bridge.MessageBus;
import io.nats.bridge.TestUtils;
import io.nats.bridge.messages.Message;
import io.nats.bridge.messages.MessageBuilder;

import io.nats.bridge.support.MessageBridgeImpl;
import io.nats.bridge.task.MessageBridgeTasksManagerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class WorkersTest {

    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final AtomicReference<String> responseFromServer = new AtomicReference<>();
    private final AtomicReference<String> responseHeaderFromServer = new AtomicReference<>();
    private CountDownLatch resultSignal;
    private CountDownLatch serverStopped;
    private CountDownLatch bridgeStopped;

    private MessageBus serverJMSMessageBus;
    private MessageBus clientMessageNatsBus;

    private MessageBridgeTasksManager manager;
    @Before
    public void setUp() throws Exception {
        clientMessageNatsBus = TestUtils.getMessageBusNats("CLIENT","A");
        serverJMSMessageBus = TestUtils.getMessageBusIbmMQWithHeaders4("SERVER",true);
        resultSignal = new CountDownLatch(1);
        serverStopped = new CountDownLatch(1);
        bridgeStopped = new CountDownLatch(1);

        final MessageBridgeTasksManagerBuilder builder = MessageBridgeTasksManagerBuilder.builder();

        manager = builder.withBridgeFactory(s -> {
            try {
                final MessageBus bridgeMessageBusNatsSource = TestUtils.getMessageBusNats("BRIDGE_SOURCE", "A");
                final MessageBus bridgeMessageBusJmsDestination = TestUtils.getMessageBusIbmMQWithHeaders4("BRIDGE_DEST", false);

                final MessageBridge messageBridge = new MessageBridgeImpl("", bridgeMessageBusNatsSource, bridgeMessageBusJmsDestination, true, null, Collections.emptyList());
                return messageBridge;
            }catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }).withName("TEST_BRIDGE_TASK_RUNNER")
                .withPollDuration(Duration.ofMillis(50)).withTasks(1).withWorkers(1).build();


        manager.start();


    }

    @After
    public void tearDown() throws Exception {
        manager.close();
    }

    @Test
    public void test() throws Exception {


        runServerLoop();


        final Message message = MessageBuilder.builder().withHeader("MY_HEADER", "MY_VALUE").withBody("RICK").build();

        clientMessageNatsBus.request(message, reply -> {

            responseHeaderFromServer.set((String) reply.headers().get("MY_HEADER"));
            responseFromServer.set(reply.bodyAsString());
            resultSignal.countDown();
        });

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runClientLoop();
                }catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        thread.start();


        resultSignal.await(10, TimeUnit.SECONDS);
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


    private void stopServerAndBridgeLoops() throws Exception {
        TestUtils.stopServerAndBridgeLoops(stop, serverStopped, bridgeStopped);
    }

    private void runServerLoop() {
        TestUtils.runServerLoop(stop, serverJMSMessageBus, serverStopped);
    }
}