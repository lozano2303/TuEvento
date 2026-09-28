package com.capysoft.tuevento.modules.notification.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InAppNotificationPageResponse {
    private List<InAppNotificationResponse> content;
    private long totalElements;
    private int page;
    private int size;
}
