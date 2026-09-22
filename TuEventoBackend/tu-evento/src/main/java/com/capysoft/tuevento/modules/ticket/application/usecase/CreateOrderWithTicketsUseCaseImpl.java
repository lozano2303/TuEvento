package com.capysoft.tuevento.modules.ticket.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.exception.EventSectionNotFoundException;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.modules.ticket.application.dto.request.CreateOrderRequest;
import com.capysoft.tuevento.modules.ticket.application.dto.response.OrderResponse;
import com.capysoft.tuevento.modules.ticket.application.dto.response.TicketResponse;
import com.capysoft.tuevento.modules.ticket.domain.model.Money;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.model.OrderStatus;
import com.capysoft.tuevento.modules.ticket.domain.model.SeatTicket;
import com.capysoft.tuevento.modules.ticket.domain.model.Ticket;
import com.capysoft.tuevento.modules.ticket.domain.model.TicketLog;
import com.capysoft.tuevento.modules.ticket.domain.model.TicketStatus;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.SeatTicketRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketLogRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;

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
    private final SeatRepository seatRepository;
    private final EventSectionRepository eventSectionRepository;
    
    @Transactional
    public OrderResponse execute(CreateOrderRequest request, Long userId) {
        // 1. Validar que todas las sillas estén reservadas temporalmente por este usuario
        List<Seat> seats = new ArrayList<>();
        for (Integer seatId : request.getSeatIds()) {
            Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));
            
            // Validar que la silla esté reservada por este usuario y la reserva siga vigente
            if (seat.getReservedBy() == null || !seat.getReservedBy().equals(userId.intValue())) {
                throw new IllegalStateException("Seat " + seatId + " is not reserved by this user");
            }
            
            if (seat.getReservedUntil() == null || LocalDateTime.now().isAfter(seat.getReservedUntil())) {
                throw new IllegalStateException("Seat " + seatId + " reservation has expired");
            }
            
            seats.add(seat);
        }
        
        // 2. Obtener precios reales desde EventSection (agrupando por sectionId para eficiencia)
        Set<Integer> uniqueSectionIds = seats.stream()
            .map(Seat::getEventSectionId)
            .collect(Collectors.toSet());
        
        // Traer todas las secciones necesarias en una sola consulta por sección única
        Map<Integer, BigDecimal> sectionPrices = new HashMap<>();
        for (Integer sectionId : uniqueSectionIds) {
            EventSection section = eventSectionRepository.findById(sectionId)
                .orElseThrow(() -> new EventSectionNotFoundException(sectionId));
            sectionPrices.put(sectionId, section.getPrice());
        }
        
        // Calcular precio total sumando el precio real de cada silla según su sección
        BigDecimal totalAmount = seats.stream()
            .map(seat -> sectionPrices.get(seat.getEventSectionId()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        String currency = "COP";
        
        // 3. Crear Order en estado DRAFT
        Order order = Order.builder()
            .userId(userId)
            .eventId(request.getEventId())
            .totalAmount(new Money(totalAmount, currency))
            .status(OrderStatus.DRAFT)
            .build();
        
        Order savedOrder = orderRepository.save(order);
        
        // 4. Crear un Ticket PENDING por cada seat con su precio real
        List<Ticket> tickets = new ArrayList<>();
        for (Seat seat : seats) {
            String ticketCode = generateTicketCode();
            String qrCode = generateQrCode(savedOrder.getOrderId(), ticketCode);
            
            // Obtener precio de la sección de esta silla (ya cacheado en memoria)
            BigDecimal seatPrice = sectionPrices.get(seat.getEventSectionId());
            
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
        for (int i = 0; i < seats.size(); i++) {
            BigDecimal seatPrice = sectionPrices.get(seats.get(i).getEventSectionId());
            
            SeatTicket seatTicket = SeatTicket.builder()
                .seatId(seats.get(i).getSeatId())
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
