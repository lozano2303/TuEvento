package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.payment.application.dto.CreatePaymentCommand;
import com.capysoft.tuevento.modules.payment.application.dto.GatewayPayment;
import com.capysoft.tuevento.modules.payment.application.dto.request.CreatePaymentRequest;
import com.capysoft.tuevento.modules.payment.application.dto.response.PaymentResponse;
import com.capysoft.tuevento.modules.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentGateway;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.ticket.application.port.in.InitiateOrderPaymentUseCase;
import com.capysoft.tuevento.modules.ticket.domain.model.Money;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.model.OrderNotFoundException;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case para crear un pago.
 * 1. Transiciona Order a PAYMENT_PENDING (vía InitiateOrderPaymentUseCase del módulo ticket)
 * 2. Crea pago en el gateway externo
 * 3. Persiste Payment local en PENDING
 */
@Service
@RequiredArgsConstructor
public class CreatePaymentUseCaseImpl {
    
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayPort paymentGatewayPort;
    private final InitiateOrderPaymentUseCase initiateOrderPaymentUseCase;
    
    @Value("${payment.gateway}")
    private String gatewayName;
    
    @Transactional
    public PaymentResponse execute(CreatePaymentRequest request) {
        // 1. Obtener orden
        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));
        
        // 2. Transicionar orden a PAYMENT_PENDING
        initiateOrderPaymentUseCase.initiateOrderPayment(request.getOrderId());
        
        // 3. Crear comando para el gateway
        CreatePaymentCommand command = CreatePaymentCommand.builder()
            .externalReference(order.getOrderId().toString())
            .amount(order.getTotalAmount().getAmount())
            .currency(order.getTotalAmount().getCurrency())
            .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "QR")
            .build();
        
        // 4. Crear pago en el gateway externo
        GatewayPayment gatewayPayment = paymentGatewayPort.createPayment(command);
        
        // 5. Persistir Payment local en PENDING
        Payment payment = Payment.builder()
            .orderId(order.getOrderId())
            .gateway(PaymentGateway.valueOf(gatewayName.toUpperCase()))
            .gatewayTransactionId(gatewayPayment.getPaymentId())
            .status(PaymentStatus.PENDING)
            .amount(new Money(order.getTotalAmount().getAmount(), order.getTotalAmount().getCurrency()))
            .paymentMethod(request.getPaymentMethod() != null ? 
                com.capysoft.tuevento.modules.payment.domain.model.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()) : 
                com.capysoft.tuevento.modules.payment.domain.model.PaymentMethod.QR)
            .build();
        
        Payment savedPayment = paymentRepository.save(payment);
        
        return PaymentResponse.fromDomain(savedPayment);
    }
}
