package io.nats.bridge.admin

import org.mockito.Mockito
import org.springframework.context.annotation.Configuration

@Configuration
class Config {

    //import org.springframework.context.annotation.*
    //@Bean
    //@Primary
    //fun ser(): KinesisService = Mockito.mock(KinesisService::class.java)
}

object MockitoHelper {
    fun <T> anyObject(): T {
        Mockito.any<T>()
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> uninitialized(): T = null as T
}