package io.nats.bridge.admin.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.nats.bridge.jms.support.JMSMessageBusBuilder
import io.nats.bridge.admin.models.logins.LoginConfig
import io.nats.bridge.admin.models.logins.LoginRequest
import io.nats.bridge.admin.models.logins.TokenResponse
import io.nats.bridge.admin.repos.ConfigRepoFromFiles
import io.nats.bridge.admin.runner.support.impl.MessageBridgeLoaderImpl
import io.nats.bridge.admin.util.ObjectMapperUtils
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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class IntegrationUtils {

    val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
    fun client() = OkHttpClient()
    val client = client()

    val yamlMapper: ObjectMapper = ObjectMapperUtils.getYamlObjectMapper()
    val conf = yamlMapper.readValue<LoginConfig>(File(Constants.initialYaml))
    var token: String? = null

    val loader = MessageBridgeLoaderImpl(ConfigRepoFromFiles(configFile = File(Constants.natsBridgeConfigFileName)))


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
        val response = post(request)
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
        val response = post(request)

        if (!response.isSuccessful) {
            throw  RequestException(url, response.message + " | " + response.body?.string(), response)
        }
        println("OK $url")
    }

    private fun readToken(): String {
        val adminTokenFile = File(Constants.adminToken)
        return if (!adminTokenFile.exists()) {
            val adminUser = adminUser()
            val url = "${Constants.loginURL}/generateToken"
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

        displayFlag(readFlag("${Constants.bridgeControlURL}/running"))
        displayFlag(readFlag("${Constants.bridgeControlURL}/started"))
        displayFlag(readFlag("${Constants.bridgeControlURL}/error/was-error"))
        //postAdmin("$bridgeControlAdminURL/clear/last/error")
        //postAdmin("$bridgeControlAdminURL/stop")
        //postAdmin("$bridgeControlAdminURL/restart")
        val stop = AtomicBoolean()
        val builder = loader.loadBridgeBuilders()[0]

        val clientBuilder  = builder.sourceBusBuilder!!

        if (clientBuilder is JMSMessageBusBuilder) {

        }

        val serverBuilder = builder.destBusBuilder!!

        if (serverBuilder is JMSMessageBusBuilder) {
            serverBuilder.asSource()
        }

        val clientBus = clientBuilder.build()
        val serverBus = serverBuilder.build()
        val natsService = FakeServer(serverBus, stop)

        natsService.run()
        val ref: AtomicReference<String> = AtomicReference()
        val count = AtomicInteger()

        for (a in 0..9) {
            println("Run $a")
            val latch = CountDownLatch(10)
            for (x in 0..9) {
                println("Call $x of run $a")
                clientBus.request("Rick $a $x") { response ->
                    ref.set(response)
                    count.incrementAndGet()
                    latch.countDown()
                }
                Thread.sleep(50)
                clientBus.process()
            }
            Thread.sleep(50)
            clientBus.process()
            latch.await(5000, TimeUnit.MILLISECONDS)
            println("REPLY COUNT " + count.get())
            displayFlag(readFlag("${Constants.bridgeControlURL}/running"))
            displayFlag(readFlag("${Constants.bridgeControlURL}/started"))
            displayFlag(readFlag("${Constants.bridgeControlURL}/error/was-error"))
        }

        Thread.sleep(1_000)
        stop.set(true)
        clientBus.close()
        serverBus.close()
        println("REPLY COUNT " + count.get())
        println("Done")

        println(ref.get())
    }

    private fun displayFlag(flag: Flag) {
        println("${flag.message} ${flag.flag}")
    }
}