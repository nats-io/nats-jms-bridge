package io.nats.bridge.admin.model

import io.nats.bridge.admin.models.logins.LoginToken
import io.nats.bridge.admin.models.logins.Role
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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