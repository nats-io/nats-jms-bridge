package io.nats.bridge.admin.proto

import com.ibm.msg.client.jms.JmsFactoryFactory
import com.ibm.msg.client.wmq.WMQConstants
import javax.jms.*

/**
 * A JMS consumer (receiver or subscriber) application that receives a message from the named
 * destination (queue or topic).
 *
 * Tip: A subscriber application must be started before the publisher application.
 *
 * Notes:
 *
 * API type: IBM JMS API (v1.1, unified domain)
 *
 * Messaging domain: Point-to-point or Publish-Subscribe
 *
 * Provider type: WebSphere MQ
 *
 * Connection mode: Client connection
 *
 * JNDI in use: No
 *
 * Usage:
 *
 * JmsConsumer -m queueManagerName -d destinationName [-h host -p port -l channel] [-u user -w passWord] [-t timeoutSeconds]
 *
 * for example:
 *
 * JmsConsumer -m QM1 -d Q1
 *
 * JmsConsumer -m QM1 -d topic://foo -h localhost -p 1414 -u tester -w testpw
 */
object JmsConsumer {
    private val host: String = "localhost"
    private val port = 1414
    private val user: String = "app"
    private var timeout = 15000 // in ms or 15 seconds
    private val channel = "DEV.APP.SVRCONN"
    private val password: String = "passw0rd"
    private val queueManagerName: String = "QM1";
    private val destinationName: String = "DEV.QUEUE.1"


    // System exit status value (assume unset value to be 1)
    private var status = 1

    /**
     * Main method
     *
     * @param args
     */
    @JvmStatic
    fun main(args: Array<String>) {

        // Variables
        var connection: Connection? = null
        var session: Session? = null
        var destination: Destination? = null
        var consumer: MessageConsumer? = null
        try {
            // Create a connection factory
            val ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER)
            val cf = ff.createConnectionFactory()

            // Set the properties
            cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, host)
            cf.setIntProperty(WMQConstants.WMQ_PORT, port)
            cf.setStringProperty(WMQConstants.WMQ_CHANNEL, channel)

            cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT)
            cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManagerName)

            cf.setStringProperty(WMQConstants.USERID, user)
            cf.setStringProperty(WMQConstants.PASSWORD, password)
            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true)
//            if (user != null) {

//            }

            // Create JMS objects
            connection = cf.createConnection()
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            destination = session.createQueue(destinationName)
            consumer = session.createConsumer(destination)

            // Start the connection
            connection.start()

            // And, receive the message
            var message: Message?
            do {
                message = consumer.receive(timeout.toLong())
                if (message != null) {
                    println("Received message:\n$message")
                }
            } while (message != null)
            System.out.format("No message received in %d seconds!\n", timeout / 1000)
            recordSuccess()
        } catch (jmsex: JMSException) {
            recordFailure(jmsex)
        } finally {
            if (consumer != null) {
                try {
                    consumer.close()
                } catch (jmsex: JMSException) {
                    println("Consumer could not be closed.")
                    recordFailure(jmsex)
                }
            }
            if (session != null) {
                try {
                    session.close()
                } catch (jmsex: JMSException) {
                    println("Session could not be closed.")
                    recordFailure(jmsex)
                }
            }
            if (connection != null) {
                try {
                    connection.close()
                } catch (jmsex: JMSException) {
                    println("Connection could not be closed.")
                    recordFailure(jmsex)
                }
            }
        }
        System.exit(status)
        return
    } // end main()

    /**
     * Process a JMSException and any associated inner exceptions.
     *
     * @param jmsex
     */
    private fun processJMSException(jmsex: JMSException) {
        println(jmsex)
        var innerException: Throwable? = jmsex.linkedException
        if (innerException != null) {
            println("Inner exception(s):")
        }
        while (innerException != null) {
            println(innerException)
            innerException = innerException.cause
        }
        return
    }

    /**
     * Record this run as successful.
     */
    private fun recordSuccess() {
        println("SUCCESS")
        status = 0
        return
    }

    /**
     * Record this run as failure.
     *
     * @param ex
     */
    private fun recordFailure(ex: java.lang.Exception?) {
        if (ex != null) {
            ex.printStackTrace()
            if (ex is JMSException) {
                processJMSException(ex)
            } else {
                println(ex)
            }
        }
        println("FAILURE")
        status = -1
        return
    }

}