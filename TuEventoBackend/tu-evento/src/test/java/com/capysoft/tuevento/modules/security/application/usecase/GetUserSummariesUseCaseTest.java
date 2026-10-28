package com.capysoft.tuevento.modules.security.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.capysoft.tuevento.modules.security.application.dto.UserSummary;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserSummariesUseCase")
class GetUserSummariesUseCaseTest {

    @Mock UserJpaRepository userJpaRepository;
    @InjectMocks GetUserSummariesUseCase useCase;

    @Test
    @DisplayName("parte las listas grandes en bloques de 500 IDs")
    @SuppressWarnings("unchecked")
    void splitsLargeListsIntoChunks() {
        List<Integer> ids = IntStream.rangeClosed(1, 1200).boxed().toList();
        when(userJpaRepository.findSummariesByUserIds(anyCollection())).thenReturn(List.of());

        useCase.getSummaries(ids);

        ArgumentCaptor<Collection<Integer>> chunks = ArgumentCaptor.forClass(Collection.class);
        verify(userJpaRepository, times(3)).findSummariesByUserIds(chunks.capture());
        assertThat(chunks.getAllValues()).extracting(Collection::size).containsExactly(500, 500, 200);
    }

    @Test
    @DisplayName("si un usuario tuviera dos filas de credenciales, se queda la primera")
    void keepsFirstRowPerUser() {
        when(userJpaRepository.findSummariesByUserIds(anyCollection())).thenReturn(List.of(
                new UserSummary(1, "ana", "ana@test.com", "Ana", null, null),
                new UserSummary(1, "ana", "otra@test.com", "Ana", null, null)));

        assertThat(useCase.getSummaries(List.of(1)).get(1).email()).isEqualTo("ana@test.com");
    }
}
