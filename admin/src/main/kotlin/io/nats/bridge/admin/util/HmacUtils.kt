package io.nats.bridge.admin.util

import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacUtils {
    fun hmac(secret: String, message: String, algorithm: String = "HmacSHA256"): String {
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(secret.toByteArray(), algorithm))
        return Base64.getEncoder().encodeToString(mac.doFinal(message.toByteArray()))
    }
}