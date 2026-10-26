package com.capysoft.storage.application.port.in;

import com.capysoft.storage.application.dto.request.DeleteFileRequest;

public interface DeleteFilePort {

    void delete(DeleteFileRequest request);
}
