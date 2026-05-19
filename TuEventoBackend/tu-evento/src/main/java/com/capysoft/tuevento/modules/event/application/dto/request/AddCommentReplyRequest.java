package com.capysoft.tuevento.modules.event.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCommentReplyRequest {

    @NotBlank
    private String replyText;

    @Positive
    private Long parentReplyId;
}
