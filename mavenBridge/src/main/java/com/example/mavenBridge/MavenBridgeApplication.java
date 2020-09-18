package com.example.mavenBridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MavenBridgeApplication {

	public static void main(String[] args) {
		String [] actualArgs = args.length == 0 ? new String[]{"--config-directory", "./BOOT-INF/classes/config/"} : args;
		io.nats.bridge.admin.ApplicationMain.main(actualArgs);
	}

}
