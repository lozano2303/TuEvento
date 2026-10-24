package com.capysoft.tuevento.modules.notification.application;

import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.service.NotificationMessageFactory;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationChannelNames;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationTypeNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NotificationMessageFactory}.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>EVENT_PUBLISHED — IN_APP body and subject</li>
 *   <li>EVENT_PUBLISHED — EMAIL body contains event name</li>
 *   <li>EVENT_REJECTED  — IN_APP body contains reason</li>
 *   <li>EVENT_REJECTED  — EMAIL body escapes HTML characters in reason</li>
 *   <li>ORGANIZER_ROLE_REVOKED — IN_APP and EMAIL</li>
 *   <li>PAYMENT_APPROVED / PAYMENT_REFUNDED / WALLET_CREDITED — EMAIL breakdown</li>
 *   <li>WELCOME — EMAIL greeting with and without name</li>
 *   <li>NEW_COMMENT / NEW_REPLY — EMAIL escaping and IN_APP texts</li>
 *   <li>Unknown type — IN_APP and EMAIL default</li>
 *   <li>EMAIL escapes user input: welcome name, wallet reason, currency in amounts</li>
 * </ul>
 */
@DisplayName("NotificationMessageFactory")
class NotificationMessageFactoryTest {

    private static final String EVENT_NAME = "Rock Festival 2027";

    private SendNotificationCommand publishedCmd() {
        return SendNotificationCommand.builder()
                .typeName(NotificationTypeNames.EVENT_PUBLISHED)
                .entityId(10L)
                .entityType("EVENT")
                .eventName(EVENT_NAME)
                .build();
    }

    private SendNotificationCommand rejectedCmd(String reason) {
        return SendNotificationCommand.builder()
                .typeName(NotificationTypeNames.EVENT_REJECTED)
                .entityId(10L)
                .entityType("EVENT")
                .eventName(EVENT_NAME)
                .reason(reason)
                .build();
    }

    @Nested
    @DisplayName("EVENT_PUBLISHED")
    class EventPublished {

        @Test
        @DisplayName("IN_APP: subject mentions 'publicado' and body contains event name")
        void inApp_subject_and_body() {
            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.EVENT_PUBLISHED, NotificationChannelNames.IN_APP, publishedCmd());

            assertThat(result.subject()).containsIgnoringCase("publicado");
            assertThat(result.body()).contains(EVENT_NAME);
        }

        @Test
        @DisplayName("EMAIL: subject contains TuEvento prefix and HTML body includes event name")
        void email_subject_and_htmlBody() {
            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.EVENT_PUBLISHED, NotificationChannelNames.EMAIL, publishedCmd());

            assertThat(result.subject()).startsWith("TuEvento");
            assertThat(result.body()).contains("<html");
            assertThat(result.body()).contains(EVENT_NAME);
        }
    }

    @Nested
    @DisplayName("EVENT_REJECTED")
    class EventRejected {

        @Test
        @DisplayName("IN_APP: body contains plain rejection reason")
        void inApp_contains_reason() {
            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.EVENT_REJECTED, NotificationChannelNames.IN_APP,
                    rejectedCmd("Missing venue details"));

            assertThat(result.subject()).containsIgnoringCase("rechazado");
            assertThat(result.body()).contains("Missing venue details");
        }

        @Test
        @DisplayName("EMAIL: HTML body escapes angle brackets in admin-written reason")
        void email_escapes_html_in_reason() {
            String maliciousReason = "Use <script>alert('xss')</script> for details";

            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.EVENT_REJECTED, NotificationChannelNames.EMAIL,
                    rejectedCmd(maliciousReason));

            assertThat(result.body()).contains("&lt;script&gt;");
            assertThat(result.body()).doesNotContain("<script>");
        }
    }

    @Nested
    @DisplayName("ORGANIZER_ROLE_REVOKED")
    class OrganizerRoleRevoked {

        private SendNotificationCommand revokedCmd() {
            return SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.ORGANIZER_ROLE_REVOKED)
                    .entityId(42L)
                    .entityType("USER")
                    .build();
        }

        @Test
        @DisplayName("IN_APP: subject mentions 'revocado' and body mentions 'Organizador'")
        void inApp_subject_and_body() {
            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.ORGANIZER_ROLE_REVOKED, NotificationChannelNames.IN_APP, revokedCmd());

            assertThat(result.subject()).containsIgnoringCase("revocado");
            assertThat(result.body()).containsIgnoringCase("organizador");
        }

        @Test
        @DisplayName("EMAIL: subject starts with TuEvento and HTML body contains 'Organizador'")
        void email_subject_and_htmlBody() {
            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.ORGANIZER_ROLE_REVOKED, NotificationChannelNames.EMAIL, revokedCmd());

            assertThat(result.subject()).startsWith("TuEvento");
            assertThat(result.body()).contains("<html");
            assertThat(result.body()).containsIgnoringCase("Organizador");
        }
    }

    @Nested
    @DisplayName("Unknown type → default message")
    class UnknownType {

        @Test
        @DisplayName("unknown type returns generic notification subject and body")
        void unknownType_returnsDefault() {
            var cmd = SendNotificationCommand.builder()
                    .typeName("UNKNOWN_TYPE")
                    .entityId(1L)
                    .build();

            var result = NotificationMessageFactory.build("UNKNOWN_TYPE", NotificationChannelNames.IN_APP, cmd);

            assertThat(result.subject()).isEqualTo("Notificación");
            assertThat(result.body()).containsIgnoringCase("notificación");
        }

        @Test
        @DisplayName("unknown type on EMAIL returns generic HTML body")
        void unknownType_email_returnsDefaultHtml() {
            var cmd = SendNotificationCommand.builder()
                    .typeName("UNKNOWN_TYPE")
                    .entityId(1L)
                    .build();

            var result = NotificationMessageFactory.build("UNKNOWN_TYPE", NotificationChannelNames.EMAIL, cmd);

            assertThat(result.subject()).isEqualTo("Notificación");
            assertThat(result.body()).contains("<html").contains("Tienes una nueva notificación en TuEvento.");
        }
    }

    @Nested
    @DisplayName("Payment and wallet types — EMAIL")
    class PaymentAndWalletEmail {

        @Test
        @DisplayName("PAYMENT_APPROVED: HTML body includes the wallet + gateway breakdown")
        void paymentApproved_email_includesBreakdown() {
            var cmd = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.PAYMENT_APPROVED)
                    .walletAmount(new BigDecimal("10000"))
                    .gatewayAmount(new BigDecimal("5000"))
                    .currency("COP")
                    .build();

            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.PAYMENT_APPROVED, NotificationChannelNames.EMAIL, cmd);

            assertThat(result.subject()).isEqualTo("TuEvento — Pago aprobado");
            assertThat(result.body())
                    .contains("<html")
                    .contains("Tu pago fue aprobado.")
                    .contains("10000.00 COP con tu cartera y 5000.00 COP con pasarela de pago.");
        }

        @Test
        @DisplayName("PAYMENT_REFUNDED: HTML body includes the refunded amount")
        void paymentRefunded_email_includesBreakdown() {
            var cmd = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.PAYMENT_REFUNDED)
                    .walletAmount(new BigDecimal("2500"))
                    .build();

            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.PAYMENT_REFUNDED, NotificationChannelNames.EMAIL, cmd);

            assertThat(result.subject()).isEqualTo("TuEvento — Reembolso procesado");
            assertThat(result.body())
                    .contains("Tu reembolso fue procesado.")
                    .contains("Pagaste 2500.00 COP con tu cartera.");
        }

        @Test
        @DisplayName("WALLET_CREDITED: HTML body highlights the amount and includes the reason")
        void walletCredited_email_highlightsAmountAndReason() {
            var cmd = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.WALLET_CREDITED)
                    .creditedAmount(new BigDecimal("3000"))
                    .currency("COP")
                    .reason("Compensación por evento cancelado")
                    .build();

            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.WALLET_CREDITED, NotificationChannelNames.EMAIL, cmd);

            assertThat(result.subject()).isEqualTo("TuEvento — Crédito en tu cartera");
            assertThat(result.body())
                    .contains("<strong style=\"color:#c4b5fd;\">3000.00 COP</strong>")
                    .contains("Motivo: Compensación por evento cancelado.");
        }

        @Test
        @DisplayName("WALLET_CREDITED: IN_APP body is plain text without HTML markup")
        void walletCredited_inApp_isPlainText() {
            var cmd = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.WALLET_CREDITED)
                    .creditedAmount(new BigDecimal("3000"))
                    .build();

            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.WALLET_CREDITED, NotificationChannelNames.IN_APP, cmd);

            assertThat(result.subject()).isEqualTo("Crédito en tu cartera");
            assertThat(result.body())
                    .isEqualTo("Se acreditaron 3000.00 COP a tu cartera.")
                    .doesNotContain("<strong");
        }
    }

    @Nested
    @DisplayName("WELCOME — EMAIL")
    class WelcomeEmail {

        @Test
        @DisplayName("greets the user by name when provided")
        void welcome_email_greetsByName() {
            var cmd = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.WELCOME)
                    .reason("Laura")
                    .build();

            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.WELCOME, NotificationChannelNames.EMAIL, cmd);

            assertThat(result.subject()).isEqualTo("Bienvenido a TuEvento");
            assertThat(result.body()).contains("¡Hola <strong style=\"color:#c4b5fd;\">Laura</strong>!");
        }

        @Test
        @DisplayName("falls back to 'Usuario' when no name is provided")
        void welcome_email_fallsBackToUsuario() {
            var cmd = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.WELCOME)
                    .build();

            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.WELCOME, NotificationChannelNames.EMAIL, cmd);

            assertThat(result.body()).contains(">Usuario</strong>");
        }
    }

    @Nested
    @DisplayName("NEW_COMMENT / NEW_REPLY")
    class CommentsAndReplies {

        private SendNotificationCommand commentCmd(String type, String comment) {
            return SendNotificationCommand.builder()
                    .typeName(type)
                    .eventName(EVENT_NAME)
                    .commenterName("Carlos")
                    .reason(comment)
                    .build();
        }

        @Test
        @DisplayName("NEW_COMMENT EMAIL: includes commenter, event and escaped comment")
        void newComment_email_includesEscapedComment() {
            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.NEW_COMMENT, NotificationChannelNames.EMAIL,
                    commentCmd(NotificationTypeNames.NEW_COMMENT, "<b>Great</b> event"));

            assertThat(result.subject()).isEqualTo("TuEvento — Carlos comentó en tu evento");
            assertThat(result.body())
                    .contains(EVENT_NAME)
                    .contains("&lt;b&gt;Great&lt;/b&gt; event")
                    .doesNotContain("<b>Great</b>");
        }

        @Test
        @DisplayName("NEW_COMMENT IN_APP: subject has no emoji and body quotes the comment")
        void newComment_inApp_subjectAndBody() {
            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.NEW_COMMENT, NotificationChannelNames.IN_APP,
                    commentCmd(NotificationTypeNames.NEW_COMMENT, "Nice!"));

            assertThat(result.subject()).isEqualTo("Carlos comentó en \"" + EVENT_NAME + "\"");
            assertThat(result.body()).isEqualTo("\"Nice!\"");
        }

        @Test
        @DisplayName("NEW_REPLY EMAIL: includes commenter, event and escaped reply")
        void newReply_email_includesEscapedReply() {
            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.NEW_REPLY, NotificationChannelNames.EMAIL,
                    commentCmd(NotificationTypeNames.NEW_REPLY, "See you <there>"));

            assertThat(result.subject()).isEqualTo("TuEvento — Carlos respondió tu comentario");
            assertThat(result.body())
                    .contains(EVENT_NAME)
                    .contains("See you &lt;there&gt;");
        }

        @Test
        @DisplayName("NEW_REPLY IN_APP: subject has no emoji and body includes event and reply")
        void newReply_inApp_subjectAndBody() {
            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.NEW_REPLY, NotificationChannelNames.IN_APP,
                    commentCmd(NotificationTypeNames.NEW_REPLY, "Thanks"));

            assertThat(result.subject()).isEqualTo("Carlos respondió tu comentario");
            assertThat(result.body()).isEqualTo("Carlos en \"" + EVENT_NAME + "\": \"Thanks\"");
        }
    }

    @Nested
    @DisplayName("EMAIL escapes user-supplied input")
    class EmailEscaping {

        private static final String RAW     = "<b>x</b>";
        private static final String ESCAPED = "&lt;b&gt;x&lt;/b&gt;";

        @Test
        @DisplayName("WELCOME: user name is escaped")
        void welcome_email_escapesUserName() {
            var cmd = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.WELCOME)
                    .reason(RAW)
                    .build();

            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.WELCOME, NotificationChannelNames.EMAIL, cmd);

            assertThat(result.body()).contains(ESCAPED).doesNotContain(RAW);
        }

        @Test
        @DisplayName("WALLET_CREDITED: reason is escaped")
        void walletCredited_email_escapesReason() {
            var cmd = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.WALLET_CREDITED)
                    .creditedAmount(new BigDecimal("3000"))
                    .reason(RAW)
                    .build();

            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.WALLET_CREDITED, NotificationChannelNames.EMAIL, cmd);

            assertThat(result.body()).contains("Motivo: " + ESCAPED + ".").doesNotContain(RAW);
        }

        @Test
        @DisplayName("WALLET_CREDITED: currency in the amount is escaped")
        void walletCredited_email_escapesCurrency() {
            var cmd = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.WALLET_CREDITED)
                    .creditedAmount(new BigDecimal("3000"))
                    .currency(RAW)
                    .build();

            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.WALLET_CREDITED, NotificationChannelNames.EMAIL, cmd);

            assertThat(result.body()).contains("3000.00 " + ESCAPED).doesNotContain(RAW);
        }

        @Test
        @DisplayName("PAYMENT_APPROVED: currency in the breakdown is escaped")
        void paymentApproved_email_escapesCurrency() {
            var cmd = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.PAYMENT_APPROVED)
                    .gatewayAmount(new BigDecimal("5000"))
                    .currency(RAW)
                    .build();

            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.PAYMENT_APPROVED, NotificationChannelNames.EMAIL, cmd);

            assertThat(result.body()).contains("5000.00 " + ESCAPED).doesNotContain(RAW);
        }

        @Test
        @DisplayName("PAYMENT_REFUNDED: currency in the breakdown is escaped")
        void paymentRefunded_email_escapesCurrency() {
            var cmd = SendNotificationCommand.builder()
                    .typeName(NotificationTypeNames.PAYMENT_REFUNDED)
                    .walletAmount(new BigDecimal("2500"))
                    .currency(RAW)
                    .build();

            var result = NotificationMessageFactory.build(
                    NotificationTypeNames.PAYMENT_REFUNDED, NotificationChannelNames.EMAIL, cmd);

            assertThat(result.body()).contains("2500.00 " + ESCAPED).doesNotContain(RAW);
        }
    }
}
