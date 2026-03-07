package com.fno;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FnoApplication {
	public static void main(String[] args) {
        SpringApplication.run(FnoApplication.class, args);
    }
}
