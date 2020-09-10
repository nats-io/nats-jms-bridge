package com.mc;

import com.mc.nats.cert.CertManager;

public class MqBridgeWrapperApplication {
	public static void main(String args[]) {
		CertManager.setJKSKeystore("KEYSTORE", "javax.net.ssl.keyStore", "KEYSTORE_PASSWORD", "javax.net.ssl.keyStorePassword");
        CertManager.setJKSTruststore("TRUSTSTORE", "javax.net.ssl.trustStore", "javax.net.ssl.trustStorePassword");
		io.nats.bridge.admin.ApplicationMain.main(args);
	}

}
