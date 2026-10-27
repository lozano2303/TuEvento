package com.capysoft.tuevento.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Identidad (usuarios, sesiones, perfiles y temas) vive en identity-service. En el monolito solo
 * queda {@code modules/identitybridge}: lo que el monolito sigue haciendo para Identidad (motor de
 * traducciones, Archivos, estadísticas del panel, /ws) y el cliente de su API interna.
 */
@DisplayName("Identidad fuera del monolito")
class IdentityBoundaryTest {

    private static final Path MODULES = Path.of("src/main/java/com/capysoft/tuevento/modules");
    private static final Path SOURCES = Path.of("src/main/java/com/capysoft/tuevento");
    private static final String BRIDGE_IMPORT = "import com.capysoft.tuevento.modules.identitybridge.";

    /** Quién más puede usar el puente: además de los adaptadores de infrastructure/client/. */
    private static final Set<String> ALSO_ALLOWED = Set.of(
            "modules/language/infrastructure/config/TranslationConfig.java",
            "shared/infrastructure/config/ProfileDataInitializer.java");

    @Test
    @DisplayName("el monolito ya no tiene los módulos security, profile ni theme")
    void identityModulesAreGone() {
        assertThat(MODULES.resolve("security")).doesNotExist();
        assertThat(MODULES.resolve("profile")).doesNotExist();
        assertThat(MODULES.resolve("theme")).doesNotExist();
    }

    @Test
    @DisplayName("ninguna consulta ni clase del monolito usa entidades de Identidad (sus tablas son de identity-service)")
    void noJpqlOverIdentityTables() throws IOException {
        Pattern identityEntity = Pattern.compile("\\b(UserEntity|LoginCredentialsEntity|ProfileEntity|RoleEntity|"
                + "UserStatusEntity|AuthSessionEntity|OrganizerPetitionEntity|ThemeEntity|UserThemeEntity)\\b");
        List<String> violations;
        try (Stream<Path> files = Files.walk(SOURCES)) {
            violations = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        try {
                            return identityEntity.matcher(Files.readString(p)).find();
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .map(p -> SOURCES.relativize(p).toString())
                    .toList();
        }
        assertThat(violations).as("referencias a entidades de Identidad").isEmpty();
    }

    @Test
    @DisplayName("solo los adaptadores (infrastructure/client/) usan el puente hacia Identidad")
    void onlyAdaptersUseTheBridge() throws IOException {
        List<String> violations;
        try (Stream<Path> files = Files.walk(SOURCES)) {
            violations = files.filter(p -> p.toString().endsWith(".java"))
                    .map(p -> SOURCES.relativize(p).toString().replace('\\', '/'))
                    .filter(rel -> !rel.startsWith("modules/identitybridge/"))
                    .filter(rel -> !rel.contains("/infrastructure/client/") && !ALSO_ALLOWED.contains(rel))
                    .filter(rel -> {
                        try {
                            return Files.readString(SOURCES.resolve(rel)).contains(BRIDGE_IMPORT);
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .toList();
        }
        assertThat(violations).as("usos del puente fuera de los adaptadores").isEmpty();
    }
}
