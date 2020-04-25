package nats.io.nats.bridge.admin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import nats.io.nats.bridge.admin.Constants.initialYaml
import nats.io.nats.bridge.admin.Constants.loginURL
import nats.io.nats.bridge.admin.models.logins.LoginConfig
import nats.io.nats.bridge.admin.models.logins.LoginRequest
import nats.io.nats.bridge.admin.models.logins.TokenResponse
import nats.io.nats.bridge.admin.util.ObjectMapperUtils
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.lang.RuntimeException


object Constants {
    const val host = "http://localhost:8080"
    const val bridgeURL = "$host/api/v1/bridge/control"
    const val bridgeAdminURL = "$host/api/v1/bridge/control/admin"
    const val loginURL = "$host/api/v1/login"
    const val initialYaml = "config/initial-nats-bridge-logins.yaml"


}
class Main {

    val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
    fun client() = OkHttpClient()
    val client = client()

    val yamlMapper: ObjectMapper = ObjectMapperUtils.getYamlObjectMapper()
    val conf = yamlMapper.readValue<LoginConfig>(File(initialYaml))
    fun adminUser() = conf.logins.find { it.subject == "admin" }!!

    fun requestBody(any: Any) = jacksonObjectMapper().writeValueAsString(any).toRequestBody(JSON)

    fun post(request: Request) = client.newCall(request).execute()

    fun postJson(bd: Any, url: String): Response {
        val request = Request.Builder()
                .post(requestBody(bd))
                .url(url).build()
        return post(request)
    }


    fun run() {
        println(readToken())
    }

    private fun readToken() :String{
        val adminUser = adminUser()
        val url = "$loginURL/generateToken"
        val bd: Any = LoginRequest(adminUser.subject, adminUser.publicKey, adminUser.secret)
        val response = postJson(bd, url)
        return if (response.isSuccessful) {
            val tokenResponse = jacksonObjectMapper().readValue<TokenResponse>(response.body?.string()!!)
            tokenResponse.token
        } else {
            throw RuntimeException("Unable to get token")
        }
    }
}

fun main() {

   Main().run()




}





