package br.com.aprendi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@PropertySource({"classpath:application.properties"})
@SpringBootApplication
public class AprendiApplication {
	public static void main(String[] args) {
		SpringApplication.run(AprendiApplication.class, args);
	}
}
