package com.capysoft.tuevento;

import com.capysoft.tuevento.shared.infrastructure.config.DataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
class TuEventoApplicationTests {

	// H2 arranca sin Liquibase (sin roles ni estados semilla): se omite la creación del admin
	@MockitoBean
	private DataInitializer dataInitializer;

	@Test
	void contextLoads() {
	}

}
