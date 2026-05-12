package com.capysoft.tuevento;

import org.springframework.boot.SpringApplication;
import com.capysoft.tuevento.TuEventoApplication;

public class TestTuEventoApplication {

	public static void main(String[] args) {
		SpringApplication.from(TuEventoApplication::main).run(args);
	}

}
