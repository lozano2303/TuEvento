package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.UploadEventMediaRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventMediaResponse;
import com.capysoft.tuevento.modules.event.application.port.in.UploadEventMediaUseCase;
import com.capysoft.tuevento.modules.event.domain.event.EventMediaUploadedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventMedia;
import com.capysoft.tuevento.modules.event.domain.model.EventMediaLog;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaLogRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.shared.application.storage.FileStorage;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
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

    private final EventMediaRepository    mediaRepository;
    private final EventMediaLogRepository mediaLogRepository;
    private final EventRepository         eventRepository;
    private final FileStorage             fileStorage;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public EventMediaResponse execute(UploadEventMediaRequest request, Long userId) {
        // Verify event exists and is in DRAFT before uploading any file.
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found with id: " + request.getEventId()));

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessException("EVENT_UPDATE_NOT_ALLOWED",
                    "Media can only be uploaded while the event is in DRAFT status");
        }

        byte[] content;
        try {
            content = request.getFile().getBytes();
        } catch (IOException e) {
            throw new BusinessException("EVENT_MEDIA_READ_ERROR",
                    "Failed to read uploaded file: " + e.getMessage());
        }

        FileStorage.StoredFileRef uploaded = fileStorage.upload(new FileStorage.FileUpload(
                FILE_CATEGORY_CODE,
                userId.intValue(),
                request.getFile().getOriginalFilename(),
                request.getFile().getContentType(),
                content,
                "EVENT",
                request.getEventId().intValue()));

        EventMedia media = mediaRepository.save(EventMedia.builder()
                .eventId(request.getEventId())
                .imgUrl(uploaded.publicUrl())
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
