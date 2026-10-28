package com.capysoft.tuevento.shared.interfaces.advice;

import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler — servicio no disponible")
class GlobalExceptionHandlerServiceUnavailableTest {

    @Test
    @DisplayName("ServiceUnavailableException responde 503 con su código")
    void mapsTo503() {
        ResponseEntity<ApiResponse<Void>> response = new GlobalExceptionHandler()
                .handleServiceUnavailableException(
                        new ServiceUnavailableException("GEOLOCATION_UNAVAILABLE", "down"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("GEOLOCATION_UNAVAILABLE");
        assertThat(response.getBody().isSuccess()).isFalse();
    }
}
