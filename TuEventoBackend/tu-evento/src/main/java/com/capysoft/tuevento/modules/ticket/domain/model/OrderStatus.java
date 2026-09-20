package com.capysoft.tuevento.modules.ticket.domain.model;

import java.util.Set;

/**
 * Estados posibles de una orden de compra.
 * Encapsula las transiciones válidas de la máquina de estados.
 */
public enum OrderStatus {
    DRAFT {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(PAYMENT_PENDING, CANCELLED);
        }
    },
    PAYMENT_PENDING {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(PAID, CANCELLED);
        }
    },
    PAID {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(USED, REFUNDED);
        }
    },
    CANCELLED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(); // Estado terminal
        }
    },
    REFUNDED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(); // Estado terminal
        }
    },
    USED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Set.of(); // Estado terminal
        }
    };
    
    /**
     * Retorna los estados a los cuales se puede transicionar desde el estado actual.
     */
    public abstract Set<OrderStatus> allowedTransitions();
    
    /**
     * Valida si se puede transicionar al nuevo estado.
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        return allowedTransitions().contains(newStatus);
    }
    
    /**
     * Verifica si el estado es terminal (no permite más transiciones).
     */
    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
