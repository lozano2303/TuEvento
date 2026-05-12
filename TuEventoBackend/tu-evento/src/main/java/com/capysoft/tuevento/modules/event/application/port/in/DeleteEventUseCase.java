package com.capysoft.tuevento.modules.event.application.port.in;

public interface DeleteEventUseCase {

    void execute(Long eventId, Long userId);
}
