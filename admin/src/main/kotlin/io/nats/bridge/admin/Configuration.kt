package io.nats.bridge.admin

import io.micrometer.core.instrument.MeterRegistry
import io.nats.bridge.admin.repos.ConfigRepoFromFiles
import io.nats.bridge.admin.repos.LoginRepoFromFiles
import io.nats.bridge.admin.runner.support.MessageBridgeLoader
import io.nats.bridge.admin.runner.support.MessageBridgeRunner
import io.nats.bridge.admin.runner.support.impl.MessageBridgeLoaderImpl
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
class Configuration {

    fun env(env: Environment): Environment {
        return env
    }

    @Bean
    fun bridgeConfigRepo(env: Environment): ConfigRepo {
        val repo = ConfigRepoFromFiles()
        repo.init()
        return repo
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
                  @Value(value = "\${repo.logins.configFile}") confFile: String
    ): LoginRepo {
        val repo = LoginRepoFromFiles(File(confFile), systemSecret = secretKey)
        repo.init()
        return repo
    }

}

@Configuration
@EnableSwagger2
class SwaggerConfig {
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



