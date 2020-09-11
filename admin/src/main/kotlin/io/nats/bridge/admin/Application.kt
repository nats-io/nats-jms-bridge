package io.nats.bridge.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.nats.bridge.admin.util.ClasspathUtils
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.io.File
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicReference

@SpringBootApplication
open class Application

object AppConfig {
    private val applicationConfigRef = AtomicReference<ApplicationConfig?>()

    fun setConfig(appConfig:ApplicationConfig) {
        if (!applicationConfigRef.compareAndSet(null, appConfig)) {
            throw IllegalStateException("Application config can only be set once")
        }
    }

    fun getConfig() : ApplicationConfig =applicationConfigRef.get()
                ?: throw IllegalStateException("Application config must bet set before running application")

}

data class ApplicationConfig(val bridgeConfigFile:String, val loginConfigFile:String, val configDirectory:String?)

open class Run(val args : Array<String>) : CliktCommand(help = "Run NATS JMS/IBM MQ Bridge", epilog="""
    You can also set environments variables by replacing dashes '-' with underscores '_' and prefix with "NATS_BRIDGE" 
    
    ```
    NATS_BRIDGE_LOGIN_CONFIG_FILE=./config/nats-bridge-logins.yaml
    NATS_BRIDGE_BRIDGE_CONFIG_FILE=./config/nats-bridge.yaml
    ```
    
    Files can also be on the classpath inside of a jar file or on the file system in the classpath. 
    Just prepend the file name with "classpath://" to denote looking for this file on the classpath instead of the file system.
    
    ```
    -f classpath://nats-bridge.yaml
    ```
    
""".trimIndent()) {

    init {
        context { autoEnvvarPrefix = "NATS_BRIDGE" }
    }

    private val configFolder: String? by option("-d", "--config-directory", help = "Location of Configuration Directory").default("./config/")
    private val bridgeConfigFile: String? by option("-f", "--bridge-config-file", help = "Location of Bridge Config File")
    private val loginConfigFile: String? by option("-l", "--login-config-file", help = "Location of Bridge Login Config File")


    override fun run() {

        val configFileLocation : String = readFileConf(bridgeConfigFile, configFolder!!)
        val loginConfigLocation : String = readFileConf(loginConfigFile, configFolder!!, "nats-bridge-logins.yaml")
        AppConfig.setConfig(ApplicationConfig(configFileLocation, loginConfigLocation, configFolder))
        SpringApplication.run(Application::class.java, *args)
    }

    private fun Run.readFileConf(configLocation:String?, configFolder:String, defaultName : String = "nats-bridge.yaml"): String {
        return if (configLocation.isNullOrBlank()) {
            val configDir = File(configFolder)

            if (!configDir.exists()) {
                try {
                    configDir.mkdirs()
                } catch (ex: Exception) {
                    echo("Configuration directory does not exist and could not be created")
                }
            }

            if (configDir.exists()) {
                val configFile = File(configDir, defaultName)
                if (configFile.exists()) {
                    echo("Using configuration file $configFile")
                    configFile.toString()
                } else {
                    if (configFile.parentFile.canWrite()) {
                        echo("Using configuration file $configFile which does not exist but can be written to")
                        configFile.toString()
                    } else {
                        echo("Trying to use configuration file $configFile but it is not writeable so using classpath://./config/$defaultName instead")
                        val paths = ClasspathUtils.paths(this.javaClass, "./config/$defaultName")
                        if (paths.isEmpty()) {
                            echo("No configuration is found, exiting")
                            throw IllegalStateException("No configuration is found, exiting; \n " +
                                    "Tried to use configuration file $configFile but it is not writeable so using classpath://./config/$defaultName instead")
                        } else {
                            "classpath://./config/$defaultName"
                        }
                    }
                }
            } else {
                throw IllegalStateException("No configuration is found, exiting")
            }
        } else {
            configLocation
        }
    }
}

object ApplicationMain {
    @JvmStatic
    fun main(args: Array<String>) {
        Run(args).main(args)
    }
}

