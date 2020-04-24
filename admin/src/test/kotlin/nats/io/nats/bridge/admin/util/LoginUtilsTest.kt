package nats.io.nats.bridge.admin.util

import nats.io.nats.bridge.admin.models.logins.LoginToken
import nats.io.nats.bridge.admin.models.logins.Role
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class LoginUtilsTest {

    @Test
    fun createLoginFromMap() {

        val token = LoginToken("Rick Hightower", listOf(Role("Admin"), Role("User")))
        val map = token.toMap()
        assertEquals("Rick Hightower", map["subject"])
        assertEquals("Admin,User", map["roles"])

        val newToken = LoginUtils.createLoginFromMap(map)

        assertEquals(token, newToken)
    }
}