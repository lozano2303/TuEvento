package com.capysoft.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway de TuEvento.
 *
 * <p>Se ejecuta delante del monolito (y en el futuro, de cada microservicio).
 * Los frontends web y móvil siguen usando exactamente la misma base URL
 * ({@code /api/v1/...} y {@code /ws}) — el gateway los enruta de forma transparente.
 *
 * <p>Responsabilidades actuales (Fase 0):
 * <ul>
 *   <li>Enrutar todo {@code /api/v1/**} y {@code /ws/**} al monolito.
 *   <li>Exponer el health check en {@code /actuator/health}.
 *   <li>CORS centralizado (los frontends apuntan al gateway, no al monolito).
 * </ul>
 *
 * <p>Responsabilidades futuras (Fases 1-5):
 * <ul>
 *   <li>Validación de JWT con JWKS ({@code /.well-known/jwks.json} del servicio Identidad).
 *   <li>Rate limiting por IP con Redis.
 *   <li>Enrutar prefijos a los microservicios extraídos ({@code /api/v1/storage/**} → Archivos, etc.).
 *   <li>Circuit breaker por ruta con Resilience4j.
 * </ul>
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
