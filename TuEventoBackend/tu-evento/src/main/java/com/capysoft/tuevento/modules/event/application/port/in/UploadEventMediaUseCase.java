package com.capysoft.tuevento.modules.event.application.port.in;

import com.capysoft.tuevento.modules.event.application.dto.request.UploadEventMediaRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventMediaResponse;

public interface UploadEventMediaUseCase {

    EventMediaResponse execute(UploadEventMediaRequest request, Long userId);
}
