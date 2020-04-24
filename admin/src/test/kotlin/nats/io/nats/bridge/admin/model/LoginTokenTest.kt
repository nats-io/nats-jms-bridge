package nats.io.nats.bridge.admin.model

import nats.io.nats.bridge.admin.models.logins.LoginToken
import nats.io.nats.bridge.admin.models.logins.Role
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class LoginTokenTest {

    @Test
    fun toMap() {

        val token = LoginToken("Rick Hightower", listOf(Role("Admin"), Role("User")))

        val map = token.toMap()
        println(map)
        assertEquals("Rick Hightower", map["subject"])
        assertEquals("Admin,User", map["roles"])

    }
}