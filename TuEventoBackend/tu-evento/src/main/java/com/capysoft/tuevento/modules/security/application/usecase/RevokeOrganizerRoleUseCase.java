package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.port.in.RevokeOrganizerRolePort;
import com.capysoft.tuevento.modules.security.domain.event.OrganizerRoleRevokedEvent;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.repository.RoleRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RevokeOrganizerRoleUseCase implements RevokeOrganizerRolePort {

    private final UserRepository           userRepository;
    private final RoleRepository           roleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void revoke(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        if (!"ORGANIZER".equals(user.getRole().getCode())) {
            throw new BusinessException("USER_NOT_ORGANIZER",
                    "User " + userId + " does not have the ORGANIZER role");
        }

        var userRole = roleRepository.findByCode("USER")
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "USER role not found"));

        user.setRole(userRole);
        userRepository.save(user);

        eventPublisher.publishEvent(OrganizerRoleRevokedEvent.builder()
                .userId(userId)
                .alias(user.getAlias())
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
