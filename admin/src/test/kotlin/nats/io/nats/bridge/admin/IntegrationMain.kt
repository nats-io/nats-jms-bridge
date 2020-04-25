package nats.io.nats.bridge.admin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.nats.bridge.MessageBus
import io.nats.bridge.messages.MessageBuilder
import nats.io.nats.bridge.admin.Constants.adminToken
import nats.io.nats.bridge.admin.Constants.bridgeControlURL
import nats.io.nats.bridge.admin.Constants.initialYaml
import nats.io.nats.bridge.admin.Constants.loginURL
import nats.io.nats.bridge.admin.Constants.natsBridgeConfigFileName
import nats.io.nats.bridge.admin.models.logins.LoginConfig
import nats.io.nats.bridge.admin.models.logins.LoginRequest
import nats.io.nats.bridge.admin.models.logins.TokenResponse
import nats.io.nats.bridge.admin.repos.ConfigRepoFromFiles
import nats.io.nats.bridge.admin.runner.support.impl.MessageBridgeLoaderImpl
import nats.io.nats.bridge.admin.util.ObjectMapperUtils
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


object Constants {
    const val host = "http://localhost:8080"
    const val bridgeControlURL = "$host/api/v1/control/bridges"
    const val bridgeControlAdminURL = "$host/api/v1/control/bridges/admin"
    const val loginURL = "$host/api/v1/login"
    const val initialYaml = "config/initial-nats-bridge-logins.yaml"
    const val adminToken = "config/admin.token"
    const val natsBridgeConfigFileName =  "./config/nats-bridge.yaml"

}

data class Flag(val message: String, val flag: Boolean)
data class Error(val name: String, val message: String)
data class Message(val message: String, val error: Error? = null)
data class RequestException(val url :String, val responseMessage:String, val response: Response) : Exception(responseMessage) {
    override fun getLocalizedMessage(): String {
        return "$url ${response.code} ${response.message} ${response.body?.string()} ${super.getLocalizedMessage()}"
    }
}



class NatService(val messageBus: MessageBus,
                 val stop: AtomicBoolean = AtomicBoolean(false)) {
    fun run() {
        Thread{
            try {

                Runtime.getRuntime().addShutdownHook(Thread(Runnable { stop.set(true) }))
                while (true) {
                    if (stop.get()) {
                        messageBus.close()
                        break
                    }
                    Thread.sleep(1)
                    val receive = messageBus.receive()

                    receive.ifPresent { message ->
                        //println("Handle message " + message.bodyAsString())
                        message.reply(MessageBuilder.builder().withBody("Hello message " + message.bodyAsString()).build())
                    }

                    if (!receive.isPresent) {
                        //println("NOTHING")
                    }
                    messageBus.process()

                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }.start()

    }
}

fun main() {
   Main().run()
}


class Main {

    val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
    fun client() = OkHttpClient()
    val client = client()

    val yamlMapper: ObjectMapper = ObjectMapperUtils.getYamlObjectMapper()
    val conf = yamlMapper.readValue<LoginConfig>(File(initialYaml))
    var token : String? = null

    val loader = MessageBridgeLoaderImpl(ConfigRepoFromFiles(configFile = File(natsBridgeConfigFileName)))


    fun adminUser() = conf.logins.find { it.subject == "admin" }!!

    fun requestBody(any: Any) = jacksonObjectMapper().writeValueAsString(any).toRequestBody(JSON)

    fun post(request: Request) = client.newCall(request).execute()

    fun postJson(bd: Any, url: String): Response {
        val request = Request.Builder()
                .post(requestBody(bd))
                .url(url).build()
        return post(request)
    }



    fun readFlag(url: String): Flag {
        val request = Request.Builder().url(url).build()
        val response =  post(request)
        return if (response.isSuccessful) {
            jacksonObjectMapper().readValue<Flag>(response.body?.string()!!)
        } else {
            throw  RequestException(url, "Can't hit URL", response)
        }
    }

    fun postAdmin(url: String) {
        val request = Request.Builder()
                .post("".toRequestBody("application/json".toMediaTypeOrNull()))
                .header("Authorization", "Bearer $token")
                .url(url).build()
        val response =  post(request)

        if (!response.isSuccessful) {
            throw  RequestException(url, response.message + " | " +response.body?.string(), response)
        }
        println("OK $url")
    }

    private fun readToken() :String{
        val adminTokenFile = File(adminToken)
        return if (!adminTokenFile.exists()) {
            val adminUser = adminUser()
            val url = "$loginURL/generateToken"
            val bd: Any = LoginRequest(adminUser.subject, adminUser.publicKey, adminUser.secret)
            val response = postJson(bd, url)
            return if (response.isSuccessful) {
                val tokenResponse = jacksonObjectMapper().readValue<TokenResponse>(response.body?.string()!!)
                adminTokenFile.writeText(tokenResponse.token)
                tokenResponse.token
            } else {
                throw RuntimeException("Unable to get token")
            }

        } else {
            adminTokenFile.readText()
        }
    }

    fun run() {
        token = readToken()

        displayFlag(readFlag("$bridgeControlURL/running"))
        displayFlag(readFlag("$bridgeControlURL/started"))
        displayFlag(readFlag("$bridgeControlURL/error/was-error"))
        //postAdmin("$bridgeControlAdminURL/clear/last/error")
        //postAdmin("$bridgeControlAdminURL/stop")
        //postAdmin("$bridgeControlAdminURL/restart")
        val stop = AtomicBoolean()
        val builder = loader.loadBridgeBuilders()[0]
        val jmsClient = builder.sourceBusBuilder?.build()!!
        val natsClient = builder.destBusBuilder?.build()!!
        val natsService = NatService(natsClient, stop)

        natsService.run()
        val ref: AtomicReference<String> = AtomicReference()

        for (a in 0..100) {
            val latch = CountDownLatch(90)
            for (x in 0..100) {
                jmsClient.request("Rick") { response ->
                    ref.set(response)
                    latch.countDown()
                }
            }
            jmsClient.process()
            latch.await(1, TimeUnit.SECONDS)
            displayFlag(readFlag("$bridgeControlURL/running"))
            displayFlag(readFlag("$bridgeControlURL/started"))
            displayFlag(readFlag("$bridgeControlURL/error/was-error"))
        }

        println(ref.get())
    }

    private fun displayFlag(flag: Flag) {
        println("${flag.message} ${flag.flag}")
    }
}




