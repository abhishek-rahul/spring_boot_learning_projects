package com.flashkart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlashkartApplication {
	private static final Logger logger = LoggerFactory.getLogger(FlashkartApplication.class);

	public static void main(String[] args) {
		logger.info("Starting Flashkart Application...");
		SpringApplication.run(FlashkartApplication.class, args);
		logger.info("Flashkart Application started successfully");
	}

}
