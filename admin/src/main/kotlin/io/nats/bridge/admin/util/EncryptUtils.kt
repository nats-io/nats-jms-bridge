package io.nats.bridge.admin.util

import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


object EncryptUtils {
    fun createSecretKey(myKey: String): SecretKeySpec {
        val sha = MessageDigest.getInstance("SHA-1")
        val key = Arrays.copyOf(sha.digest(myKey.toByteArray()), 16)
        return SecretKeySpec(key, "AES")
    }

    fun createEncrypt(secret: String) = Encrypt(createSecretKey(secret))


}


class Encrypt(private val secretKey: SecretKeySpec, val cipherAlgorithm: String = "AES/ECB/PKCS5Padding") {


    fun encrypt(message: String): String {
        val cipher = Cipher.getInstance(cipherAlgorithm)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return Base64.getEncoder().encodeToString(cipher.doFinal(message.toByteArray(charset("UTF-8"))))
    }

    fun decrypt(strToDecrypt: String?): String {
        val cipher = Cipher.getInstance(cipherAlgorithm)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)))
    }
}