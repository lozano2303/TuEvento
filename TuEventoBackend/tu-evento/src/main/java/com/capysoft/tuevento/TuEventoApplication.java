package com.capysoft.tuevento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TuEventoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TuEventoApplication.class, args);
	}

}
