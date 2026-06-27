package com.codewithsubhra.money_tracker_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class MoneyTrackerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoneyTrackerBackendApplication.class, args);
	}

}
