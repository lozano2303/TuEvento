package com.capysoft.fakepaymentgateway.domain.model;

import java.util.Set;

/**
 * Estados posibles de un pago.
 * Encapsula las transiciones válidas de la máquina de estados.
 */
public enum PaymentStatus {
    PENDING {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(PROCESSING, CANCELLED);
        }
    },
    PROCESSING {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(APPROVED, DECLINED, FAILED);
        }
    },
    APPROVED {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(REFUNDED);
        }
    },
    REFUNDED {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(); // Estado terminal
        }
    },
    DECLINED {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(); // Estado terminal
        }
    },
    FAILED {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(); // Estado terminal
        }
    },
    CANCELLED {
        @Override
        public Set<PaymentStatus> allowedTransitions() {
            return Set.of(); // Estado terminal
        }
    };
    
    /**
     * Retorna los estados a los cuales se puede transicionar desde el estado actual.
     */
    public abstract Set<PaymentStatus> allowedTransitions();
    
    /**
     * Valida si se puede transicionar al nuevo estado.
     */
    public boolean canTransitionTo(PaymentStatus newStatus) {
        return allowedTransitions().contains(newStatus);
    }
    
    /**
     * Verifica si el estado es terminal (no permite más transiciones).
     */
    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
