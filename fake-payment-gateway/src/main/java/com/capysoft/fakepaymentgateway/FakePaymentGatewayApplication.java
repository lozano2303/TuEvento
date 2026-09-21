package com.capysoft.fakepaymentgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicación principal del microservicio fake-payment-gateway.
 * Simula una pasarela de pago para desarrollo local de Tu Evento.
 */
@SpringBootApplication
public class FakePaymentGatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(FakePaymentGatewayApplication.class, args);
    }
}
