package com.capysoft.tuevento.modules.storage.application.port.in;

import com.capysoft.tuevento.modules.storage.application.dto.request.StorageRequester;
import com.capysoft.tuevento.modules.storage.application.dto.response.UploadFileResponse;

/**
 * Upload initiated directly by an end user through the REST API.
 * Unlike {@link UploadFilePort} (trusted, used by other modules), it enforces the access policy
 * and never trusts client-supplied ownership data.
 */
public interface UploadUserFilePort {

    UploadFileResponse uploadAsUser(String fileCategoryCode, String originalFilename, String contentType,
                                    byte[] content, StorageRequester requester);
}
