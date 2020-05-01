package io.nats.bridge.admin.repos

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.nats.bridge.admin.LoginRepo
import io.nats.bridge.admin.RepoException
import io.nats.bridge.admin.models.logins.*
import io.nats.bridge.admin.util.EncryptUtils
import io.nats.bridge.admin.util.ObjectMapperUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime

class LoginRepoFromFiles(private val configFile: File = File("./config/nats-bridge-logins.yaml"),
                         private val mapper: ObjectMapper = ObjectMapperUtils.getYamlObjectMapper(),
                         private val systemSecret: String) : LoginRepo {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun init() {
        saveConfig(readConfig())
    }

    private fun readConfig(): LoginConfig {
        if (!configFile.exists()) {
            saveConfig(defaultLoginConfig)
            val backup = LoginRepoFromFiles(configFile = File(configFile.parentFile, "initial-nats-bridge-logins.yaml"), mapper = mapper, systemSecret = "")
            jacksonObjectMapper().writeValue(File(configFile.parentFile, "initial-nats-bridge-logins.json"), defaultLoginConfig)
            backup.saveConfig(defaultLoginConfig)
        }
        return mapper.readValue(configFile)
    }

    private fun saveConfig(conf: LoginConfig) {
        logger.info("Saving LoginConfig config... " + LocalDateTime.now())
        configFile.parentFile.mkdirs()

        val loginsToEncrypt = conf.logins.filter { it.secret.startsWith("sk-") }

        if (systemSecret.isBlank() || loginsToEncrypt.isEmpty()) {
            mapper.writeValue(configFile, conf)
        } else {
            val newLogins = conf.logins.map { login ->
                if (login.secret.startsWith("sk-")) {
                    val encryptKey = login.genKey(systemSecret)
                    val encrypt = EncryptUtils.createEncrypt(encryptKey)
                    login.copy(secret = encrypt.encrypt(login.secret))
                } else login
            }
            mapper.writeValue(configFile, conf.copy(logins = newLogins))
        }
    }

    override fun loadLogin(tokenRequest: LoginRequest): Login? {
        return readConfig().logins.find { it.subject == tokenRequest.subject && it.publicKey == tokenRequest.publicKey }
    }

    override fun addLogin(login: Login) {
        val loginConfig = readConfig()

        if (loginConfig.logins.find { it.subject == login.subject } != null) {
            throw RepoException("Login already exists ${login.subject}")
        }
        val newLoginConf = loginConfig.copy(logins = listOf(*loginConfig.logins.toTypedArray(), login))
        saveConfig(newLoginConf)
    }

    override fun removeLogin(subject: String) {
        val loginConfig = readConfig()
        if (loginConfig.logins.find { it.subject == subject } == null) {
            throw RepoException("Login does not exist $subject")
        }
        val newLoginConf = loginConfig.copy(logins = loginConfig.logins.filter { it.subject != subject })
        saveConfig(newLoginConf)
    }

    override fun addRoleToLogin(subject: String, role: String) {
        val loginConfig = readConfig()
        val login = loginConfig.logins.find { it.subject == subject }

        if (loginConfig.roles.find { it.name == role } == null) {
            throw RepoException("Unable to find role $role")
        }
        if (login != null) {
            val updatedLogin = login.copy(roles = listOf(*login.roles.toTypedArray(), Role(role)))
            val logins = loginConfig.logins.filter { it.subject != subject }.toTypedArray()
            val newLoginConf = loginConfig.copy(logins = listOf(*logins, updatedLogin))
            saveConfig(newLoginConf)
        } else {
            throw RepoException("Unable to find login with $subject")
        }

    }

    override fun removeRoleFromLogin(subject: String, role: String) {
        val loginConfig = readConfig()
        val login = loginConfig.logins.find { it.subject == subject }
        val updatedLogin = if (login != null) {
            val roleToRemove = login.roles.find { it.name == role }
            if (roleToRemove != null) {
                login.copy(roles = login.roles.filter { it.name != role })
            } else {
                throw RepoException("Login $subject did not have role $role to remove")
            }
        } else {
            throw RepoException("Login $subject not found to remove role $role")
        }
        val logins = loginConfig.logins.filter { it.subject != subject }.toTypedArray()
        val newLoginConf = loginConfig.copy(logins = listOf(*logins, updatedLogin))
        saveConfig(newLoginConf)
    }

    override fun listLogins(): List<String> {
        return readConfig().logins.map { it.subject }
    }


    override fun listLoginsWithRole(role: String): List<String> {
        return readConfig().logins.filter { it.roles.find { r -> r.name == role } != null }.map { it.subject }
    }

    override fun listRolesForLogin(subject: String): List<String> {
        return readConfig().logins.find { it.subject == subject }?.roles?.map { it.name } ?: emptyList()
    }

    override fun listSystemRoles(): List<String> {
        return readConfig().roles.map { it.name }
    }
}