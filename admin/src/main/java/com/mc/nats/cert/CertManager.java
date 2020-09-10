/**
 *  * 
 *   */
package com.mc.nats.cert;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import org.apache.commons.lang.RandomStringUtils;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Base64;

/**
 *  * @author e060428
 *   *
 *    */
public class CertManager {

	final private static Logger LOG = LoggerFactory.getLogger(CertManager.class);

	public static final String KEYSTORE = "KEYSTORE";
	public static final String KEYSTORE_PASSWORD = "KEYSTORE_PASSWORD";
	public static final String TRUSTSTORE = "TRUSTSTORE";
	public static final String TRUSTSTORE_PASSWORD = "TRUSTSTORE_PASSWORD";
	static final String JKS_LOCATION_VAR = "JKS_ROOT_DIRECTORY";
	static final String JKS = "JKS";
	static final String X509 = "X.509";
	static final String JAVA_KEYSTORE_PROPERTY = "javax.net.ssl.keyStore";
	static final String JAVA_KEYSTORE_PASSWORD_PROPERTY = "javax.net.ssl.keyStorePassword";
	static final String JAVA_TRUSTSTORE_PROPERTY = "javax.net.ssl.trustStore";
	static final String JAVA_TRUSTSTORE_PASSWORD_PROPERTY = "javax.net.ssl.trustStorePassword";


	public static void setJKSKeystore(String envVar, String javaProperty, String passwordProperty, String javaPasswordProperty) {
		String encodedJks = System.getenv(envVar);

		if (null == encodedJks) {
			LOG.info("No " + envVar + " environment variable defined");
		} else {
			LOG.info("Encoded JKS is [{}]",encodedJks);
			String dir = getDirforJKS();
			byte[] jks = Base64.getDecoder().decode(encodedJks);

			try {
				String location = Paths.get(dir, envVar.toLowerCase()) + ".jks";
				OutputStream stream = new FileOutputStream(location);
				Throwable var9 = null;

				try {
					stream.write(jks);
				} catch (Throwable var19) {
					var9 = var19;
					throw var19;
				} finally {
					if (stream != null) {
						if (var9 != null) {
							try {
								stream.close();
							} catch (Throwable var18) {
								var9.addSuppressed(var18);
							}
						} else {
							stream.close();
						}
					}

				}

				System.setProperty(javaProperty, location);
				LOG.info("Created JKS at {}", location);
				String pass = new String(Base64.getDecoder().decode(System.getenv(passwordProperty)));
				LOG.info("*** pass: " + pass);
				if (!StringUtils.isEmpty(pass)) {
					System.setProperty(javaPasswordProperty, pass);
					LOG.info("set password property {}", javaPasswordProperty);
				}
			} catch (IOException var21) {
				LOG.error("Couldn't write JKS file", var21);
			}

		}
	}

	private static String getDirforJKS() {
		String dir = System.getenv("JKS_ROOT_DIRECTORY");
		if (StringUtils.isEmpty(dir)) {
			dir = "/home/vcap/";
		}

		return dir;
	}

	public static void setJKSTruststore(String envVarWithLocOfCerts, String javaProperty, String javaPasswordProperty) {
		String locationofCerts = System.getenv(envVarWithLocOfCerts);
		if (StringUtils.isEmpty(locationofCerts)) {
			LOG.info("No " + envVarWithLocOfCerts + " environment variable defined");
		} else {
			LOG.info("location of Certs is [{}]",locationofCerts);
			String jksDir = getDirforJKS();
			//String randomTruststorePass = RandomStringUtils.randomAlphabetic(BigInteger.TEN.intValue());
			String randomTruststorePass = "password";
			KeyStore ks = null;

			try {
				ks = KeyStore.getInstance("JKS");
				ks.load((InputStream)null, (char[])null);
				FileInputStream fis = new FileInputStream(locationofCerts);
				BufferedInputStream bis = new BufferedInputStream(fis);
				CertificateFactory cf = null;
				cf = CertificateFactory.getInstance("X.509");
				Certificate cert = null;

				while(bis.available() > 0) {
					cert = cf.generateCertificate(bis);
					ks.setCertificateEntry(String.valueOf(bis.available()), cert);
				}

				ks.setCertificateEntry(String.valueOf(bis.available()), cert);
				String location = Paths.get(jksDir) + "/" + "TRUSTSTORE".toLowerCase() + ".jks";
				ks.store(new FileOutputStream(location), randomTruststorePass.toCharArray());
				System.setProperty(javaProperty, location);
				LOG.info("Created JKS at {}", location);
				System.setProperty(javaPasswordProperty, randomTruststorePass);
				LOG.info("set password property {}", javaPasswordProperty);
			} catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | IOException var12) {
				LOG.error("Couldn't write JKS file", var12);
			}
		}
	}
}

