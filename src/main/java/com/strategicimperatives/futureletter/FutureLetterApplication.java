package com.strategicimperatives.futureletter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class FutureLetterApplication {

	public static void main(String[] args) {
		SpringApplication.run(FutureLetterApplication.class, args);
	}

}
