package io.nats.bridge.admin

import io.micrometer.core.instrument.MeterRegistry
import io.nats.bridge.admin.repos.ConfigRepoFromPath
import io.nats.bridge.admin.repos.LoginRepoFromPath
import io.nats.bridge.admin.runner.support.MessageBridgeLoader
import io.nats.bridge.admin.runner.support.MessageBridgeRunner
import io.nats.bridge.admin.runner.support.impl.MessageBridgeLoaderImpl
import io.nats.bridge.admin.util.ClasspathUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.io.File


@Configuration
open class Configuration {

    fun env(env: Environment): Environment {
        return env
    }

    @Bean fun appConfig() : ApplicationConfig {
        return AppConfig.getConfig()
    }

    @Bean
    fun bridgeConfigRepo(env: Environment, app:ApplicationConfig): ConfigRepo {
        return if (app.bridgeConfigFile.startsWith("classpath://") ) {
            val configFile = app.bridgeConfigFile.substring("classpath://".length)
            val paths = ClasspathUtils.paths(this.javaClass, configFile)
            val repo = ConfigRepoFromPath(configFile = paths[0])
            repo.init()
            repo
        } else {
            val repo = ConfigRepoFromPath(File(app.bridgeConfigFile).toPath())
            repo.init()
            repo
        }
    }

    @Bean
    fun messageBridgeLoader(repo: ConfigRepo, metricsRegistry: MeterRegistry): MessageBridgeLoader =
         MessageBridgeLoaderImpl(repo, metricsRegistry = metricsRegistry)

    @Bean
    fun messageBridgeRunner( messageBridgeLoader : MessageBridgeLoader) : MessageBridgeRunner =
            MessageBridgeRunner(messageBridgeLoader)

    @Bean
    fun loginRepo(env: Environment,
                  @Value(value = "\${security.secretKey}") secretKey: String,
                  @Value(value = "\${repo.logins.configFile}") confFile: String,
                  app:ApplicationConfig
    ): LoginRepo {

        return if (app.loginConfigFile.startsWith("classpath://") ) {
            val configFile = app.bridgeConfigFile.substring("classpath://".length)
            val paths = ClasspathUtils.paths(this.javaClass, configFile)
            val repo = LoginRepoFromPath(configFile = paths[0], systemSecret = secretKey)
            repo.init()
            repo
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
    fun natsBridgeAPI(@Value(value = "\${version:dev}") version: String): Docket {
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



