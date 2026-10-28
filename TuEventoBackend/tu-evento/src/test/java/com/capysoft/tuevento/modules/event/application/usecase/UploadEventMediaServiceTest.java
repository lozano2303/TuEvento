package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.UploadEventMediaRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventMediaResponse;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventMedia;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaLogRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.shared.application.storage.FileStorage;
import com.capysoft.tuevento.shared.application.storage.FileStorage.FileUpload;
import com.capysoft.tuevento.shared.application.storage.FileStorage.StoredFileRef;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.ImagePolicyViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("UploadEventMediaService")
class UploadEventMediaServiceTest {

    private final EventMediaRepository media = mock(EventMediaRepository.class);
    private final EventMediaLogRepository logs = mock(EventMediaLogRepository.class);
    private final EventRepository events = mock(EventRepository.class);
    private final FileStorage fileStorage = mock(FileStorage.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final UploadEventMediaService service =
            new UploadEventMediaService(media, logs, events, fileStorage, publisher);

    private static UploadEventMediaRequest request() {
        return UploadEventMediaRequest.builder().eventId(9L)
                .file(new MockMultipartFile("file", "cover.jpg", "image/jpeg", new byte[]{1, 2})).build();
    }

    private void event(EventStatus status) {
        when(events.findById(9L)).thenReturn(Optional.of(Event.builder().eventId(9L).status(status).build()));
    }

    @Test
    @DisplayName("sube la imagen al servicio de archivos como EVENT_MEDIA del evento y guarda su URL")
    void uploadsThroughFileStorage() {
        event(EventStatus.DRAFT);
        when(fileStorage.upload(any())).thenReturn(new StoredFileRef(5, "https://cdn/cover.jpg"));
        when(media.save(any())).thenAnswer(i -> {
            EventMedia saved = i.getArgument(0);
            return EventMedia.builder().mediaId(3L).eventId(saved.getEventId()).imgUrl(saved.getImgUrl()).build();
        });

        EventMediaResponse response = service.execute(request(), 42L);

        ArgumentCaptor<FileUpload> upload = ArgumentCaptor.forClass(FileUpload.class);
        verify(fileStorage).upload(upload.capture());
        assertThat(upload.getValue()).isEqualTo(new FileUpload("EVENT_MEDIA", 42, "cover.jpg", "image/jpeg",
                upload.getValue().content(), "EVENT", 9));
        assertThat(upload.getValue().content()).containsExactly(1, 2);
        assertThat(response.getImgUrl()).isEqualTo("https://cdn/cover.jpg");
        assertThat(response.getMediaId()).isEqualTo(3L);
        verify(publisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("fuera de DRAFT no sube nada")
    void onlyInDraft() {
        event(EventStatus.PUBLISHED);

        assertThatThrownBy(() -> service.execute(request(), 42L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("EVENT_UPDATE_NOT_ALLOWED");
        verifyNoInteractions(fileStorage);
    }

    @Test
    @DisplayName("una imagen rechazada por la moderación no crea el media del evento")
    void rejectedImage() {
        event(EventStatus.DRAFT);
        when(fileStorage.upload(any())).thenThrow(new ImagePolicyViolationException("NSFW"));

        assertThatThrownBy(() -> service.execute(request(), 42L)).isInstanceOf(ImagePolicyViolationException.class);
        verify(media, never()).save(any());
    }
}
