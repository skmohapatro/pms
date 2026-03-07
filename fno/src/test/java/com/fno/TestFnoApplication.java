package com.fno;

import org.springframework.boot.SpringApplication;

public class TestFnoApplication {

	public static void main(String[] args) {
		SpringApplication.from(FnoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
