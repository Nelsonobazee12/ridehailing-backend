package com.ridehailing.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.ridehailing.backend.repository",
		excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
				type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
				classes = com.ridehailing.backend.repository.DriverLocationRepository.class
		)
)
@EnableRedisRepositories(basePackages = "com.ridehailing.backend.repository",
		includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
				type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
				classes = com.ridehailing.backend.repository.DriverLocationRepository.class
		)
)
public class BackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}
}