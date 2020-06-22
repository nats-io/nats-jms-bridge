#!/bin/sh
set -e -i

CERT_PATH=${CERT_PATH:-./certs}
P12=${P12:-keystore.p12}
P12PASS=${P12PASS:-cloudurable0}
CERTFILE=${CERTFILE:-localhost.pem}
CERTFILEKEY=${CERTFILEKEY:-localhost-key.pem}
KNAME=${KNAME:-keystore.jks}
KEY_PASSCODE=${KEY_PASSCODE:-cloudurable1}
ROOTCA=${ROOTCA:-localhost.pem}
TRUST_PASSCODE=${TRUST_PASSCODE:-cloudurable2}
TNAME=${TNAME:-truststore.jks}

# Java SSLUtils requires certificates to be in the java keystore format (.jks).
# To do that you need JDK installed and use openssl to generate a pkcs12 file (.p12) using the certificate file (localhost.pem) and the certificate key (localhost-key.pem).
# The resulting file is then imported int a java keystore named keystore.jks using keytool which is part of java jdk.
# keytool is also used to import the CA certificate rootCA.pem into truststore.jks.

echo "Generating keystore.p12 file......"
openssl pkcs12 -export -out $CERT_PATH/$P12 -inkey $CERT_PATH/$CERTFILEKEY -in $CERT_PATH/$CERTFILE -password pass:$P12PASS
echo "File generated"
echo ".............."
echo "Generating keystore.jks file......"
keytool -importkeystore -srcstoretype PKCS12 -srckeystore $CERT_PATH/$P12 -srcstorepass $P12PASS -destkeystore $CERT_PATH/$KNAME -deststorepass $KEY_PASSCODE
echo "File generated"
echo ".............."
echo "Generating truststore.jks file......"
keytool -importcert -trustcacerts -file $CERT_PATH/$ROOTCA -storepass $TRUST_PASSCODE -noprompt -keystore $CERT_PATH/$TNAME
echo "File generated"
