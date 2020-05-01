package io.nats.bridge.admin.integration

import io.nats.bridge.MessageBus
import io.nats.bridge.messages.MessageBuilder
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class FakeServer(val messageBus: MessageBus,
                 val stop: AtomicBoolean = AtomicBoolean(false)) {
    fun run() {
        Thread {
            try {

                Runtime.getRuntime().addShutdownHook(Thread(Runnable { stop.set(true) }))
                while (true) {
                    if (stop.get()) {
                        messageBus.close()
                        break
                    }
                    Thread.sleep(1)
                    val receive = messageBus.receive(Duration.ofSeconds(1))

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