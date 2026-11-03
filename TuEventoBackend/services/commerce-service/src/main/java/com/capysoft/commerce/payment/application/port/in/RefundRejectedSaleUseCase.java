package com.capysoft.commerce.payment.application.port.in;

/** Catálogo no pudo vender las sillas de una orden ya pagada: se devuelve el dinero a la wallet. */
public interface RefundRejectedSaleUseCase {

    void execute(Long orderId, String reason);
}
