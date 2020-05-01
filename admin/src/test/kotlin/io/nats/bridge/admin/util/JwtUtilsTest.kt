package io.nats.bridge.admin.util

import io.nats.bridge.admin.models.logins.LoginToken
import io.nats.bridge.admin.models.logins.Role
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class JwtUtilsTest {


    private val secretKey = "sk_2c637054-b8ec-46e4-9fcc-139e71abd4de-139e71abd4de-139e71abd4de-139e71abd4de-139e71abd4de-139e71abd4de"

    @Test
    fun generateToken() {

        val token = JwtUtils.generateToken("foo", mapOf("name" to "rick"), secretKey)

        val claims = JwtUtils.readClaims(token, secretKey)!!

        assertEquals("rick", claims["name"])

    }

    @Test
    fun generateActualToken() {

        val loginToken = LoginToken("Rick Hightower", listOf(Role("Admin"), Role("User")))
        val map = loginToken.toMap()
        val token = JwtUtils.generateToken("foo", map, secretKey)
        val claims = JwtUtils.readClaims(token, secretKey)!!
        assertEquals("Rick Hightower", claims["subject"])
        assertEquals("Admin,User", claims["roles"])

    }
}