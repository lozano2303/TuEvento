package com.capysoft.tuevento.modules.event.application.service;

import com.capysoft.tuevento.modules.event.application.port.out.UserDirectoryPort.UserRef;
import com.capysoft.tuevento.shared.application.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Foto de perfil de los autores de los comentarios, leída del servicio de archivos. Una petición
 * por foto distinta (no por comentario) y nunca bloquea: si la foto no existe o el servicio no
 * responde, el comentario se muestra sin ella (con la inicial del autor).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentAuthorAvatars {

    private final FileStorage fileStorage;

    /** URL de la foto de cada autor, por userId; los autores sin foto no aparecen. */
    public Map<Integer, String> byUser(Collection<UserRef> authors) {
        Map<Integer, String> urlByFile = new HashMap<>();
        authors.stream()
                .map(UserRef::avatarFileId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(fileId -> resolve(fileId).ifPresent(url -> urlByFile.put(fileId, url)));

        Map<Integer, String> urlByUser = new HashMap<>();
        for (UserRef author : authors) {
            String url = author.avatarFileId() == null ? null : urlByFile.get(author.avatarFileId());
            if (url != null) {
                urlByUser.put(author.userId(), url);
            }
        }
        return urlByUser;
    }

    /** URL de la foto de un autor, o null si no tiene o no se pudo leer. */
    public String of(UserRef author) {
        return author == null || author.avatarFileId() == null ? null : resolve(author.avatarFileId()).orElse(null);
    }

    private Optional<String> resolve(Integer fileId) {
        try {
            return fileStorage.findPublicUrl(fileId);
        } catch (Exception e) {
            log.warn("Comment author avatar {} could not be resolved: {}", fileId, e.getMessage());
            return Optional.empty();
        }
    }
}
