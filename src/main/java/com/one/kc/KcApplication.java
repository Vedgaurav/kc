package com.one.kc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class KcApplication {

	public static void main(String[] args) {
		SpringApplication.run(KcApplication.class, args);
	}

}
