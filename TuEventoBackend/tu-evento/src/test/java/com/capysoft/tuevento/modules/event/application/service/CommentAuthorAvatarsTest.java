package com.capysoft.tuevento.modules.event.application.service;

import com.capysoft.tuevento.modules.event.application.port.out.UserDirectoryPort.UserRef;
import com.capysoft.tuevento.shared.application.storage.FileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("CommentAuthorAvatars")
class CommentAuthorAvatarsTest {

    private final FileStorage fileStorage = mock(FileStorage.class);
    private final CommentAuthorAvatars avatars = new CommentAuthorAvatars(fileStorage);

    @Test
    @DisplayName("una petición por foto distinta aunque varios autores la compartan")
    void resolvesEachPhotoOnce() {
        when(fileStorage.findPublicUrl(1)).thenReturn(Optional.of("https://cdn/default.jpg"));
        when(fileStorage.findPublicUrl(9)).thenReturn(Optional.of("https://cdn/ana.jpg"));

        Map<Integer, String> urls = avatars.byUser(List.of(
                new UserRef(7, "Ana", null, 9),
                new UserRef(8, "Luis", null, 1),
                new UserRef(10, "Eva", null, 1)));

        assertThat(urls).containsExactlyInAnyOrderEntriesOf(Map.of(
                7, "https://cdn/ana.jpg", 8, "https://cdn/default.jpg", 10, "https://cdn/default.jpg"));
        verify(fileStorage, times(1)).findPublicUrl(1);
    }

    @Test
    @DisplayName("los autores sin foto, o con una foto que ya no existe, quedan sin URL")
    void authorsWithoutPhoto() {
        when(fileStorage.findPublicUrl(5)).thenReturn(Optional.empty());

        Map<Integer, String> urls = avatars.byUser(List.of(new UserRef(7, "Ana", null, null),
                new UserRef(8, "Luis", null, 5)));

        assertThat(urls).isEmpty();
    }

    @Test
    @DisplayName("si el servicio de archivos falla, el comentario sale sin foto")
    void storageFailureIsSoft() {
        when(fileStorage.findPublicUrl(9)).thenThrow(new IllegalStateException("storage down"));

        assertThat(avatars.byUser(List.of(new UserRef(7, "Ana", null, 9)))).isEmpty();
        assertThat(avatars.of(new UserRef(7, "Ana", null, 9))).isNull();
    }

    @Test
    @DisplayName("la foto de un autor, o null si no hay autor o no tiene foto")
    void singleAuthor() {
        when(fileStorage.findPublicUrl(9)).thenReturn(Optional.of("https://cdn/ana.jpg"));

        assertThat(avatars.of(new UserRef(7, "Ana", null, 9))).isEqualTo("https://cdn/ana.jpg");
        assertThat(avatars.of(null)).isNull();
        assertThat(avatars.of(new UserRef(8, "Luis", null, null))).isNull();
    }

    @Test
    @DisplayName("sin autores no consulta nada")
    void noAuthors() {
        assertThat(avatars.byUser(List.of())).isEmpty();
        verifyNoInteractions(fileStorage);
    }
}
