package io.nats.bridge.admin.models.logins

import java.util.*

data class Role(val name: String)
data class Login(val subject: String, val secret: String,
                 val publicKey: String, val roles: List<Role>) {
    fun genKey(systemSecret: String): String {
        return "secured-$systemSecret-nats-bridge-pub-$publicKey-subject-$subject-abcdefjhijklmnopqrstuvwxyz"
    }

    fun genToken(): LoginToken {
        val cp = this.copy()
        return LoginToken(cp.subject, cp.roles)
    }
}

data class LoginConfig(val logins: List<Login>, val roles: List<Role>)


data class LoginToken(val subject: String, val roles: List<Role>) {
    fun toMap(): Map<String, String> {
        return mapOf("subject" to subject, "roles" to roles.map { it.name }.joinToString(","))
    }
}

data class LoginRequest(val subject: String, val publicKey: String, val secret: String)

data class TokenResponse(val token: String, val publicKey: String, val subject: String)

val defaultLoginConfig = LoginConfig(
        roles = listOf(Role("Admin"), Role("User")),
        logins = listOf(Login(subject = "admin", secret = "sk-" + UUID.randomUUID().toString(),
                publicKey = "pk-" + UUID.randomUUID().toString(), roles = listOf(Role("Admin"))
        ))
)
