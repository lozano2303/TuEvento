package com.capysoft.tuevento.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Catálogo y Comercio serán dos servicios: mientras sigan en el monolito solo se hablan por los
 * adaptadores de {@code infrastructure/client/}, que son lo único que cambia al separarlos.
 */
@DisplayName("Frontera entre Catálogo y Comercio")
class CatalogCommerceBoundaryTest {

    private static final Path MODULES = Path.of("src/main/java/com/capysoft/tuevento/modules");
    private static final List<String> CATALOG = List.of("event", "category", "section", "seat");
    private static final List<String> COMMERCE = List.of("ticket", "payment", "wallet");
    private static final String ADAPTERS = "/infrastructure/client/";

    /** Pendiente de Catálogo: cuenta las entradas vendidas con TicketRepository. Se borra al corregirlo. */
    private static final Set<String> KNOWN_CATALOG_VIOLATIONS = Set.of("event/application/usecase/GetEventService.java");

    @Test
    @DisplayName("Comercio solo usa clases de Catálogo desde sus adaptadores")
    void commerceReachesCatalogThroughAdapters() throws IOException {
        assertThat(referencesOutsideAdapters(COMMERCE, CATALOG)).isEmpty();
    }

    @Test
    @DisplayName("Catálogo solo usa clases de Comercio desde sus adaptadores")
    void catalogReachesCommerceThroughAdapters() throws IOException {
        assertThat(referencesOutsideAdapters(CATALOG, COMMERCE)).isSubsetOf(KNOWN_CATALOG_VIOLATIONS);
    }

    @Test
    @DisplayName("ningún archivo de Comercio nombra una entidad de Catálogo (p. ej. un JOIN en JPQL)")
    void commerceQueriesDoNotUseCatalogEntities() throws IOException {
        assertThat(entityMentions(COMMERCE, CATALOG)).isEmpty();
    }

    @Test
    @DisplayName("ningún archivo de Catálogo nombra una entidad de Comercio (p. ej. un JOIN en JPQL)")
    void catalogQueriesDoNotUseCommerceEntities() throws IOException {
        assertThat(entityMentions(CATALOG, COMMERCE)).isEmpty();
    }

    private static List<String> referencesOutsideAdapters(List<String> from, List<String> to) throws IOException {
        Pattern otherService = Pattern.compile(
                "com\\.capysoft\\.tuevento\\.modules\\.(" + String.join("|", to) + ")\\.");
        List<String> violations = new ArrayList<>();
        for (Path file : sources(from)) {
            String relative = MODULES.relativize(file).toString().replace('\\', '/');
            if (!relative.contains(ADAPTERS) && otherService.matcher(Files.readString(file)).find()) {
                violations.add(relative);
            }
        }
        return violations;
    }

    /** Los adaptadores tampoco: las consultas de un servicio no pueden leer las tablas del otro. */
    private static List<String> entityMentions(List<String> from, List<String> to) throws IOException {
        Set<String> entities = sources(to).stream()
                .map(file -> file.getFileName().toString().replace(".java", ""))
                .filter(name -> name.endsWith("Entity"))
                .collect(Collectors.toSet());
        // Sin entidades la regla pasaría siempre: la frontera estaría midiendo una carpeta equivocada
        assertThat(entities).isNotEmpty();
        Pattern anyEntity = Pattern.compile("\\b(" + String.join("|", entities) + ")\\b");
        List<String> violations = new ArrayList<>();
        for (Path file : sources(from)) {
            if (anyEntity.matcher(Files.readString(file)).find()) {
                violations.add(MODULES.relativize(file).toString().replace('\\', '/'));
            }
        }
        return violations;
    }

    private static List<Path> sources(List<String> modules) throws IOException {
        List<Path> files = new ArrayList<>();
        for (String module : modules) {
            try (Stream<Path> walk = Files.walk(MODULES.resolve(module))) {
                walk.filter(path -> path.toString().endsWith(".java")).forEach(files::add);
            }
        }
        return files;
    }
}
