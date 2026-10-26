package com.capysoft.storage.interfaces.rest;

import com.capysoft.storage.application.dto.request.DeleteFileRequest;
import com.capysoft.storage.application.dto.request.StorageRequester;
import com.capysoft.storage.application.dto.response.PublicUrlResponse;
import com.capysoft.storage.application.dto.response.StoredFileResponse;
import com.capysoft.storage.application.dto.response.UploadFileResponse;
import com.capysoft.storage.application.port.in.DeleteFilePort;
import com.capysoft.storage.application.port.in.GeneratePublicUrlPort;
import com.capysoft.storage.application.port.in.GetFilePort;
import com.capysoft.storage.application.port.in.GetFilesByOwnerPort;
import com.capysoft.storage.application.port.in.UploadUserFilePort;
import com.capysoft.common.infrastructure.security.SecurityUser;
import com.capysoft.common.interfaces.ApiResponse;
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
 * Every endpoint requires authentication (SecurityConfig). Access to each file is decided by
 * StorageAccessPolicy inside the use cases; the controller only passes the authenticated user.
 */
@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "File storage endpoints")
public class StorageController {

    private final UploadUserFilePort    uploadUserFilePort;
    private final DeleteFilePort        deleteFilePort;
    private final GetFilePort           getFilePort;
    private final GetFilesByOwnerPort   getFilesByOwnerPort;
    private final GeneratePublicUrlPort generatePublicUrlPort;

    @Operation(summary = "Upload a profile picture — requires authentication",
               description = "Only PROFILE_PICTURE can be uploaded here; the uploader is the authenticated user. "
                       + "Errors: STORAGE_AUTHENTICATION_REQUIRED (401), STORAGE_CATEGORY_NOT_ALLOWED (403), "
                       + "FILE_CATEGORY_NOT_FOUND (404), INVALID_FILE_EXTENSION, FILE_TOO_LARGE, "
                       + "S3_UPLOAD_FAILED, UPLOAD_FAILED (400), image policy violation (422)")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<UploadFileResponse>> upload(
            @RequestParam MultipartFile file,
            @RequestParam String categoryCode,
            @AuthenticationPrincipal SecurityUser principal) throws IOException {

        UploadFileResponse response = uploadUserFilePort.uploadAsUser(
                categoryCode, file.getOriginalFilename(), file.getContentType(), file.getBytes(), toRequester(principal));

        return ResponseEntity.ok(ApiResponse.ok("File uploaded successfully", response));
    }

    @Operation(summary = "Delete a file — owner or ADMIN",
               description = "Organizer documents can only be deleted by an ADMIN. "
                       + "Errors: STORAGE_AUTHENTICATION_REQUIRED (401), STORAGE_ACCESS_DENIED (403), FILE_NOT_FOUND (404)")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer fileId,
                                                    @AuthenticationPrincipal SecurityUser principal) {
        deleteFilePort.delete(DeleteFileRequest.builder()
                .storedFileId(fileId)
                .requester(toRequester(principal))
                .build());
        return ResponseEntity.ok(ApiResponse.ok("File deleted successfully"));
    }

    @Operation(summary = "Get file info — public categories for any user, private files for owner or ADMIN",
               description = "Errors: STORAGE_AUTHENTICATION_REQUIRED (401), STORAGE_ACCESS_DENIED (403), FILE_NOT_FOUND (404)")
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<StoredFileResponse>> getFile(@PathVariable Integer fileId,
                                                                   @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("File retrieved", getFilePort.getById(fileId, toRequester(principal))));
    }

    @Operation(summary = "Get files by owner entity — only the files the caller can read")
    @GetMapping("/owner/{ownerEntityType}/{ownerEntityId}")
    public ResponseEntity<ApiResponse<List<StoredFileResponse>>> getByOwner(
            @PathVariable String ownerEntityType,
            @PathVariable Integer ownerEntityId,
            @AuthenticationPrincipal SecurityUser principal) {

        return ResponseEntity.ok(ApiResponse.ok("Files retrieved",
                getFilesByOwnerPort.getByOwner(ownerEntityId, ownerEntityType, toRequester(principal))));
    }

    @Operation(summary = "Generate presigned URL — public categories for any user, private files for owner or ADMIN",
               description = "Errors: STORAGE_AUTHENTICATION_REQUIRED (401), STORAGE_ACCESS_DENIED (403), "
                       + "FILE_NOT_FOUND (404), S3_PRESIGN_FAILED (400)")
    @GetMapping("/{fileId}/url")
    public ResponseEntity<ApiResponse<PublicUrlResponse>> generateUrl(@PathVariable Integer fileId,
                                                                      @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(ApiResponse.ok("URL generated",
                generatePublicUrlPort.generate(fileId, toRequester(principal))));
    }

    private static StorageRequester toRequester(SecurityUser principal) {
        return principal == null
                ? StorageRequester.anonymous()
                : StorageRequester.of(principal.getUserId(), principal.getRole());
    }
}
