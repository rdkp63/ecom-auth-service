package com.rdkp63.ecom_auth_services;

import org.springframework.boot.SpringApplication;

public class TestEcomAuthServicesApplication {

	public static void main(String[] args) {
		SpringApplication.from(EcomAuthServicesApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
