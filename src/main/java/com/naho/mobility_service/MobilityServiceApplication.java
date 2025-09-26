package com.naho.mobility_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling //스케줄링 기능 활성화
@SpringBootApplication
public class MobilityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MobilityServiceApplication.class, args);
	}

}
