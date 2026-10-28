package com.capysoft.identity.security.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.identity.security.application.port.out.OrganizerDocumentPort;
import com.capysoft.identity.shared.domain.exception.ServiceUnavailableException;
import com.capysoft.identity.shared.infrastructure.client.MonolithInternalApiConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Documentos de las solicitudes de organizador en Archivos, por la API interna del monolito
 * ({@code /internal/organizer-documents}): la subida pasa por la misma moderación y el mismo bucket
 * que siempre. Un rechazo de Archivos (formato, tamaño, imagen no permitida) llega como 400 con su
 * código; un fallo del servicio, como 503.
 */
@Slf4j
@Component
public class MonolithOrganizerDocumentClient implements OrganizerDocumentPort {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MonolithOrganizerDocumentClient(@Qualifier(MonolithInternalApiConfig.BEAN) RestClient restClient,
                                           ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope<T>(T data, String code, String message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UploadData(Integer storedFileId) {
    }

    @Override
    public Integer upload(DocumentUpload upload) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("fileCategoryCode", upload.fileCategoryCode());
        body.part("ownerEntityType", upload.ownerEntityType());
        body.part("uploadedBy", String.valueOf(upload.uploadedBy()));
        body.part("file", new ByteArrayResource(upload.content()) {
            @Override
            public String getFilename() {
                return upload.originalFilename();
            }
        }).contentType(MediaType.parseMediaType(
                upload.contentType() != null ? upload.contentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        try {
            Envelope<UploadData> response = restClient.post().uri("/internal/organizer-documents")
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(body.build())
                    .retrieve().body(new ParameterizedTypeReference<Envelope<UploadData>>() { });
            if (response == null || response.data() == null || response.data().storedFileId() == null) {
                throw unavailable("empty upload response");
            }
            return response.data().storedFileId();
        } catch (HttpClientErrorException e) {
            throw rejected(e);
        } catch (RestClientException e) {
            throw unavailable(e.getMessage());
        }
    }

    @Override
    public DocumentUrl publicUrl(Integer storedFileId, Integer requesterUserId, String requesterRole) {
        try {
            Envelope<DocumentUrl> response = restClient.get()
                    .uri("/internal/organizer-documents/{id}/url?requesterUserId={userId}&requesterRole={role}",
                            storedFileId, requesterUserId, requesterRole)
                    .retrieve().body(new ParameterizedTypeReference<Envelope<DocumentUrl>>() { });
            if (response == null || response.data() == null) {
                throw unavailable("empty url response");
            }
            return response.data();
        } catch (HttpClientErrorException e) {
            throw rejected(e);
        } catch (RestClientException e) {
            throw unavailable(e.getMessage());
        }
    }

    /** Conserva el código de negocio que devolvió Archivos (p. ej. tipo de archivo no permitido). */
    private BusinessException rejected(HttpClientErrorException e) {
        try {
            Envelope<?> error = objectMapper.readValue(e.getResponseBodyAsString(), Envelope.class);
            if (error != null && error.message() != null) {
                return new BusinessException(error.code() != null ? error.code() : "ORGANIZER_DOCUMENT_REJECTED",
                        error.message());
            }
        } catch (Exception ignored) {
            // cuerpo no legible: se usa el mensaje genérico
        }
        return new BusinessException("ORGANIZER_DOCUMENT_REJECTED", "The document could not be processed");
    }

    private static ServiceUnavailableException unavailable(String reason) {
        log.warn("Organizer document storage unavailable: {}", reason);
        return new ServiceUnavailableException("STORAGE_UNAVAILABLE",
                "The storage service is not available, try again later");
    }
}
