package com.capysoft.tuevento.modules.payment.application.port.out;

import java.util.Collection;
import java.util.Map;

/** Datos de los compradores para el panel de reembolsos. Los aporta Identidad. */
public interface BuyerDirectoryPort {

    /** Compradores pedidos por userId; los desconocidos no aparecen. */
    Map<Integer, Buyer> findBuyers(Collection<Integer> userIds);

    record Buyer(String alias, String fullName, String email) {
    }
}
