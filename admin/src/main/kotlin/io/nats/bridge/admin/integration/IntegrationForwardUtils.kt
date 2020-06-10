package io.nats.bridge.admin.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.nats.bridge.jms.support.JMSMessageBusBuilder
import io.nats.bridge.admin.models.logins.LoginConfig
import io.nats.bridge.admin.models.logins.LoginRequest
import io.nats.bridge.admin.models.logins.TokenResponse
import io.nats.bridge.admin.repos.ConfigRepoFromPath
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
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class IntegrationForwardUtils {

    val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
    fun client() = OkHttpClient()
    val client = client()

    val yamlMapper: ObjectMapper = ObjectMapperUtils.getYamlObjectMapper()
    val conf = yamlMapper.readValue<LoginConfig>(File(Constants.initialYaml))
    var token: String? = null

    val loader = MessageBridgeLoaderImpl(ConfigRepoFromPath(configFile = File(Constants.natsBridgeConfigFileName).toPath()))


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
        val builder = loader.loadBridgeConfigs()[0].builders[0]

        val clientBuilder  = builder.sourceBusBuilder!!
        val serverBuilder = builder.destinationBusBuilder!!

        if (serverBuilder is JMSMessageBusBuilder) {
            serverBuilder.asSource()
        }

        val senderBus = clientBuilder.build()

        val receiverBus = serverBuilder.build()


        val ref: AtomicReference<String> = AtomicReference()
        val count = AtomicInteger()

        val startTime = System.currentTimeMillis()

        val totalLatch = CountDownLatch(50)

        val thread = Thread(Runnable {
            while (count.get() < 2500) {
                val receive = receiverBus.receive(Duration.ofMillis(100))
                if (receive.isPresent) {
                    count.incrementAndGet()
                }
                val timeNow = System.currentTimeMillis()
                if (timeNow - startTime > 60_000) {
                    break
                }
            }
        })

        thread.start()

        var totalSent = 0
        for (a in 0..49) {
            println("Run $a")

            for (x in 0..49) {
                totalSent++
                println("Call $x of run $a")
                senderBus.publish("Rick $a $x")

                senderBus.process()
            }

            for (x in 0..100) {
                if (totalLatch.await(5, TimeUnit.MILLISECONDS)) {
                    break
                }
                senderBus.process()
            }
            val timeSpent = System.currentTimeMillis() - startTime
            println("############### FORWARD COUNT ${count.get()} of $totalSent in time $timeSpent")
            displayFlag(readFlag("${Constants.bridgeControlURL}/running"))
            displayFlag(readFlag("${Constants.bridgeControlURL}/started"))
            displayFlag(readFlag("${Constants.bridgeControlURL}/error/was-error"))
        }

        val totalTime = System.currentTimeMillis() - startTime
        println("TOTAL SENT ############### $totalSent in time $totalTime")
        Thread.sleep(1_000)
        senderBus.process()


        println("Complete "+ count.get())

        stop.set(true)
        senderBus.close()
        receiverBus.close()
        println("FORWARD COUNT " + count.get())
        println("Done")

        println(ref.get())
    }

    private fun displayFlag(flag: Flag) {
        println("${flag.message} ${flag.flag}")
    }
}

fun main() {
    val utils = IntegrationForwardUtils()
    utils.run()
}