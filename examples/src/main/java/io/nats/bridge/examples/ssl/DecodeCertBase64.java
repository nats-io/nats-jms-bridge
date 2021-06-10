package io.nats.bridge.examples.ssl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DecodeCertBase64 {

    public static void main(String[] args) throws Exception {

        final File binFile = new File("C:\\nats\\nats-jms-bridge\\certs\\keystore.jks");
        final FileInputStream inputStream = new FileInputStream(binFile);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[1024];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        byte[] bytes = buffer.toByteArray();

        byte[] encodedBytes = Base64.getEncoder().encode(bytes);

        System.out.println(new String(encodedBytes, StandardCharsets.UTF_8));

        byte[] decodedBytes = Base64.getDecoder().decode(encodedBytes);


        if (bytes.length != decodedBytes.length) {
            throw new IllegalStateException("Did not match");
        }

        for (int index = 0; index < bytes.length; index++) {
            if (decodedBytes[index] != bytes[index]) {
                System.out.println("Index " + index + " not equal ");
                throw new IllegalStateException("Did not match " + decodedBytes[index] + "!=" + bytes[index] + " at " + index);
            }
        }

        System.out.println("Equal");
        inputStream.close();

    }
}

