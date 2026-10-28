package com.capysoft.storage.interfaces.rest;

import com.capysoft.common.interfaces.ApiResponse;
import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.application.dto.request.UploadFileRequest;
import com.capysoft.storage.application.dto.response.PublicUrlResponse;
import com.capysoft.storage.application.dto.response.StoredFileResponse;
import com.capysoft.storage.application.dto.response.UploadFileResponse;
import com.capysoft.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.storage.application.port.in.GetFilePort;
import com.capysoft.storage.application.port.in.GetFilesByOwnerPort;
import com.capysoft.storage.application.port.in.UploadFilePort;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * API interna de archivos para el monolito y los demás servicios (con {@code X-Internal-Api-Key},
 * el gateway no la enruta). La subida es la de confianza ({@link UploadFilePort}): quien llama ya
 * validó al usuario y decide categoría y dueño. Las lecturas aplican la política de acceso con el
 * usuario en cuyo nombre se pregunta; sin él, solo se leen las categorías públicas.
 */
@Hidden
@RestController
@RequestMapping("/internal/files")
@RequiredArgsConstructor
public class InternalStorageController {

    private final UploadFilePort        uploadFilePort;
    private final GetFilePort           getFilePort;
    private final GetFilesByOwnerPort   getFilesByOwnerPort;
    private final GeneratePublicUrlPort generatePublicUrlPort;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<UploadFileResponse>> upload(
            @RequestParam MultipartFile file,
            @RequestParam String categoryCode,
            @RequestParam Integer uploadedBy,
            @RequestParam(required = false) String ownerEntityType,
            @RequestParam(required = false) Integer ownerEntityId) throws IOException {

        UploadFileResponse response = uploadFilePort.upload(UploadFileRequest.builder()
                .fileCategoryCode(categoryCode)
                .uploadedBy(uploadedBy)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .content(file.getBytes())
                .ownerEntityType(ownerEntityType)
                .ownerEntityId(ownerEntityId)
                .build());
        return ResponseEntity.ok(ApiResponse.ok("File uploaded successfully", response));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<StoredFileResponse>> getFile(
            @PathVariable Integer fileId,
            @RequestParam(required = false) Integer requesterUserId,
            @RequestParam(required = false) String requesterRole) {
        return ResponseEntity.ok(ApiResponse.ok("File retrieved",
                getFilePort.getById(fileId, requester(requesterUserId, requesterRole))));
    }

    @GetMapping("/{fileId}/url")
    public ResponseEntity<ApiResponse<PublicUrlResponse>> generateUrl(
            @PathVariable Integer fileId,
            @RequestParam(required = false) Integer requesterUserId,
            @RequestParam(required = false) String requesterRole) {
        return ResponseEntity.ok(ApiResponse.ok("URL generated",
                generatePublicUrlPort.generate(fileId, requester(requesterUserId, requesterRole))));
    }

    @GetMapping("/owner/{ownerEntityType}/{ownerEntityId}")
    public ResponseEntity<ApiResponse<List<StoredFileResponse>>> getByOwner(
            @PathVariable String ownerEntityType,
            @PathVariable Integer ownerEntityId) {
        return ResponseEntity.ok(ApiResponse.ok("Files retrieved",
                getFilesByOwnerPort.getByOwner(ownerEntityId, ownerEntityType, StorageRequester.anonymous())));
    }

    private static StorageRequester requester(Integer userId, String role) {
        return userId == null ? StorageRequester.anonymous() : StorageRequester.of(userId, role);
    }
}
