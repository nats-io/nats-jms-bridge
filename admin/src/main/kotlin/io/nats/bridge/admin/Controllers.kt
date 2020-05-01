package io.nats.bridge.admin


import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.MeterRegistry
import io.swagger.annotations.ApiImplicitParam
import io.nats.bridge.admin.importer.BridgeFileDelimImporter
import io.nats.bridge.admin.importer.BridgeFileImporter
import io.nats.bridge.admin.models.bridges.MessageBridgeInfo
import io.nats.bridge.admin.models.bridges.NatsBridgeConfig
import io.nats.bridge.admin.models.logins.Login
import io.nats.bridge.admin.models.logins.LoginRequest
import io.nats.bridge.admin.models.logins.LoginToken
import io.nats.bridge.admin.models.logins.TokenResponse
import io.nats.bridge.admin.runner.BridgeRunnerManager
import io.nats.bridge.admin.util.EncryptUtils
import io.nats.bridge.admin.util.JwtUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

data class Flag(val message: String, val flag: Boolean)
data class Error(val name: String, val message: String, val root:String?=null)
data class ResponseMessage(val message: String, val error: Error? = null)

@RestController
@RequestMapping("/api/v1/login")
class LoginController(@Value("\${security.secretKey}") private val adminSecretKey: String,
                      @Value("\${jwt.algo}") private val jwtAlgorithm: String,
                      private val longRepo: LoginRepo,
                      registry: MeterRegistry) {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val generateTokenCount = registry.counter("token_count", "module", "security")
    private val tokenErrorCount = registry.counter("token_error", "module", "security")


    @PostMapping("/generateToken")
    @Timed
    @ApiImplicitParam(name = "Content-Type", value = "application/json", dataType = "string", paramType = "header")
    fun generateToken(@RequestHeader headers: Map<String, String>, @RequestBody tokenRequest: LoginRequest) = doGenerateToken(headers, tokenRequest)


    fun doGenerateToken(headers: Map<String, String>, tokenRequest: LoginRequest): TokenResponse {

        val authLogin = longRepo.loadLogin(tokenRequest)
        if (authLogin != null) {
            val pwd: String = if (authLogin.secret.startsWith("pk-")) authLogin.secret else {
                logger.warn("Read auth Login that was not encrypted")
                val encryptUtils = EncryptUtils.createEncrypt(authLogin.genKey(adminSecretKey))
                encryptUtils.decrypt(authLogin.secret)
            }
            if (tokenRequest.secret == pwd) {
                val token = JwtUtils.generateToken("LOGIN_TOKEN", authLogin.genToken().toMap(),
                        adminSecretKey + adminSecretKey, jwtAlgorithm)
                generateTokenCount.increment()
                return TokenResponse(token, authLogin.publicKey, authLogin.subject)
            } else {
                tokenErrorCount.increment()
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Bad Token Request")
            }
        } else {
            tokenErrorCount.increment()
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Login not found")
        }
    }


}

@RestController
@RequestMapping("/")
class RootController(registry: MeterRegistry) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val pingCounter = registry.counter("ping_count", "module", "root")


    @GetMapping(path = ["/ping"])
    @Timed
    fun ping(): String {
        pingCounter.increment()
        logger.info("Ping was called at " + Date())
        return "pong"
    }

    @RequestMapping(value = ["/"], produces = ["text/html"])
    fun index(): String? {
        return """<html><body><H1>NATS JMS BRIDGE ADMIN</H1> <p><a href="./swagger-ui.html#!">Click Here</a></p></body></html>"""
    }

}

@RestController
@RequestMapping("/api/v1/util")
class UtilController {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping(path = ["/ping"])
    fun ping() = "pong"

    @GetMapping(path = ["/"])
    fun root() = "Nats JMS Bridge is running"
}

@RestController
@RequestMapping("/api/v1/auth")
class AuthUtilController {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping(path = ["/ping"])
    fun authPing(authentication: Authentication?) = authentication?.details as LoginToken?
}


@RestController
@RequestMapping("/api/v1/bridges")
class AdminController(private val config: ConfigRepo) {
    private val logger = LoggerFactory.getLogger(this.javaClass)


    @PreAuthorize("hasAnyAuthority('Admin')")
    @GetMapping(path = ["/admin/config"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun showConfig(authentication: Authentication) = config.readConfig()

    @PreAuthorize("hasAnyAuthority('Admin')")
    @PutMapping(path = ["/admin/config/bridge"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun addBridge(messageBridge: MessageBridgeInfo) = config.addBridge(messageBridge)

    @PreAuthorize("hasAnyAuthority('Admin')")
    @PostMapping(path = ["/admin/config"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun saveConfig(conf: NatsBridgeConfig) = config.saveConfig(conf)
    

    @PreAuthorize("hasAnyAuthority('Admin')")
    @PutMapping(path = ["/admin/config/import/bridges"], consumes=["text/tsv", "text/csv"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun importBridges(@RequestBody text: String, @RequestParam(name="name") name: String, @RequestParam(name="delim") delim: String="tab",
                      @Value("\${app.config.etcd}") configDir:String) : ResponseMessage {

        return try {
            val fileName = (name + Date()).replace("/", "_").replace("..", "_").replace(" ", "_").replace(":", "_")
            val ext = if (delim == "tab") "tsv" else if (delim == "comma" || delim == ",") "csv" else "_sv"
            val actualDelim = if (delim == "tab") "\t" else if (delim == "comma") "," else delim
            val outputFile = File(File(configDir), "$fileName.$ext")
            println(text.split("###___").map{it.trim()}.filter { !it.isBlank() }.joinToString ("\r\n" ))
            outputFile.writeText(text.split("###___").filter { !it.isBlank() }.joinToString ("\r\n"))
            val importer = BridgeFileDelimImporter(configRepo = config, delim = actualDelim)
            importer.import(outputFile)
            ResponseMessage("All Good")
        } catch (ex:Exception) {
            ResponseMessage("Failed to upload $name", error = io.nats.bridge.admin.Error(ex.javaClass.simpleName,
                    ex.localizedMessage, ex.cause?.localizedMessage))
        }

    }

}

@RestController
@RequestMapping("/api/v1/logins")
class UserAdminController(private val loginRepo: LoginRepo, registry: MeterRegistry) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val loginCount = AtomicInteger()
    private val loginsLevel = registry.gauge("login_level", loginCount);

    @PostMapping(path = ["/admin/login"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun addLogin(login: Login) {
        logger.info("New Login Added ${login.subject}")
        loginRepo.addLogin(login)
    }

    @DeleteMapping(path = ["/admin/login"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun removeLogin(@RequestParam subject: String) {
        logger.info("Login Removed $subject")
        loginRepo.removeLogin(subject)
    }

    @PutMapping(path = ["/admin/login/role"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun addRoleToLogin(@RequestParam subject: String, @RequestParam role: String) = loginRepo.addRoleToLogin(subject, role)

    @DeleteMapping(path = ["/admin/login/role"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun removeRoleFromLogin(@RequestParam subject: String, @RequestParam role: String) = loginRepo.removeRoleFromLogin(subject, role)

    @GetMapping(path = ["/admin/login"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun listLogins(): List<String>  {
        val logins =  loginRepo.listLogins()
        loginCount.set(logins.size)
        return logins;
    }

    @GetMapping(path = ["/admin/login/by/role"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun listLoginsWithRole(@RequestParam role: String) = loginRepo.listLoginsWithRole(role)

    @GetMapping(path = ["/admin/login/role/by/login"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun listRolesForLogin(@RequestParam subject: String) = loginRepo.listRolesForLogin(subject)

    @GetMapping(path = ["/admin/login/system/roles"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun listSystemRoles() = loginRepo.listSystemRoles()

    @GetMapping(path = ["/whoami"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun whoami(authentication: Authentication) = authentication.name

    @GetMapping(path = ["/roles"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun roles(authentication: Authentication) = authentication.authorities

}

@RestController
@RequestMapping("/api/v1/control/bridges")
class Runner(val bridgeRunner: BridgeRunnerManager) {
    private val logger = LoggerFactory.getLogger(this.javaClass)




    @GetMapping(path = ["/running"])
    fun isRunning() = Flag("Running?", flag = bridgeRunner.isRunning())

    @GetMapping(path = ["/started"])
    fun wasStarted() = Flag("Started?", flag = bridgeRunner.wasStarted())


    @GetMapping(path = ["/error/was-error"])
    fun wasError() = Flag("Errors?", flag = bridgeRunner.wasError())


    @GetMapping(path = ["/error/last"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun getLastError(): ResponseMessage {
        val lastError = bridgeRunner.getLastError()
        return if (lastError != null) {
            ResponseMessage("ERROR", Error(message = lastError.localizedMessage,
                    name = lastError.javaClass.simpleName, root = lastError.cause?.localizedMessage))
        } else {
            ResponseMessage("NO ERRORS")
        }
    }

    @PostMapping(path = ["/admin/clear/last/error"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun clearLastError() = bridgeRunner.clearLastError()

    @PostMapping(path = ["/admin/stop"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun stop() = bridgeRunner.stop()

    @PostMapping(path = ["/admin/restart"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun restart() = bridgeRunner.restart()


}