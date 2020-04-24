package nats.io.nats.bridge.admin


import nats.io.nats.bridge.admin.models.bridges.Cluster
import nats.io.nats.bridge.admin.models.bridges.MessageBridgeInfo
import nats.io.nats.bridge.admin.models.bridges.NatsBridgeConfig
import nats.io.nats.bridge.admin.models.logins.Login
import nats.io.nats.bridge.admin.models.logins.LoginRequest


class RepoException(message: String) : Exception(message)

interface ConfigRepo {
    fun readConfig(): NatsBridgeConfig

    fun readClusterConfigs(): Map<String, Cluster>

    fun addBridge(messageBridge: MessageBridgeInfo)

    fun saveConfig(conf: NatsBridgeConfig)
}


interface LoginRepo {

    fun loadLogin(tokenRequest: LoginRequest): Login?
    fun addLogin(login: Login)
    fun removeLogin(subject: String)
    fun addRoleToLogin(subject: String, role: String)
    fun removeRoleFromLogin(subject: String, role: String)
    fun listLogins(): List<String>
    fun listLoginsWithRole(role: String): List<String>
    fun listRolesForLogin(subject: String): List<String>
    fun listSystemRoles(): List<String>


}