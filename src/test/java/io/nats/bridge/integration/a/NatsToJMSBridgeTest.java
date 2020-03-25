package io.nats.bridge.integration.a;

import io.nats.bridge.Message;
import io.nats.bridge.MessageBridge;
import io.nats.bridge.MessageBus;
import io.nats.bridge.StringMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class NatsToJMSBridgeTest {

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
        clientMessageBus = ServiceATestUtil.getMessageBusNats();
        serverMessageBus = ServiceATestUtil.getMessageBusJms();
        resultSignal = new CountDownLatch(1);
        serverStopped = new CountDownLatch(1);
        bridgeStopped = new CountDownLatch(1);

        bridgeMessageBusSource = ServiceATestUtil.getMessageBusNats();
        bridgeMessageBusDestination = ServiceATestUtil.getMessageBusJms();
        messageBridge = new MessageBridge(bridgeMessageBusSource, bridgeMessageBusDestination);

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

        resultSignal.await();

        assertEquals("Hello RICK", responseFromServer.get());


        stopServerAndBridgeLoops();
    }

    private void runBridgeLoop() {

        final Thread thread = new Thread(() -> {

            try {
                while (!stop.get()) {
                    Thread.sleep(10);
                    messageBridge.process();
                }
                messageBridge.close();
                bridgeStopped.countDown();
            } catch (Exception ex) {
                ex.printStackTrace();
            }


        });


        thread.start();
    }

    private void stopServerAndBridgeLoops() throws Exception{
        stop.set(true);
        serverStopped.await();
        bridgeStopped.await();
    }

    private void runServerLoop() {

        final Thread thread = new Thread(() -> {
            while (true) {
                if (stop.get()) {
                    serverMessageBus.close();
                    break;
                }
                final Optional<Message> receive = serverMessageBus.receive();
                receive.ifPresent(message -> {

                    StringMessage stringMessage = (StringMessage) message;
                    System.out.println("Handle message " + stringMessage.getBody());
                    message.reply(new StringMessage("Hello " + stringMessage.getBody()));
                });


                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                serverMessageBus.process();
            }

            serverStopped.countDown();



        });

        thread.start();

    }
}