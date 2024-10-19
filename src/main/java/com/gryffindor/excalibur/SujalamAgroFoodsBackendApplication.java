package com.gryffindor.excalibur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class SujalamAgroFoodsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SujalamAgroFoodsBackendApplication.class, args);
		System.out.println("Sujalam Agro Foods Application started");
	}
}
