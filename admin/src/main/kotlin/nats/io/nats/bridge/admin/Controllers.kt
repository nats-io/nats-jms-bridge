package nats.io.nats.bridge.admin


import io.swagger.annotations.ApiImplicitParam
import nats.io.nats.bridge.admin.models.bridges.*
import nats.io.nats.bridge.admin.models.logins.Login
import nats.io.nats.bridge.admin.models.logins.LoginRequest
import nats.io.nats.bridge.admin.models.logins.LoginToken
import nats.io.nats.bridge.admin.models.logins.TokenResponse
import nats.io.nats.bridge.admin.util.EncryptUtils
import nats.io.nats.bridge.admin.util.JwtUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException


@RestController
@RequestMapping("/api/v1/login")
class LoginController(@Value("\${security.secretKey}") private val adminSecretKey: String,
                      @Value("\${jwt.algo}") private val jwtAlgorithm: String,
                      private val longRepo: LoginRepo) {
    private val logger = LoggerFactory.getLogger(this.javaClass)


    @PostMapping("/generateToken")
    @ApiImplicitParam(name = "Content-Type", value = "application/json", dataType = "string", paramType = "header")
    fun generateToken(@RequestHeader headers: Map<String, String>, @RequestBody tokenRequest: LoginRequest) = doGenerateToken(headers, tokenRequest)


    fun doGenerateToken(headers: Map<String, String>, tokenRequest: LoginRequest): TokenResponse {

        val authLogin = longRepo.loadLogin(tokenRequest)
        if (authLogin != null) {
            val pwd: String = if (authLogin.secret.startsWith("pk-")) authLogin.secret else {
                val encryptUtils = EncryptUtils.createEncrypt(authLogin.genKey(adminSecretKey))
                encryptUtils.decrypt(authLogin.secret)
            }
            if (tokenRequest.secret == pwd) {
                val token = JwtUtils.generateToken("LOGIN_TOKEN", authLogin.genToken().toMap(),
                        adminSecretKey+adminSecretKey, jwtAlgorithm)
                return TokenResponse(token, authLogin.publicKey, authLogin.subject)
            } else {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Bad Token Request")
            }
        } else {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Login not found")
        }
    }


}

@RestController
@RequestMapping("/")
class RootController {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping(path = ["/ping"])
    fun ping() = "pong"

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

}

@RestController
@RequestMapping("/api/v1/logins")
class UserAdminController(private val loginRepo: LoginRepo) {
    private val logger = LoggerFactory.getLogger(this.javaClass)


    @PostMapping(path = ["/admin/login"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun addLogin(login: Login) = loginRepo.addLogin(login)

    @DeleteMapping(path = ["/admin/login"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun removeLogin(@RequestParam subject: String) = loginRepo.removeLogin(subject)

    @PutMapping(path = ["/admin/login/role"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun addRoleToLogin(@RequestParam subject: String, @RequestParam role: String) = loginRepo.addRoleToLogin(subject, role)

    @DeleteMapping(path = ["/admin/login/role"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun removeRoleFromLogin(@RequestParam subject: String, @RequestParam role: String) = loginRepo.removeRoleFromLogin(subject, role)

    @GetMapping(path = ["/admin/login"])
    @ApiImplicitParam(name = "Authorization", value = "Authorization token", dataType = "string", paramType = "header")
    fun listLogins(): List<String> = loginRepo.listLogins()

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
@RequestMapping("/api/v1/logins")
class Runner(private val loginRepo: LoginRepo) {
    private val logger = LoggerFactory.getLogger(this.javaClass)
}