package io.nats.bridge.tls;

import io.nats.bridge.MessageBus;

import io.nats.bridge.nats.support.NatsMessageBusBuilder;
import org.junit.Test;

public class NatsTlsCrtFile {


    @Test
    public  void test(){
        SslContextBuilder sslContext = new SslContextBuilder();
        sslContext.withAlgorithm("SunX509");
        sslContext.withKeystorePath("../certs/keystore.crt");
        sslContext.withTruststorePath("../certs/truststore.jks");
        sslContext.withKeyPassword("cloudurable1");
        sslContext.withStorePassword("cloudurable2");


        NatsMessageBusBuilder natsMessageBusBuilder = new NatsMessageBusBuilder();
        natsMessageBusBuilder.withName("natsCluster");
        natsMessageBusBuilder.withUseTls(true);
        natsMessageBusBuilder.withSubject("a1-subject");
        natsMessageBusBuilder.withQueueGroup("exampleGroup");
        natsMessageBusBuilder.withSslContext(sslContext.build());
        natsMessageBusBuilder.getOptionsBuilder().server("nats://localhost:4222");
        natsMessageBusBuilder.getOptionsBuilder().noReconnect();

        MessageBus messageBus = natsMessageBusBuilder.build();

        System.out.println("Connect using CRT Cert - Good bye");
        messageBus.close();
    }

    //@Test Need a certain cert docker container for this to work.
    public  void testAlias(){
        SslContextBuilder sslContext = new SslContextBuilder();
        sslContext.withAlgorithm("SunX509");
        sslContext.withKeystorePath("../certs/keyalias/keystore.jks");
        sslContext.withKeyStoreAlias("mamatus-cert");
        sslContext.withTruststorePath("../certs/keyalias/truststore.jks");
        sslContext.withKeyPassword("password");
        sslContext.withStorePassword("cloudurable2");
        NatsMessageBusBuilder natsMessageBusBuilder = new NatsMessageBusBuilder();
        natsMessageBusBuilder.withName("natsCluster");
        natsMessageBusBuilder.withUseTls(true);
        natsMessageBusBuilder.withSubject("a1-subject");
        natsMessageBusBuilder.withQueueGroup("exampleGroup");
        natsMessageBusBuilder.withSslContext(sslContext.build());
        natsMessageBusBuilder.getOptionsBuilder().server("nats://localhost:4222");
        natsMessageBusBuilder.getOptionsBuilder().noReconnect();

        MessageBus messageBus = natsMessageBusBuilder.build();

        System.out.println("Connect using Alias - " + sslContext.getKeyStoreAlias());
        messageBus.close();
    }
}
