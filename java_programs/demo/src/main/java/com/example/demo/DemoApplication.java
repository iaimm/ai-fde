package com.example.demo;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		MDC.put("traceId", "DemoApplication"); 
		SpringApplication.run(DemoApplication.class, args);
	}

}
 