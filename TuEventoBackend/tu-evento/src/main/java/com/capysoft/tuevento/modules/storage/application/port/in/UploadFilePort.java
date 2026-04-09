package com.capysoft.tuevento.modules.storage.application.port.in;

import com.capysoft.tuevento.modules.storage.application.dto.request.UploadFileRequest;
import com.capysoft.tuevento.modules.storage.application.dto.response.UploadFileResponse;

public interface UploadFilePort {

    UploadFileResponse upload(UploadFileRequest request);
}
