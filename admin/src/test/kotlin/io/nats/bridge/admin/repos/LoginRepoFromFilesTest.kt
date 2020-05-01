package io.nats.bridge.admin.repos

import io.nats.bridge.admin.models.logins.Login
import io.nats.bridge.admin.models.logins.Role
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

internal class LoginRepoFromFilesTest {

    var repo = LoginRepoFromFiles(systemSecret = "")
    var file = File("./data/logins-" + UUID.randomUUID().toString() + "+.yaml")


    @BeforeEach
    fun before() {
        file = File("./data/logins-" + UUID.randomUUID().toString() + "+.yaml")
        repo = LoginRepoFromFiles(file, systemSecret = "")

        repo.addLogin(Login("Rick Hightower", roles = listOf(Role("Admin")),
                secret = "sk-" + UUID.randomUUID().toString(), publicKey = "pk-" + UUID.randomUUID().toString()))

        repo.addLogin(Login("Paul Hix", roles = listOf(Role("Admin")),
                secret = "sk-" + UUID.randomUUID().toString(), publicKey = "pk-" + UUID.randomUUID().toString()))

        repo.addLogin(Login("Sam Hix", roles = listOf(Role("User")),
                secret = "sk-" + UUID.randomUUID().toString(), publicKey = "pk-" + UUID.randomUUID().toString()))

        repo.addLogin(Login("Joe Hix", roles = listOf(Role("User")),
                secret = "sk-" + UUID.randomUUID().toString(), publicKey = "pk-" + UUID.randomUUID().toString()))
    }

    @AfterEach
    fun after() {
        file.delete()
    }

    @Test
    fun addLogin() {

        repo.addLogin(Login("John Doe", roles = listOf(Role("Admin")),
                secret = "sk-" + UUID.randomUUID().toString(), publicKey = "pk-" + UUID.randomUUID().toString()))

        assertTrue(repo.listLogins().find { it == "John Doe" } != null)
        assertTrue(repo.listRolesForLogin("John Doe").find { it == "Admin" } != null)
    }

    @Test
    fun removeLogin() {
        assertTrue(repo.listLogins().find { it == "Rick Hightower" } != null)
        repo.removeLogin("Rick Hightower")
        assertTrue(repo.listLogins().find { it == "Rick Hightower" } == null)
    }

    @Test
    fun addRoleToLogin() {
        repo.addRoleToLogin("Rick Hightower", "User")
        assertTrue(repo.listRolesForLogin("Rick Hightower").find { it == "User" } != null)
        assertTrue(repo.listRolesForLogin("Rick Hightower").find { it == "Admin" } != null)
    }

    @Test
    fun removeRoleFromLogin() {
        repo.removeRoleFromLogin("Rick Hightower", "Admin")
        assertTrue(repo.listRolesForLogin("Rick Hightower").find { it == "User" } == null)
        assertTrue(repo.listRolesForLogin("Rick Hightower").find { it == "Admin" } == null)
    }


    @Test
    fun listLoginsWithRole() {

        val admins = repo.listLoginsWithRole("Admin").toSet()
        val users = repo.listLoginsWithRole("User").toSet()

        assertEquals(3, admins.size)
        assertEquals(2, users.size)

        assertTrue(admins.contains("Rick Hightower"))
        assertTrue(!users.contains("Rick Hightower"))

    }

}