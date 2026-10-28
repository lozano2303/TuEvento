package com.capysoft.tuevento.modules.security.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileEntity;
import com.capysoft.tuevento.modules.security.application.dto.UserSummary;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.LoginCredentialsEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.RoleEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserStatusEntity;
import com.capysoft.tuevento.shared.infrastructure.config.JpaConfig;

/**
 * Las consultas que Identidad ofrece a los demás servicios, contra una base real (H2 del perfil
 * test): la proyección con LEFT JOIN a perfil y credenciales y el recorrido por cursor.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, GetUserSummariesUseCase.class})
@DisplayName("GetUserSummariesUseCase (JPA)")
class GetUserSummariesUseCaseJpaTest {

    @Autowired TestEntityManager em;
    @Autowired GetUserSummariesUseCase useCase;

    private RoleEntity role;
    private UserStatusEntity status;

    @BeforeEach
    void setUp() {
        role = em.persist(RoleEntity.builder().code("USER").name("User").description("Usuario").build());
        status = em.persist(UserStatusEntity.builder().code("ACTIVE").name("Active").description("Activo").build());
    }

    private UserEntity user(String alias) {
        return em.persist(UserEntity.builder().alias(alias).role(role).userStatus(status).activated(true).build());
    }

    private void credentials(UserEntity user, String email) {
        em.persist(LoginCredentialsEntity.builder().user(user).email(email).passwordHash("hash").build());
    }

    private void profile(UserEntity user, String fullName, Integer storedFileId, Long languageId) {
        em.persist(ProfileEntity.builder().userId(user.getUserId()).fullName(fullName)
                .storedFileId(storedFileId).preferredLanguageId(languageId).build());
    }

    @Test
    @DisplayName("devuelve alias, email, nombre, foto e idioma de cada usuario en una consulta")
    void returnsFullSummaries() {
        UserEntity ana = user("ana");
        credentials(ana, "ana@test.com");
        profile(ana, "Ana García", 15, 2L);
        em.flush();

        Map<Integer, UserSummary> summaries = useCase.getSummaries(List.of(ana.getUserId()));

        assertThat(summaries).containsOnlyKeys(ana.getUserId());
        assertThat(summaries.get(ana.getUserId()))
                .isEqualTo(new UserSummary(ana.getUserId(), "ana", "ana@test.com", "Ana García", 15, 2L));
    }

    @Test
    @DisplayName("un usuario sin perfil ni credenciales sale igual, con esos campos en null")
    void userWithoutProfileOrCredentials() {
        UserEntity bob = user("bob");
        em.flush();

        assertThat(useCase.getSummary(bob.getUserId()))
                .contains(new UserSummary(bob.getUserId(), "bob", null, null, null, null));
    }

    @Test
    @DisplayName("ignora IDs nulos, repetidos y desconocidos")
    void ignoresNullDuplicateAndUnknownIds() {
        UserEntity ana = user("ana");
        UserEntity bob = user("bob");
        em.flush();

        Map<Integer, UserSummary> summaries = useCase.getSummaries(
                Arrays.asList(ana.getUserId(), null, ana.getUserId(), bob.getUserId(), 999_999));

        assertThat(summaries).containsOnlyKeys(ana.getUserId(), bob.getUserId());
        assertThat(useCase.getSummary(999_999)).isEmpty();
        assertThat(useCase.getSummary(null)).isEmpty();
        assertThat(useCase.getSummaries(List.of())).isEmpty();
        assertThat(useCase.getSummaries(null)).isEmpty();
    }

    @Test
    @DisplayName("recorre todos los IDs por cursor, en orden y sin repetir")
    void walksAllIdsByCursor() {
        List<Integer> ids = List.of(user("u1").getUserId(), user("u2").getUserId(), user("u3").getUserId(),
                user("u4").getUserId(), user("u5").getUserId());
        em.flush();

        List<Integer> first = useCase.findUserIdsAfter(0, 2);
        List<Integer> second = useCase.findUserIdsAfter(first.get(1), 2);
        List<Integer> third = useCase.findUserIdsAfter(second.get(1), 2);
        List<Integer> end = useCase.findUserIdsAfter(third.get(0), 2);

        assertThat(first).containsExactly(ids.get(0), ids.get(1));
        assertThat(second).containsExactly(ids.get(2), ids.get(3));
        assertThat(third).containsExactly(ids.get(4));
        assertThat(end).isEmpty();
        assertThat(useCase.countUsers()).isEqualTo(5);
        assertThat(useCase.findUserIdsAfter(0, 0)).isEmpty();
    }
}
