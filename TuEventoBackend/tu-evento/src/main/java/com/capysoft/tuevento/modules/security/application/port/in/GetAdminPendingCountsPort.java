package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.response.AdminPendingCountsResponse;

public interface GetAdminPendingCountsPort {
    /**
     * Devuelve cuántas solicitudes de organizador y cuántos eventos
     * esperan revisión del administrador.
     */
    AdminPendingCountsResponse getCounts();
}
