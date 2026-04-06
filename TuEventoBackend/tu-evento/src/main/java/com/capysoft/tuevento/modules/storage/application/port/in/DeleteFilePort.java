package com.capysoft.tuevento.modules.storage.application.port.in;

import com.capysoft.tuevento.modules.storage.application.dto.request.DeleteFileRequest;

public interface DeleteFilePort {

    void delete(DeleteFileRequest request);
}
