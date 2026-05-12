package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.UploadEventMediaRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventMediaResponse;
import com.capysoft.tuevento.modules.event.application.port.in.UploadEventMediaUseCase;
import com.capysoft.tuevento.modules.event.domain.event.EventMediaUploadedEvent;
import com.capysoft.tuevento.modules.event.domain.model.EventMedia;
import com.capysoft.tuevento.modules.event.domain.model.EventMediaLog;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaLogRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaRepository;
import com.capysoft.tuevento.modules.storage.application.dto.request.UploadFileRequest;
import com.capysoft.tuevento.modules.storage.application.dto.response.UploadFileResponse;
import com.capysoft.tuevento.modules.storage.application.port.in.UploadFilePort;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UploadEventMediaService implements UploadEventMediaUseCase {

    private static final String FILE_CATEGORY_CODE = "EVENT_MEDIA";

    private final EventMediaRepository mediaRepository;
    private final EventMediaLogRepository mediaLogRepository;
    private final UploadFilePort uploadFilePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public EventMediaResponse execute(UploadEventMediaRequest request, Long userId) {
        byte[] content;
        try {
            content = request.getFile().getBytes();
        } catch (IOException e) {
            throw new BusinessException("EVENT_MEDIA_READ_ERROR",
                    "Failed to read uploaded file: " + e.getMessage());
        }

        UploadFileResponse uploaded = uploadFilePort.upload(UploadFileRequest.builder()
                .fileCategoryCode(FILE_CATEGORY_CODE)
                .uploadedBy(userId.intValue())
                .originalFilename(request.getFile().getOriginalFilename())
                .contentType(request.getFile().getContentType())
                .content(content)
                .ownerEntityId(request.getEventId().intValue())
                .ownerEntityType("EVENT")
                .build());

        EventMedia media = mediaRepository.save(EventMedia.builder()
                .eventId(request.getEventId())
                .imgUrl(uploaded.getPublicUrl())
                .build());

        int nextVersion = mediaLogRepository.findNextVersionByEventId(request.getEventId());

        mediaLogRepository.save(EventMediaLog.builder()
                .eventId(request.getEventId())
                .mediaId(media.getMediaId())
                .imgUrl(media.getImgUrl())
                .version(nextVersion)
                .isVisible(true)
                .build());

        eventPublisher.publishEvent(EventMediaUploadedEvent.builder()
                .mediaId(media.getMediaId())
                .eventId(request.getEventId())
                .imgUrl(media.getImgUrl())
                .occurredAt(LocalDateTime.now())
                .build());

        return EventMediaResponse.builder()
                .mediaId(media.getMediaId())
                .eventId(media.getEventId())
                .imgUrl(media.getImgUrl())
                .build();
    }
}
