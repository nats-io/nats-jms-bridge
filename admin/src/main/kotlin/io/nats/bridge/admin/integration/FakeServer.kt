package io.nats.bridge.admin.integration
import io.nats.bridge.MessageBus
import io.nats.bridge.messages.MessageBuilder
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

class FakeServer(val messageBusSupplier: Supplier<MessageBus>,
                 val stop: AtomicBoolean = AtomicBoolean(false)) {
    fun run() {
        Thread {
            try {


                var messageBus  = messageBusSupplier.get();
                Runtime.getRuntime().addShutdownHook(Thread(Runnable { stop.set(true) }))
                while (true) {
                    if (stop.get()) {
                        messageBus.close()
                        break
                    }

                    try {
                        val receive = messageBus.receive(Duration.ofMillis(50))

                        receive.ifPresent { message ->
                            //println("Handle message " + message.bodyAsString())
                            message.reply(MessageBuilder.builder().withBody("Hello message " + message.bodyAsString()).build())
                        }

//                    if (!receive.isPresent) {
//                        //println("NOTHING")
//                    }
                        messageBus.process()
                    } catch (ex:java.lang.Exception) {
                        try {

                            messageBus?.close()
                            Thread.sleep(3000)
                        }catch (ex:Exception) {
                            System.out.println("Unable to close message bus on recover")
                        }

                        messageBus = messageBusSupplier.get()
                    }

                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }.start()
    }
}