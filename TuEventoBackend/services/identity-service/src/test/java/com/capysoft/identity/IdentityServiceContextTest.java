package com.capysoft.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.capysoft.identity.security.application.port.out.AdminPanelSourcesPort;
import com.capysoft.identity.security.application.port.out.OrganizerDocumentPort;
import com.capysoft.identity.security.application.port.out.TokenRevocationPort;

/**
 * Arranca el contexto completo (con H2 en vez de PostgreSQL): falla si falta un bean, como pasaría
 * al desplegar. Solo aquí se permite un par de claves RSA en memoria; en el servicio real la clave
 * es obligatoria.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:identity-context;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;NON_KEYWORDS=ROW,VALUE,USER,KEY,YEAR,MONTH,DAY",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.ephemeral-keys-allowed=true",
        "app.jwt.private-key-path=classpath:keys/does-not-exist.pem"
})
@DisplayName("identity-service — contexto completo")
class IdentityServiceContextTest {

    @Autowired private ApplicationContext context;

    @Test
    @DisplayName("arranca con todos sus adaptadores (revocación, panel de admin, documentos)")
    void contextLoads() {
        assertThat(context.getBean(TokenRevocationPort.class)).isNotNull();
        assertThat(context.getBean(AdminPanelSourcesPort.class)).isNotNull();
        assertThat(context.getBean(OrganizerDocumentPort.class)).isNotNull();
    }
}
