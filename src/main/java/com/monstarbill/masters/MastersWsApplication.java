package com.monstarbill.masters;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import feign.Logger;

@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
@EnableAutoConfiguration
public class MastersWsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MastersWsApplication.class, args);
	}
	
	@Bean
	Logger.Level fiegnLoggerLevel() {
		return Logger.Level.FULL;
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
					.allowedOrigins("http://43.205.33.156:8080")
					.allowedOrigins("http://43.205.33.156:8082");
			}
		};
	}
}
