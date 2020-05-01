package io.nats.bridge.admin.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.*


internal class EncryptionFactoryTest {

    @Test
    fun testEncrypt() {
        val secretKey = UUID.randomUUID().toString()
        val encrypt = EncryptUtils.createEncrypt(secretKey)
        val originalString = "Rick Hightower"
        val encryptedString = encrypt.encrypt(originalString)
        assertNotEquals(originalString, encryptedString)
        val decryptedString = encrypt.decrypt(encryptedString)
        assertEquals(originalString, decryptedString)
    }


}