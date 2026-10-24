package com.capysoft.tuevento.modules.security.application.port.in;

public interface RevokeOrganizerRolePort {
    void revoke(Integer userId, String reason, Integer adminUserId);
}
