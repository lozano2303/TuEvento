package com.capysoft.tuevento.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Commerce (orders, tickets, payments and wallet) lives in commerce-service. Catalog talks to it only
 * through the saga messages and {@code modules/catalogbridge}: its internal API client is used from
 * the {@code infrastructure/client/} adapters alone.
 */
@DisplayName("Commerce outside the monolith")
class CommerceBoundaryTest {

    private static final Path MODULES = Path.of("src/main/java/com/capysoft/tuevento/modules");
    private static final Path SOURCES = Path.of("src/main/java/com/capysoft/tuevento");
    private static final String CLIENT_IMPORT = "import com.capysoft.tuevento.modules.catalogbridge.client.";

    @Test
    @DisplayName("the monolith no longer has the ticket, payment or wallet modules")
    void commerceModulesAreGone() {
        assertThat(MODULES.resolve("ticket")).doesNotExist();
        assertThat(MODULES.resolve("payment")).doesNotExist();
        assertThat(MODULES.resolve("wallet")).doesNotExist();
    }

    @Test
    @DisplayName("no class or query of the monolith names a Commerce entity (its tables belong to commerce-service)")
    void noJpqlOverCommerceTables() throws IOException {
        Pattern commerceEntity = Pattern.compile("\\b(OrderEntity|TicketEntity|SeatTicketEntity|TicketCheckinEntity|"
                + "TicketLogEntity|PaymentEntity|PaymentLogEntity|RefundEntity|TransactionWebhookEntity|"
                + "WalletEntity|WalletTransactionEntity)\\b");
        assertThat(sourcesMatching(commerceEntity)).as("references to Commerce entities").isEmpty();
    }

    @Test
    @DisplayName("only the adapters (infrastructure/client/) use the commerce-service client")
    void onlyAdaptersUseTheClient() throws IOException {
        List<String> violations = sourcesMatching(Pattern.compile(Pattern.quote(CLIENT_IMPORT))).stream()
                .filter(rel -> !rel.startsWith("modules/catalogbridge/"))
                .filter(rel -> !rel.contains("/infrastructure/client/"))
                .toList();
        assertThat(violations).as("uses of the commerce client outside the adapters").isEmpty();
    }

    private static List<String> sourcesMatching(Pattern pattern) throws IOException {
        try (Stream<Path> files = Files.walk(SOURCES)) {
            return files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        try {
                            return pattern.matcher(Files.readString(p)).find();
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .map(p -> SOURCES.relativize(p).toString().replace('\\', '/'))
                    .toList();
        }
    }
}
