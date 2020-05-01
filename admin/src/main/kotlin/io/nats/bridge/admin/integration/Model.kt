package io.nats.bridge.admin.integration

import okhttp3.Response

data class Flag(val message: String, val flag: Boolean)
data class Error(val name: String, val message: String)
data class Message(val message: String, val error: Error? = null)
data class RequestException(val url: String, val responseMessage: String, val response: Response) : Exception(responseMessage) {
    override fun getLocalizedMessage(): String {
        return "$url ${response.code} ${response.message} ${response.body?.string()} ${super.getLocalizedMessage()}"
    }
}
