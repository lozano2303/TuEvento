package com.capysoft.tuevento.modules.security.interfaces.rest;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.capysoft.tuevento.modules.security.application.port.out.OrganizerDocumentPort;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

/**
 * API interna para identity-service: sube el documento de una solicitud de organizador a Archivos
 * y genera su URL para el admin, por el mismo puerto que usaba la solicitud. No la usan web ni
 * móvil y el gateway no la enruta; exige la clave entre servicios ({@code InternalApiKeyFilter}).
 */
@Hidden
@RestController
@RequestMapping("/internal/organizer-documents")
@RequiredArgsConstructor
public class InternalOrganizerDocumentController {

    private final OrganizerDocumentPort organizerDocumentPort;

    public record UploadedDocument(Integer storedFileId) {
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadedDocument>> upload(@RequestParam String fileCategoryCode,
                                                                @RequestParam String ownerEntityType,
                                                                @RequestParam Integer uploadedBy,
                                                                @RequestParam("file") MultipartFile file) throws IOException {
        Integer storedFileId = organizerDocumentPort.upload(new OrganizerDocumentPort.DocumentUpload(fileCategoryCode,
                ownerEntityType, uploadedBy, file.getOriginalFilename(), file.getContentType(), file.getBytes()));
        return ResponseEntity.ok(ApiResponse.ok("Document uploaded", new UploadedDocument(storedFileId)));
    }

    @GetMapping("/{storedFileId}/url")
    public ResponseEntity<ApiResponse<OrganizerDocumentPort.DocumentUrl>> publicUrl(@PathVariable Integer storedFileId,
                                                                                  @RequestParam Integer requesterUserId,
                                                                                  @RequestParam String requesterRole) {
        return ResponseEntity.ok(ApiResponse.ok("Document URL generated",
                organizerDocumentPort.publicUrl(storedFileId, requesterUserId, requesterRole)));
    }
}
