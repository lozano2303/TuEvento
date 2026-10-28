package com.capysoft.tuevento.shared.infrastructure.storage;

import com.capysoft.tuevento.shared.application.storage.FileStorage;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.ForbiddenException;
import com.capysoft.tuevento.shared.domain.exception.ImagePolicyViolationException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * {@link FileStorage} sobre la API interna de storage-service ({@code /internal/files}, con la
 * clave entre servicios). Los errores del servicio llegan al usuario igual que cuando el módulo
 * vivía aquí: 404 → {@link NotFoundException}, 403 → {@link ForbiddenException}, 422 (moderación)
 * → {@link ImagePolicyViolationException}, otro 4xx → {@link BusinessException} con su código.
 * Un servicio caído, o una clave rechazada, es un 503 {@code STORAGE_UNAVAILABLE}.
 */
@Slf4j
@Component
public class StorageServiceClient implements FileStorage {

    static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String UNAVAILABLE = "STORAGE_UNAVAILABLE";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public StorageServiceClient(RestClient.Builder builder, ObjectMapper objectMapper,
                                @Value("${app.storage-service.base-url:http://localhost:8085}") String baseUrl,
                                @Value("${app.internal.api-key:}") String apiKey) {
        this(builder.baseUrl(baseUrl).defaultHeader(API_KEY_HEADER, apiKey).requestFactory(timeouts()).build(),
                objectMapper);
    }

    StorageServiceClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope<T>(T data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FileData(Integer storedFileId, String publicUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorBody(String code, String message) {
    }

    private static final ParameterizedTypeReference<Envelope<FileData>> FILE = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<Envelope<List<FileData>>> FILES =
            new ParameterizedTypeReference<>() { };

    @Override
    public StoredFileRef upload(FileUpload upload) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(upload.content()) {
            @Override
            public String getFilename() {
                return upload.originalFilename();
            }
        });
        form.add("categoryCode", upload.categoryCode());
        form.add("uploadedBy", upload.uploadedBy());
        if (upload.ownerEntityType() != null) {
            form.add("ownerEntityType", upload.ownerEntityType());
        }
        if (upload.ownerEntityId() != null) {
            form.add("ownerEntityId", upload.ownerEntityId());
        }
        FileData data = call("upload", () -> restClient.post().uri("/internal/files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve().body(FILE));
        return new StoredFileRef(data.storedFileId(), data.publicUrl());
    }

    @Override
    public StoredFileRef publicUrl(Integer storedFileId, Integer requesterUserId, String requesterRole) {
        FileData data = call("url", () -> restClient.get()
                .uri("/internal/files/{id}/url?requesterUserId={userId}&requesterRole={role}",
                        storedFileId, requesterUserId, requesterRole)
                .retrieve().body(FILE));
        return new StoredFileRef(data.storedFileId(), data.publicUrl());
    }

    @Override
    public Optional<String> findPublicUrl(Integer storedFileId) {
        try {
            return Optional.ofNullable(call("file", () -> restClient.get().uri("/internal/files/{id}", storedFileId)
                    .retrieve().body(FILE)).publicUrl());
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Integer> findIdsByOwner(String ownerEntityType, Integer ownerEntityId) {
        List<FileData> files = call("owner", () -> restClient.get()
                .uri("/internal/files/owner/{type}/{id}", ownerEntityType, ownerEntityId)
                .retrieve().body(FILES));
        return files.stream().map(FileData::storedFileId).toList();
    }

    @FunctionalInterface
    private interface Request<T> {
        Envelope<T> send();
    }

    private <T> T call(String what, Request<T> request) {
        Envelope<T> body;
        try {
            body = request.send();
        } catch (HttpClientErrorException e) {
            throw translate(what, e);
        } catch (RestClientException e) {
            throw unavailable(what, e);
        }
        if (body == null || body.data() == null) {
            throw new ServiceUnavailableException(UNAVAILABLE, "Storage returned no data for " + what);
        }
        return body.data();
    }

    private RuntimeException translate(String what, HttpClientErrorException e) {
        HttpStatusCode status = e.getStatusCode();
        if (status.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            // La clave entre servicios no coincide: es un fallo de configuración, no del usuario
            return unavailable(what, e);
        }
        ErrorBody error = errorBody(e);
        String code = error.code() != null ? error.code() : "STORAGE_ERROR";
        String message = error.message() != null ? error.message() : e.getStatusText();
        if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return new NotFoundException(code, message);
        }
        if (status.isSameCodeAs(HttpStatus.FORBIDDEN)) {
            return new ForbiddenException(code, message);
        }
        if (status.isSameCodeAs(HttpStatus.UNPROCESSABLE_ENTITY)) {
            return new ImagePolicyViolationException(code);
        }
        return new BusinessException(code, message);
    }

    private ErrorBody errorBody(HttpClientErrorException e) {
        try {
            ErrorBody body = objectMapper.readValue(e.getResponseBodyAsByteArray(), ErrorBody.class);
            return body != null ? body : new ErrorBody(null, null);
        } catch (Exception ignored) {
            return new ErrorBody(null, null);
        }
    }

    private static ServiceUnavailableException unavailable(String what, Exception e) {
        log.warn("Storage service unavailable ({}): {}", what, e.getMessage());
        return new ServiceUnavailableException(UNAVAILABLE, "The file service is not available, try again later");
    }

    /** Subir pasa por la moderación de imágenes (servicio externo): lectura con margen amplio. */
    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return factory;
    }
}
