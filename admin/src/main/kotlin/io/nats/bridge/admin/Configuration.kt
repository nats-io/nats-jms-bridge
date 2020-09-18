package io.nats.bridge.admin

import io.micrometer.core.instrument.MeterRegistry
import io.nats.bridge.admin.repos.ConfigRepoFromPath
import io.nats.bridge.admin.repos.LoginRepoFromPath
import io.nats.bridge.admin.runner.support.MessageBridgeLoader
import io.nats.bridge.admin.runner.support.MessageBridgeRunner
import io.nats.bridge.admin.runner.support.impl.MessageBridgeLoaderImpl
import io.nats.bridge.admin.util.ClasspathUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.io.File
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.*


@Configuration
open class Configuration {


    @Autowired
    var resourceLoader: ResourceLoader? = null

    fun env(env: Environment): Environment {
        return env
    }

    @Bean
    open fun appConfig(): ApplicationConfig {
        return AppConfig.getConfig()
    }

    @Bean
    open fun bridgeConfigRepo(env: Environment, app: ApplicationConfig): ConfigRepo {
        return if (app.bridgeConfigFile.startsWith("classpath://")) {
            val configFile = app.bridgeConfigFile.substring("classpath://".length)
            val paths = ClasspathUtils.paths(this.javaClass, configFile)
            val repo = ConfigRepoFromPath(configFile = paths[0])
            repo.init()
            repo
        } else if (app.bridgeConfigFile.startsWith("classpath:")) {


            println("URI ********************************* " + app.bridgeConfigFile)
            val resource: Resource = resourceLoader!!.getResource(app.bridgeConfigFile)
            println("URI ********************************* " + resource.uri)
            println("URL ********************************* " + resource.url)

            val resURI = resource.uri.toString()

            if (resURI.startsWith("jar:file")) {
                val split = resURI.split("!")
                println("SPLIT " + split)
                val fileSystemURI: URI = URI.create(split[0])
                println("fileSystemURI " + fileSystemURI)

                val res: String = split[1] + split[2]
                println("res " + res)

                val envFS: MutableMap<String, String> = HashMap()
                envFS["create"] = "true"

                val zipFS: FileSystem = try {
                    FileSystems.newFileSystem(fileSystemURI, envFS)
                } catch (ex:Exception) {
                    FileSystems.getFileSystem(fileSystemURI)
                }

                val actualResource: Path = zipFS.getPath(res)

                val repo = ConfigRepoFromPath(configFile = actualResource)
                repo

            } else {
                throw IllegalStateException("Unable to read resource " + resURI)
            }
        } else {
            val repo = ConfigRepoFromPath(File(app.bridgeConfigFile).toPath())
            repo.init()
            repo
        }
    }

    @Bean
    open fun messageBridgeLoader(repo: ConfigRepo, metricsRegistry: MeterRegistry): MessageBridgeLoader =
            MessageBridgeLoaderImpl(repo, metricsRegistry = metricsRegistry)

    @Bean
    open fun messageBridgeRunner(messageBridgeLoader: MessageBridgeLoader): MessageBridgeRunner =
            MessageBridgeRunner(messageBridgeLoader)

    @Bean
    open fun loginRepo(env: Environment,
                       @Value(value = "\${security.secretKey}") secretKey: String,
                       @Value(value = "\${repo.logins.configFile}") confFile: String,
                       app: ApplicationConfig
    ): LoginRepo {

        return if (app.loginConfigFile.startsWith("classpath://")) {
            val configFile = app.bridgeConfigFile.substring("classpath://".length)
            val paths = ClasspathUtils.paths(this.javaClass, configFile)
            val repo = LoginRepoFromPath(configFile = paths[0], systemSecret = secretKey)
            repo.init()
            repo
        } else if (app.loginConfigFile.startsWith("classpath:")) {
            println("URI ********************************* " + app.loginConfigFile)
            val resource: Resource = resourceLoader!!.getResource(app.loginConfigFile)
            println("URI ********************************* " + resource.uri)
            println("URL ********************************* " + resource.url)

            val resURI = resource.uri.toString()

            if (resURI.startsWith("jar:file")) {
                val split = resURI.split("!")
                println("SPLIT " + split)
                val fileSystemURI: URI = URI.create(split[0])
                println("fileSystemURI " + fileSystemURI)

                val res: String = split[1] + split[2]
                println("res " + res)

                val envFS: MutableMap<String, String> = HashMap()
                envFS["create"] = "true"

                val zipFS: FileSystem = try {
                    FileSystems.newFileSystem(fileSystemURI, envFS)
                } catch (ex:Exception) {
                    FileSystems.getFileSystem(fileSystemURI)
                }

                val actualResource: Path = zipFS.getPath(res)

                val repo = LoginRepoFromPath(configFile = actualResource, systemSecret = secretKey)
                repo

            } else {
                throw IllegalStateException("Unable to read resource " + resURI)
            }


        } else {
            val repo = LoginRepoFromPath(File(app.loginConfigFile).toPath(), systemSecret = secretKey)
            repo.init()
            repo
        }

    }

}

@Configuration
@EnableSwagger2
open class SwaggerConfig {
    @Bean
    open fun natsBridgeAPI(@Value(value = "\${version:dev}") version: String): Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .groupName("NatsBridgeAdmin")
                .apiInfo(
                        ApiInfoBuilder()
                                .title("NATS Bridge Admin Service API")
                                .description("NATS Bridge Admin service.")
                                .contact(
                                        Contact(
                                                "NATS developers",
                                                "",
                                                "info@synadia.com")
                                )
                                .version(version)
                                .license("")
                                .build())
                .select()
                .paths(PathSelectors.ant("/api/v1/**"))
                .build()
    }
}



