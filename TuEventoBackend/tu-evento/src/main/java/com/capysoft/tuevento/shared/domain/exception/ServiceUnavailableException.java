package com.capysoft.tuevento.shared.domain.exception;

/**
 * Un servicio del que depende la operación no respondió. Se traduce a 503 para que el
 * cliente pueda reintentar, en vez de confundirlo con un dato inexistente.
 */
public class ServiceUnavailableException extends BusinessException {

    public ServiceUnavailableException(String code, String message) {
        super(code, message);
    }
}
