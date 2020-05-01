package io.nats.bridge.admin.util

import io.nats.bridge.admin.models.logins.LoginToken
import io.nats.bridge.admin.models.logins.Role


object Roles {
    val roles = listOf(Role("Admin"), Role("User"))
    val roleSet = roles.toSet()
    val roleMap = roles.map { it.name to it }.toMap()
}

object LoginUtils {

    private fun extractRoles(roleString: String?): List<Role> {
        return roleString?.split(",")?.filter { !it.isBlank() }?.map { Role(it) }//?.filter { Roles.roleSet.contains(it) }
                ?: emptyList()
    }

    fun createLoginFromMap(map: Map<String, String>): LoginToken {
        return LoginToken(map["subject"]!!, extractRoles(map["roles"]))
    }
}