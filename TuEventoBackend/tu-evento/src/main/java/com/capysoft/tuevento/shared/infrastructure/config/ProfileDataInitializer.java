package com.capysoft.tuevento.shared.infrastructure.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.identitybridge.client.IdentityServiceClient;
import com.capysoft.tuevento.shared.application.storage.FileStorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Asigna el avatar por defecto a los perfiles que no tienen foto (los creados antes de que
 * existiera). El avatar lo sube storage-service al arrancar; los perfiles son de identity-service.
 * Si alguno de los dos aún no responde (arranque simultáneo), se reintenta en el próximo arranque.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ProfileDataInitializer implements ApplicationRunner {

    static final String OWNER_ENTITY_TYPE = "DEFAULT_AVATAR";
    static final int    OWNER_ENTITY_ID   = 0;

    private final FileStorage           fileStorage;
    private final IdentityServiceClient identityServiceClient;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Integer> avatars = fileStorage.findIdsByOwner(OWNER_ENTITY_TYPE, OWNER_ENTITY_ID);
            if (avatars.isEmpty()) {
                log.info("The default avatar is not uploaded yet: profiles keep their current picture");
                return;
            }
            int assigned = identityServiceClient.assignDefaultAvatar(avatars.get(0));
            log.info("Default avatar (storedFileId={}) assigned to {} profile(s)", avatars.get(0), assigned);
        } catch (Exception e) {
            log.warn("Could not assign the default avatar to profiles now: {}", e.getMessage());
        }
    }
}
