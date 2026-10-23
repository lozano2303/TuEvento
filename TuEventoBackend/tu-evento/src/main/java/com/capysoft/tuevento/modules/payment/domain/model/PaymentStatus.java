package com.capysoft.tuevento.modules.payment.domain.model;

import java.util.Set;

/**
 * Estados posibles de un pago.
 * Encapsula las transiciones válidas de la máquina de estados.
 */
public enum PaymentStatus {
    PENDING {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(APPROVED, REJECTED, ERROR);
        }
    },
    APPROVED {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(REFUNDED);
        }
    },
    REJECTED {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(); // Estado terminal
        }
    },
    ERROR {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(); // Estado terminal
        }
    },
    REFUNDED {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(); // Estado terminal
        }
    };
    
    public abstract Set<PaymentStatus> allowedTransitions();
    
    public boolean canTransitionTo(PaymentStatus newStatus) {
        return allowedTransitions().contains(newStatus);
    }
    
    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
