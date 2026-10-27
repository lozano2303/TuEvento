package com.capysoft.tuevento.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Frontera del servicio Identidad (security, profile, theme) en el monolito. Las reglas son las
 * de la guía de migración: entre servicios solo se habla por puertos, con el adaptador en
 * {@code infrastructure/client/}, o por eventos.
 *
 * <p>Las excepciones conocidas están listadas abajo con su motivo; quitar una cuando se resuelva.
 * Un import nuevo que cruce la frontera hace fallar este test.
 */
@DisplayName("Frontera del servicio Identidad")
class IdentityBoundaryTest {

    private static final Path MODULES = Path.of("src/main/java/com/capysoft/tuevento/modules");
    private static final Set<String> IDENTITY = Set.of("security", "profile", "theme");
    private static final Pattern IMPORT = Pattern.compile("^import com\\.capysoft\\.tuevento\\.modules\\.([a-z]+)\\.(\\S+);",
            Pattern.MULTILINE);

    /** Archivo (relativo a modules/) → motivo. Pendientes que dependen de otra fase. */
    private static final Map<String, String> OUTGOING_EXCEPTIONS = Map.of(
            "profile/infrastructure/translation/ProfileTranslatableContentHandler.java",
            "Fase 3 (Localización): las *_translation pasan al dominio dueño",
            "profile/application/usecase/GetProfileByUserIdUseCase.java",
            "Fase 3 (Localización): profile_translation pasa a Identidad",
            "theme/application/usecase/GetThemesUseCase.java",
            "Fase 3 (Localización): theme_translation pasa a Identidad",
            "security/infrastructure/websocket/AdminPendingCountsWebSocketListener.java",
            "Escucha EventStatusChangedEvent (contrato de evento); pasará a Kafka al separar Catálogo",
            "profile/interfaces/rest/ProfileTranslationController.java",
            "Localización (PR #148) lo movió a profile; usa los idiomas hasta que pase a identity-service");

    private static final Map<String, String> INCOMING_EXCEPTIONS = Map.of(
            "language/", "Fase 3 (Localización, de otro equipo): traducciones de perfil y tema",
            "notification/infrastructure/messaging/UserActivatedListener.java",
            "Escucha UserActivatedEvent (contrato de evento); pasará a Kafka",
            "notification/infrastructure/messaging/OrganizerRoleRevokedListener.java",
            "Escucha OrganizerRoleRevokedEvent (contrato de evento); pasará a Kafka");

    @Test
    @DisplayName("Identidad solo importa otros servicios desde infrastructure/client/")
    void identityReachesOthersOnlyThroughClients() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles()) {
            String relative = MODULES.relativize(file).toString().replace('\\', '/');
            String module = relative.substring(0, relative.indexOf('/'));
            if (!IDENTITY.contains(module) || relative.contains("/infrastructure/client/")
                    || OUTGOING_EXCEPTIONS.containsKey(relative)) {
                continue;
            }
            Matcher m = IMPORT.matcher(Files.readString(file));
            while (m.find()) {
                if (!IDENTITY.contains(m.group(1))) {
                    violations.add(relative + " → " + m.group(1) + "." + m.group(2));
                }
            }
        }
        assertThat(violations).as("imports de Identidad hacia otros servicios fuera de infrastructure/client/")
                .isEmpty();
    }

    @Test
    @DisplayName("los demás servicios usan Identidad por sus puertos o eventos, nunca sus repositorios ni entidades")
    void othersNeverReadIdentityInternals() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles()) {
            String relative = MODULES.relativize(file).toString().replace('\\', '/');
            String module = relative.substring(0, relative.indexOf('/'));
            if (IDENTITY.contains(module) || relative.contains("/infrastructure/client/")
                    || INCOMING_EXCEPTIONS.keySet().stream().anyMatch(relative::startsWith)) {
                continue;
            }
            Matcher m = IMPORT.matcher(Files.readString(file));
            while (m.find()) {
                if (IDENTITY.contains(m.group(1))) {
                    violations.add(relative + " → " + m.group(1) + "." + m.group(2));
                }
            }
        }
        assertThat(violations).as("imports hacia Identidad fuera de infrastructure/client/").isEmpty();
    }

    @Test
    @DisplayName("las excepciones listadas siguen existiendo (si ya se resolvió una, quitarla de la lista)")
    void exceptionsAreStillNeeded() {
        OUTGOING_EXCEPTIONS.keySet().forEach(f ->
                assertThat(MODULES.resolve(f)).as("excepción obsoleta: " + f).exists());
        INCOMING_EXCEPTIONS.keySet().stream().filter(f -> f.endsWith(".java")).forEach(f ->
                assertThat(MODULES.resolve(f)).as("excepción obsoleta: " + f).exists());
    }

    private static List<Path> javaFiles() throws IOException {
        try (Stream<Path> files = Files.walk(MODULES)) {
            return files.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }
}
