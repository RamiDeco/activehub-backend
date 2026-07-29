package com.utn.activehub;

import org.springframework.boot.SpringApplication;

public class TestActivehubApplication {

	public static void main(String[] args) {
		SpringApplication.from(ActivehubApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
