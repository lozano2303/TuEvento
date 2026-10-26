package com.capysoft.storage.application.port.in;

import com.capysoft.storage.application.dto.request.UploadFileRequest;
import com.capysoft.storage.application.dto.response.UploadFileResponse;

public interface UploadFilePort {

    UploadFileResponse upload(UploadFileRequest request);
}
