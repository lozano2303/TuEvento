package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.OrganizerRoleRevocationEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizerRoleRevocationRepositoryImpl")
class OrganizerRoleRevocationRepositoryImplTest {

    @Mock private OrganizerRoleRevocationJpaRepository jpaRepository;
    @InjectMocks private OrganizerRoleRevocationRepositoryImpl repository;

    @Test
    @DisplayName("record → guarda usuario, admin, motivo y fecha")
    void record_savesRevocation() {
        repository.record(15, 1, "Incumplimiento de las normas");

        ArgumentCaptor<OrganizerRoleRevocationEntity> cap = ArgumentCaptor.forClass(OrganizerRoleRevocationEntity.class);
        verify(jpaRepository).save(cap.capture());
        OrganizerRoleRevocationEntity saved = cap.getValue();
        assertThat(saved.getUserId()).isEqualTo(15);
        assertThat(saved.getRevokedBy()).isEqualTo(1);
        assertThat(saved.getReason()).isEqualTo("Incumplimiento de las normas");
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
