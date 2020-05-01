package io.nats.bridge.admin.proto

import com.ibm.msg.client.jms.JmsConnectionFactory
import com.ibm.msg.client.jms.JmsDestination
import java.util.*
import javax.jms.*
import javax.naming.Context
import javax.naming.NamingException
import javax.naming.directory.InitialDirContext


/**
 * A JMS consumer (receiver or subscriber) application that receives a message from the named
 * destination (queue or topic) by looking up the connection factory instance and the destination
 * instance in an initial context (This sample supports file system context only).
 *
 * Tip: A subscriber application must be started before the publisher application.
 *
 * Notes:
 *
 * API type: IBM JMS API (v1.1, unified domain)
 *
 * Messaging domain: Point-to-point or Publish-Subscribe
 *
 * Provider type: IBM MQ
 *
 * Connection mode: Client connection or bindings connection
 *
 * JNDI in use: Yes
 *
 * Usage:
 *
 * JmsJndiConsumer -i initialContext -c connectionFactory -d destination
 *
 * for example:
 *
 * JmsJndiConsumer -i file:/C:/JNDI-Directory -c myQCF -d myQueue
 */
object JmsJndiConsumer {
    private var initialContextUrl: String? = null
    private var connectionFactoryFromJndi: String? = null
    private var destinationFromJndi: String? = null
    private const val timeout = 15000 // in ms or 15 seconds

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
        parseArgs(args)

        // Variables
        var connection: Connection? = null
        var session: Session? = null
        var destination: Destination? = null
        var consumer: MessageConsumer? = null
        try {
            // Instantiate the initial context
            val contextFactory = "com.sun.jndi.fscontext.RefFSContextFactory"
            val environment = Hashtable<String, String?>()
            environment[Context.INITIAL_CONTEXT_FACTORY] = contextFactory
            environment[Context.PROVIDER_URL] = initialContextUrl
            val context: Context = InitialDirContext(environment)
            println("Initial context found!")

            // Lookup the connection factory
            val cf = context.lookup(connectionFactoryFromJndi) as JmsConnectionFactory

            // Lookup the destination
            destination = context.lookup(destinationFromJndi) as JmsDestination

            // Create JMS objects
            connection = cf.createConnection()
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
            consumer = session.createConsumer(destination)

            // Start the connection
            connection.start()

            // And, receive the message
            val message = consumer.receive(timeout.toLong())
            if (message != null) {
                println("Received message:\n$message")
            } else {
                println("No message received!\n")
                recordFailure(null)
            }
            recordSuccess()
        } catch (jmsex: JMSException) {
            recordFailure(jmsex)
        } catch (ne: NamingException) {
            println("The initial context could not be instantiated, or the lookup failed.")
            recordFailure(ne)
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
    private fun recordFailure(ex: Exception?) {
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

    /**
     * Parse user supplied arguments.
     *
     * @param args
     */
    private fun parseArgs(args: Array<String>) {
        try {
            val length = args.size
            require(length != 0) { "No arguments! Mandatory arguments must be specified." }
            require(length % 2 == 0) { "Incorrect number of arguments!" }
            var i = 0
            while (i < length) {
                require(args[i][0] == '-') { "Expected a '-' character next: " + args[i] }
                val opt = args[i].toLowerCase()[1]
                when (opt) {
                    'i' -> initialContextUrl = args[++i]
                    'c' -> connectionFactoryFromJndi = args[++i]
                    'd' -> destinationFromJndi = args[++i]
                    else -> {
                        throw IllegalArgumentException("Unknown argument: $opt")
                    }
                }
                ++i
            }
            requireNotNull(initialContextUrl) { "An initial context must be specified." }
            requireNotNull(connectionFactoryFromJndi) { "A connection factory to lookup in the initial context must be specified." }
            requireNotNull(destinationFromJndi) { "A destination to lookup in the initial context must be specified." }
        } catch (e: Exception) {
            println(e.message)
            printUsage()
            System.exit(-1)
        }
        return
    }

    /**
     * Display usage help.
     */
    private fun printUsage() {
        println("\nUsage:")
        println("JmsJndiConsumer -i initialContext -c connectionFactory -d destination")
        return
    }
} // end class
