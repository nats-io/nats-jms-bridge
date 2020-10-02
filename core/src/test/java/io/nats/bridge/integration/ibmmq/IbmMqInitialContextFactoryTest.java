package io.nats.bridge.integration.ibmmq;

import org.jgroups.util.Base64;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class IbmMqInitialContextFactoryTest {


    @Test
    public void test() throws Exception {

        Hashtable<String, String> jndiProperties = new Hashtable<>();
        Map<String, String> env = new HashMap<>();
        Properties systemProps = new Properties();

        String keyStorePass = "mommy";
        String keyStorePassBase64Encoded = Base64.encodeBytes(keyStorePass.getBytes(StandardCharsets.UTF_8));
        jndiProperties.put(IbmMqInitialContextFactory.KEY_STORE_PASS_BASE64_ENV, "MY_KEY");
        env.put("MY_KEY", keyStorePassBase64Encoded);

        String trustStorePass = "daddy";
        String trustStorePassBase64Encoded = Base64.encodeBytes(trustStorePass.getBytes(StandardCharsets.UTF_8));
        jndiProperties.put(IbmMqInitialContextFactory.TRUST_STORE_PASS_BASE64_ENV, "MY_TRUST");
        env.put("MY_TRUST", trustStorePassBase64Encoded);


        IbmMqInitialContextFactory.initSSL(jndiProperties, systemProps, env);

        assertEquals(keyStorePass, systemProps.get("javax.net.ssl.keyStorePassword"));
        assertEquals(trustStorePass, systemProps.get("javax.net.ssl.trustStorePassword"));
    }



    @Test
    public void testDefaults() throws Exception {

        Hashtable<String, String> jndiProperties = new Hashtable<>();
        Map<String, String> env = new HashMap<>();
        Properties systemProps = new Properties();

        String keyStorePass = "mommy";
        String keyStorePassBase64Encoded = Base64.encodeBytes(keyStorePass.getBytes(StandardCharsets.UTF_8));
        env.put("KEYSTORE_PASSWORD", keyStorePassBase64Encoded);

        String trustStorePass = "daddy";
        String trustStorePassBase64Encoded = Base64.encodeBytes(trustStorePass.getBytes(StandardCharsets.UTF_8));
        env.put("TRUSTSTORE_PASSWORD", trustStorePassBase64Encoded);


        IbmMqInitialContextFactory.initSSL(jndiProperties, systemProps, env);

        assertEquals(keyStorePass, systemProps.get("javax.net.ssl.keyStorePassword"));
        assertEquals(trustStorePass, systemProps.get("javax.net.ssl.trustStorePassword"));
    }

}