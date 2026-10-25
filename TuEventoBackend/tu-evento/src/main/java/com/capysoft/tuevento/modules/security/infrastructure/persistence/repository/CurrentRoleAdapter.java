package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.application.port.out.CurrentRolePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CurrentRoleAdapter implements CurrentRolePort {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<String> findRoleCode(Integer userId) {
        return Optional.ofNullable(userJpaRepository.findRoleCodeByUserId(userId));
    }
}
