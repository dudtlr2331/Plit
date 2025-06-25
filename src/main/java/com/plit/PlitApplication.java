package com.plit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
public class PlitApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlitApplication.class, args);
	}

}
