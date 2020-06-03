package io.nats.bridge.examples.jms;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class SimpleNatsClient {

    public static void main(final String[] args)  {
        try {

            final Options.Builder builder = new Options.Builder().server("nats://localhost:4222");
            final Connection connect = Nats.connect(builder.build());

            final Message replyFromJmsServer = connect.request("request_reply_jms",
                    "Hello World!".getBytes(StandardCharsets.UTF_8), Duration.ofSeconds(20));

            if (replyFromJmsServer != null) {
                System.out.println("RESPONSE FROM SERVER " + new String(replyFromJmsServer.getData(), StandardCharsets.UTF_8));
            } else {
                System.out.println("No reply message sent from JMS server");
            }


        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
