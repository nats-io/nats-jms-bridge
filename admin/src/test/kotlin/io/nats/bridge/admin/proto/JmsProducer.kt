package io.nats.bridge.admin.proto

import com.ibm.msg.client.jms.JmsFactoryFactory
import com.ibm.msg.client.wmq.WMQConstants
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.jms.*

object JmsProducer {
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
        // Parse the arguments

        // Variables
        var connection: Connection? = null
        var session: Session? = null
        var destination: Destination? = null
        var producer: MessageProducer? = null
        try {
            // Create a connection factory
            val ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER)
            val cf = ff.createConnectionFactory()

            // Set the properties
            cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, host)
            cf.setIntProperty(WMQConstants.WMQ_PORT, port)
            cf.setStringProperty(WMQConstants.WMQ_CHANNEL, channel)

            cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT)
//            } else {
//                cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_BINDINGS)
//            }
            cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManagerName)
            cf.setStringProperty(WMQConstants.USERID, user)
            cf.setStringProperty(WMQConstants.PASSWORD, password)
            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true)

            // Create JMS objects
            connection = cf.createConnection()
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)

            destination = session.createQueue(destinationName)

            producer = session.createProducer(destination)

            // Start the connection
            connection.start()
            var line: String
            val `in` = BufferedReader(InputStreamReader(System.`in`))
            do {
                print("Enter some text to be sent in a message <ENTER to finish>:")
                System.out.flush()
                line = `in`.readLine()
                if (line != null) {
                    if (line.trim { it <= ' ' }.length == 0) {
                        break
                    }
                    val message = session.createTextMessage(line)
                    // And, send the message
                    producer.send(message)
                    println("Sent message:\n$message")
                }
            } while (line != null)
            recordSuccess()
        } catch (jmsex: JMSException) {
            recordFailure(jmsex)
        } catch (jmsex: IOException) {
            recordFailure(jmsex)
        } finally {
            if (producer != null) {
                try {
                    producer.close()
                } catch (jmsex: JMSException) {
                    println("Producer could not be closed.")
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