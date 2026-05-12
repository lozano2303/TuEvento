package com.capysoft.tuevento.modules.event.application.port.in;

import com.capysoft.tuevento.modules.event.application.dto.request.AddCommentReplyRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventCommentReplyResponse;

public interface AddCommentReplyUseCase {

    EventCommentReplyResponse execute(Long ratingId, AddCommentReplyRequest request, Long userId);
}
