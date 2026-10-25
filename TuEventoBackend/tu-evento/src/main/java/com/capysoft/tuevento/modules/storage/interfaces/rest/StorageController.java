package com.capysoft.tuevento.modules.storage.interfaces.rest;

import com.capysoft.tuevento.modules.storage.application.dto.request.DeleteFileRequest;
import com.capysoft.tuevento.modules.storage.application.dto.request.UploadFileRequest;
import com.capysoft.tuevento.modules.storage.application.dto.response.PublicUrlResponse;
import com.capysoft.tuevento.modules.storage.application.dto.response.StoredFileResponse;
import com.capysoft.tuevento.modules.storage.application.dto.response.UploadFileResponse;
import com.capysoft.tuevento.modules.storage.application.port.in.CheckFileAccessPort;
import com.capysoft.tuevento.modules.storage.application.port.in.DeleteFilePort;
import com.capysoft.tuevento.modules.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.tuevento.modules.storage.application.port.in.GetFilePort;
import com.capysoft.tuevento.modules.storage.application.port.in.GetFilesByOwnerPort;
import com.capysoft.tuevento.modules.storage.application.port.in.UploadFilePort;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Todos los endpoints exigen autenticación. Los archivos privados (documentos
 * de organizador) solo los ve o borra quien los subió o un administrador.
 */
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "File storage endpoints")
public class StorageController {

    private final UploadFilePort        uploadFilePort;
    private final DeleteFilePort        deleteFilePort;
    private final GetFilePort           getFilePort;
    private final GetFilesByOwnerPort   getFilesByOwnerPort;
    private final GeneratePublicUrlPort generatePublicUrlPort;
    private final CheckFileAccessPort   checkFileAccessPort;

    @Operation(summary = "Upload a file — requires authentication")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<UploadFileResponse>> upload(
            @RequestParam MultipartFile file,
            @RequestParam String categoryCode,
            @RequestParam(required = false) Integer ownerEntityId,
            @RequestParam(required = false) String ownerEntityType,
            @AuthenticationPrincipal SecurityUser principal) throws IOException {

        UploadFileRequest request = UploadFileRequest.builder()
                .fileCategoryCode(categoryCode)
                .uploadedBy(principal.getUserId())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .content(file.getBytes())
                .ownerEntityId(ownerEntityId)
                .ownerEntityType(ownerEntityType)
                .build();

        return ResponseEntity.ok(ApiResponse.ok("File uploaded successfully", uploadFilePort.upload(request)));
    }

    @Operation(summary = "Delete a file — uploader or admin only")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Integer fileId,
            @AuthenticationPrincipal SecurityUser principal) {
        checkFileAccessPort.assertCanDelete(fileId, principal.getUserId(), principal.isAdmin());

        DeleteFileRequest request = DeleteFileRequest.builder()
                .storedFileId(fileId)
                .deletedBy(principal.getUserId())
                .build();

        deleteFilePort.delete(request);
        return ResponseEntity.ok(ApiResponse.ok("File deleted successfully"));
    }

    @Operation(summary = "Get file info — public categories or uploader/admin")
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<StoredFileResponse>> getFile(
            @PathVariable Integer fileId,
            @AuthenticationPrincipal SecurityUser principal) {
        checkFileAccessPort.assertCanRead(fileId, principal.getUserId(), principal.isAdmin());
        return ResponseEntity.ok(ApiResponse.ok("File retrieved", getFilePort.getById(fileId)));
    }

    @Operation(summary = "Get files by owner entity — only the files the requester can see")
    @GetMapping("/owner/{ownerEntityType}/{ownerEntityId}")
    public ResponseEntity<ApiResponse<List<StoredFileResponse>>> getByOwner(
            @PathVariable String ownerEntityType,
            @PathVariable Integer ownerEntityId,
            @AuthenticationPrincipal SecurityUser principal) {

        return ResponseEntity.ok(ApiResponse.ok("Files retrieved",
                getFilesByOwnerPort.getByOwner(ownerEntityId, ownerEntityType,
                        principal.getUserId(), principal.isAdmin())));
    }

    @Operation(summary = "Generate presigned URL — public categories or uploader/admin")
    @GetMapping("/{fileId}/url")
    public ResponseEntity<ApiResponse<PublicUrlResponse>> generateUrl(
            @PathVariable Integer fileId,
            @AuthenticationPrincipal SecurityUser principal) {
        checkFileAccessPort.assertCanRead(fileId, principal.getUserId(), principal.isAdmin());
        return ResponseEntity.ok(ApiResponse.ok("URL generated", generatePublicUrlPort.generate(fileId)));
    }
}
