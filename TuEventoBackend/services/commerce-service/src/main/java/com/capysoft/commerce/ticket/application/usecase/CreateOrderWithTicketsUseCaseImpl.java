package com.capysoft.commerce.ticket.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.commerce.ticket.application.port.out.CatalogSeatsPort;
import com.capysoft.commerce.ticket.application.port.out.CatalogSeatsPort.HeldSeat;
import com.capysoft.commerce.ticket.application.port.out.CatalogSeatsPort.HeldSeats;
import com.capysoft.commerce.ticket.application.dto.request.CreateOrderRequest;
import com.capysoft.commerce.ticket.application.dto.response.OrderResponse;
import com.capysoft.commerce.ticket.application.dto.response.TicketResponse;
import com.capysoft.commerce.ticket.domain.model.Money;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.model.OrderStatus;
import com.capysoft.commerce.ticket.domain.model.SeatTicket;
import com.capysoft.commerce.ticket.domain.model.Ticket;
import com.capysoft.commerce.ticket.domain.model.TicketLog;
import com.capysoft.commerce.ticket.domain.model.TicketStatus;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;
import com.capysoft.commerce.ticket.domain.repository.SeatTicketRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketLogRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

/**
 * Use case para crear una orden con tickets desde sillas reservadas.
 */
@Service
@RequiredArgsConstructor
public class CreateOrderWithTicketsUseCaseImpl {
    
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final SeatTicketRepository seatTicketRepository;
    private final TicketLogRepository ticketLogRepository;
    private final CatalogSeatsPort catalogSeatsPort;

    private static final String DEFAULT_CURRENCY = "COP";
    
    @Transactional
    public OrderResponse execute(CreateOrderRequest request, Long userId) {
        // 1. Crear la Order en DRAFT: hace falta su id para bloquear las sillas a su nombre.
        // Si el bloqueo falla, la transacción se deshace y la orden no llega a existir
        // Las sillas se bloquean en orden ascendente (dos órdenes con sillas cruzadas no se
        // interbloquean) y sin repetidas (una silla enviada dos veces daría dos entradas)
        List<Integer> seatIds = request.getSeatIds().stream().distinct().sorted().toList();
        Order savedOrder = orderRepository.save(Order.builder()
            .userId(userId)
            .eventId(request.getEventId())
            .totalAmount(new Money(BigDecimal.ZERO, DEFAULT_CURRENCY))
            .status(OrderStatus.DRAFT)
            .build());

        // 2. Bloquear las sillas en Catálogo: reservadas por este usuario, vigentes y del evento.
        // Devuelve el precio real de cada una, que la orden y sus entradas copian
        HeldSeats held = catalogSeatsPort.holdForOrder(savedOrder.getOrderId(), userId, request.getEventId(), seatIds);
        String currency = held.currency();

        // 3. Fijar el total con los precios de las sillas
        savedOrder.assignTotal(new Money(held.total(), currency));
        savedOrder = orderRepository.save(savedOrder);
        
        // 4. Crear un Ticket PENDING por cada seat con su precio real
        List<Ticket> tickets = new ArrayList<>();
        for (HeldSeat seat : held.seats()) {
            String ticketCode = generateTicketCode();
            String qrCode = generateQrCode(savedOrder.getOrderId(), ticketCode);
            
            // Obtener precio de la sección de esta silla (ya cacheado en memoria)
            BigDecimal seatPrice = seat.price();
            
            Ticket ticket = Ticket.builder()
                .eventId(request.getEventId())
                .userId(userId)
                .orderId(savedOrder.getOrderId())
                .code(ticketCode)
                .qrCode(qrCode)
                .status(TicketStatus.PENDING)
                .expirationDate(calculateExpirationDate(request.getEventId()))
                .totalPrice(new Money(seatPrice, currency))
                .build();
            
            tickets.add(ticket);
        }
        
        List<Ticket> savedTickets = ticketRepository.saveAll(tickets);
        
        // 5. Crear SeatTicket con price snapshot del precio real
        List<SeatTicket> seatTickets = new ArrayList<>();
        for (int i = 0; i < held.seats().size(); i++) {
            BigDecimal seatPrice = held.seats().get(i).price();
            
            SeatTicket seatTicket = SeatTicket.builder()
                .seatId(held.seats().get(i).seatId())
                .ticketId(savedTickets.get(i).getTicketId())
                .price(seatPrice)
                .build();
            seatTickets.add(seatTicket);
        }
        
        seatTicketRepository.saveAll(seatTickets);
        
        // 6. Generar logs de auditoría para cada ticket
        String changedBy = getCurrentUsername();
        for (Ticket ticket : savedTickets) {
            TicketLog log = TicketLog.builder()
                .ticketId(ticket.getTicketId())
                .oldStatus(null)
                .newStatus(TicketStatus.PENDING)
                .changedAt(LocalDateTime.now())
                .changedBy(changedBy)
                .reason("Order created")
                .build();
            ticketLogRepository.save(log);
        }
        
        // 7. Las sillas permanecen reservadas hasta que se confirme el pago
        // No se liberan aquí, se confirman definitivamente en ConfirmOrderPaymentUseCase
        
        // 8. Construir respuesta
        List<TicketResponse> ticketResponses = savedTickets.stream()
            .map(TicketResponse::fromDomain)
            .collect(Collectors.toList());
        
        return OrderResponse.fromDomainWithTickets(savedOrder, ticketResponses);
    }
    
    private String generateTicketCode() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private String generateQrCode(Long orderId, String ticketCode) {
        // Genera string que codifica ticketId+hash para validación
        // No genera imagen, solo el string que el frontend usará
        return String.format("TUEVENTO:%s:%s:%s", 
            orderId, 
            ticketCode,
            UUID.randomUUID().toString().substring(0, 8));
    }
    
    private LocalDateTime calculateExpirationDate(Long eventId) {
        // Por ahora retorna 30 días desde ahora
        // En producción debería obtener la fecha del evento y calcular basado en eso
        return LocalDateTime.now().plusDays(30);
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}
