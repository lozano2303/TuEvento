package com.capysoft.tuevento.modules.storage.interfaces.rest;

import com.capysoft.tuevento.modules.storage.application.dto.request.DeleteFileRequest;
import com.capysoft.tuevento.modules.storage.application.dto.request.UploadFileRequest;
import com.capysoft.tuevento.modules.storage.application.dto.response.PublicUrlResponse;
import com.capysoft.tuevento.modules.storage.application.dto.response.StoredFileResponse;
import com.capysoft.tuevento.modules.storage.application.dto.response.UploadFileResponse;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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

    @Operation(summary = "Upload a file — requires authentication")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<UploadFileResponse>> upload(
            @RequestParam MultipartFile file,
            @RequestParam String categoryCode,
            @RequestParam(required = false) Integer ownerEntityId,
            @RequestParam(required = false) String ownerEntityType) throws IOException {

        UploadFileRequest request = UploadFileRequest.builder()
                .fileCategoryCode(categoryCode)
                .uploadedBy(resolveCurrentUserId())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .content(file.getBytes())
                .ownerEntityId(ownerEntityId)
                .ownerEntityType(ownerEntityType)
                .build();

        return ResponseEntity.ok(ApiResponse.ok("File uploaded successfully", uploadFilePort.upload(request)));
    }

    @Operation(summary = "Delete a file — requires authentication")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer fileId) {
        DeleteFileRequest request = DeleteFileRequest.builder()
                .storedFileId(fileId)
                .deletedBy(resolveCurrentUserId())
                .build();

        deleteFilePort.delete(request);
        return ResponseEntity.ok(ApiResponse.ok("File deleted successfully"));
    }

    @Operation(summary = "Get file info — public")
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<StoredFileResponse>> getFile(@PathVariable Integer fileId) {
        return ResponseEntity.ok(ApiResponse.ok("File retrieved", getFilePort.getById(fileId)));
    }

    @Operation(summary = "Get files by owner entity — public")
    @GetMapping("/owner/{ownerEntityType}/{ownerEntityId}")
    public ResponseEntity<ApiResponse<List<StoredFileResponse>>> getByOwner(
            @PathVariable String ownerEntityType,
            @PathVariable Integer ownerEntityId) {

        return ResponseEntity.ok(ApiResponse.ok("Files retrieved",
                getFilesByOwnerPort.getByOwner(ownerEntityId, ownerEntityType)));
    }

    @Operation(summary = "Generate presigned URL for a file — requires authentication")
    @GetMapping("/{fileId}/url")
    public ResponseEntity<ApiResponse<PublicUrlResponse>> generateUrl(@PathVariable Integer fileId) {
        return ResponseEntity.ok(ApiResponse.ok("URL generated", generatePublicUrlPort.generate(fileId)));
    }

    private Integer resolveCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getUserId();
        }
        return null;
    }
}
