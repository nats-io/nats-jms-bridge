package com.mc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.mc.nats.cert.CertManager;

//import io.nats.bridge.admin.Application;

@SpringBootApplication
public class MqBridgeWrapperApplication {
    
        public static void main(String args[]) {
                CertManager.setJKSKeystore("KEYSTORE", "javax.net.ssl.keyStore", "KEYSTORE_PASSWORD", "javax.net.ssl.keyStorePassword");
                CertManager.setJKSTruststore("TRUSTSTORE", "javax.net.ssl.trustStore", "javax.net.ssl.trustStorePassword");
    
                SpringApplication.run(MqBridgeWrapperApplication.class, args);
                System.out.println("##Starting the bridge##");
                io.nats.bridge.admin.ApplicationMain.main(args);
        }   
}
