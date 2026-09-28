package com.capysoft.tuevento.modules.wallet.application.usecase;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.wallet.application.dto.WalletTransactionResponse;
import com.capysoft.tuevento.modules.wallet.domain.model.Wallet;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletNotFoundException;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletReference;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletReferenceEntityType;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletReferenceRepository;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletRepository;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletTransactionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Retorna el historial de movimientos enriquecido de la wallet del usuario.
 * Incluye entityType, entityId y eventName cuando aplica.
 */
@Service
@RequiredArgsConstructor
public class GetWalletTransactionsUseCaseImpl {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletReferenceRepository walletReferenceRepository;
    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> execute(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new WalletNotFoundException(userId));

        List<WalletTransactionResponse> transactions = walletTransactionRepository
            .findByWalletId(wallet.getWalletId())
            .stream()
            .map(WalletTransactionResponse::fromDomain)
            .collect(Collectors.toList());

        // Enriquecer cada transacción con wallet_reference y event data
        return transactions.stream()
            .map(this::enrichTransaction)
            .collect(Collectors.toList());
    }

    private WalletTransactionResponse enrichTransaction(WalletTransactionResponse tx) {
        // Buscar wallet_reference para esta transacción
        List<WalletReference> references = walletReferenceRepository.findByTransactionId(tx.getTransactionId());
        if (references.isEmpty()) {
            return tx; // Sin referencia, retornar tal cual
        }

        WalletReference ref = references.get(0); // Tomar la primera (debería ser única)

        // Construir respuesta con entityType y entityId
        WalletTransactionResponse.WalletTransactionResponseBuilder builder = WalletTransactionResponse.builder()
            .transactionId(tx.getTransactionId())
            .walletId(tx.getWalletId())
            .type(tx.getType())
            .amount(tx.getAmount())
            .status(tx.getStatus())
            .balanceAfter(tx.getBalanceAfter())
            .idempotencyKey(tx.getIdempotencyKey())
            .createdAt(tx.getCreatedAt())
            .createdBy(tx.getCreatedBy())
            .entityType(ref.getEntityType())
            .entityId(ref.getEntityId());

        // Si es ORDER, intentar obtener el nombre del evento
        if (ref.getEntityType() == WalletReferenceEntityType.ORDER && ref.getEntityId() != null) {
            orderRepository.findById(ref.getEntityId()).ifPresent(order ->
                eventRepository.findById(order.getEventId()).ifPresent(event ->
                    builder.eventName(event.getEventName())
                )
            );
        }

        return builder.build();
    }
}
