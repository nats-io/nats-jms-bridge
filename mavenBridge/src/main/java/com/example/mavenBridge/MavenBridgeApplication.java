package com.example.mavenBridge;


import io.nats.bridge.admin.AppConfig;
import io.nats.bridge.admin.NATSJmsBridgeApplication;
import io.nats.bridge.admin.Run;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MavenBridgeApplication {

	public static void main(String[] args) {

		final AppConfig appConfig = AppConfig.INSTANCE;
		appConfig.setBridgeConfigFileDefault("classpath:nats-bridge.yaml");
		appConfig.setLoginConfigFileDefault("classpath:nats-bridge-logins.yaml");
		appConfig.setRunSpringBootDirect(false);


		Run runner = AppConfig.runner();
		runner.runMain(args);
		SpringApplication.run(new Class[]{NATSJmsBridgeApplication.class, MavenBridgeApplication.class}, args);
	}

}
