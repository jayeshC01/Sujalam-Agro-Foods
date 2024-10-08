package com.gryffindor.excalibur;

import org.springframework.boot.SpringApplication;

public class TestSujalamAgroFoodsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(SujalamAgroFoodsBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
