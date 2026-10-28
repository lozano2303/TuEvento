package com.capysoft.identity.security.application.usecase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.identity.security.application.dto.UserSummary;
import com.capysoft.identity.security.application.port.in.GetUserSummariesPort;
import com.capysoft.identity.security.application.port.in.ListUserIdsPort;
import com.capysoft.identity.security.infrastructure.persistence.repository.UserJpaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lecturas de usuarios que Identidad ofrece a los demás servicios. Las consultas son en lote:
 * los IDs se agrupan en bloques de {@value #CHUNK_SIZE} para no pasar del límite de parámetros
 * de la base con listas muy grandes.
 */
@Service
@RequiredArgsConstructor
public class GetUserSummariesUseCase implements GetUserSummariesPort, ListUserIdsPort {

    static final int CHUNK_SIZE = 500;

    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, UserSummary> getSummaries(Collection<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Integer> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<Integer, UserSummary> byId = new LinkedHashMap<>();
        for (List<Integer> chunk : chunks(ids)) {
            // Si un usuario tuviera dos filas de credenciales, se queda la primera
            userJpaRepository.findSummariesByUserIds(chunk)
                    .forEach(summary -> byId.putIfAbsent(summary.userId(), summary));
        }
        return byId;
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsers() {
        return userJpaRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> findUserIdsAfter(int afterUserId, int limit) {
        if (limit < 1) {
            return List.of();
        }
        return userJpaRepository.findUserIdsAfter(afterUserId, PageRequest.of(0, limit));
    }

    private static List<List<Integer>> chunks(List<Integer> ids) {
        List<List<Integer>> chunks = new ArrayList<>();
        for (int from = 0; from < ids.size(); from += CHUNK_SIZE) {
            chunks.add(ids.subList(from, Math.min(from + CHUNK_SIZE, ids.size())));
        }
        return chunks;
    }
}
