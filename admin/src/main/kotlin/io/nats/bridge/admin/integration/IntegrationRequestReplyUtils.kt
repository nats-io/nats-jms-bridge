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
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class IntegrationRequestReplyUtils {

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
        val builder = loader.loadBridgeConfigs()[0].builders[0]
        val builder2 = loader.loadBridgeConfigs()[0].builders[1]

        val clientBuilder  = builder.sourceBusBuilder!!
        val clientBuilder2  = builder2.sourceBusBuilder!!


        val serverBuilder = builder.destinationBusBuilder!!

        if (serverBuilder is JMSMessageBusBuilder) {
            serverBuilder.useIBMMQ() //TODO fix so this works with activemq too
            serverBuilder.asSource()
        }

        if (builder2.destinationBusBuilder!! is JMSMessageBusBuilder) {
            (builder2.destinationBusBuilder as JMSMessageBusBuilder).useIBMMQ() //todo fix so this works with activemq
        }

        val clientBus = clientBuilder.build()
        val clientBus2 = clientBuilder2.build()
        val serverBus = serverBuilder.build()
        val serverBus2 = builder2.destinationBusBuilder!!.build()
        val natsService = FakeServer(serverBus, stop)
        val nats2Service = FakeServer(serverBus2, stop)

        natsService.run()
        nats2Service.run()
        val ref: AtomicReference<String> = AtomicReference()
        val count = AtomicInteger()

        val startTime = System.currentTimeMillis()

        var totalSent = 0
        for (a in 0..49) {
            println("Run $a")
            val latch = CountDownLatch(50)
            for (x in 0..49) {
                totalSent++
                println("Call $x of run $a")

                if (x % 2 == 0) {
                    clientBus.request("Rick $a $x") { response ->
                        ref.set(response)
                        count.incrementAndGet()
                        latch.countDown()
                    }
                } else {
                    clientBus2.request("Rick $a $x") { response ->
                        ref.set(response)
                        count.incrementAndGet()
                        latch.countDown()
                    }
                }

                latch.await(1, TimeUnit.MILLISECONDS)
                clientBus.process()
                clientBus2.process()
            }

            for (x in 0..1000) {
                if (latch.await(10, TimeUnit.MILLISECONDS)) {
                    break
                }
                clientBus.process()
                clientBus2.process()
            }

            val timeSpent = System.currentTimeMillis() - startTime
            println("############### REPLY COUNT ${count.get()} of $totalSent in time $timeSpent")
            displayFlag(readFlag("${Constants.bridgeControlURL}/running"))
            displayFlag(readFlag("${Constants.bridgeControlURL}/started"))
            displayFlag(readFlag("${Constants.bridgeControlURL}/error/was-error"))
        }

        val totalTime = System.currentTimeMillis() - startTime
        println("TOTAL SENT ############### $totalSent in time $totalTime")
        Thread.sleep(1_000)
        clientBus.process()
        clientBus2.process()

        println("Complete "+ count.get())

        stop.set(true)
        clientBus.close()
        clientBus2.close()
        serverBus.close()
        serverBus2.close()
        println("REPLY COUNT " + count.get())
        println("Done")

        println(ref.get())
    }

    private fun displayFlag(flag: Flag) {
        println("${flag.message} ${flag.flag}")
    }
}