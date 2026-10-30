package com.capysoft.tuevento.modules.payment.application.port.in;

/** Reembolsa los pagos aprobados de un evento que Catálogo acaba de cancelar. */
public interface RefundCancelledEventUseCase {

    void execute(Long eventId);
}
