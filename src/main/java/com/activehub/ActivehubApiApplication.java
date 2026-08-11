package com.activehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ActivehubApiApplication {

	public static void main(String[] args) {
		// pgjdbc sends the JVM's default zone as a startup parameter; some minimal
		// postgres images ship a truncated zoneinfo table that doesn't recognize
		// every IANA zone (e.g. America/Buenos_Aires), which fails the handshake.
		System.setProperty("user.timezone", "UTC");
		SpringApplication.run(ActivehubApiApplication.class, args);
	}

}
