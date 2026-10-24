package com.capysoft.tuevento.modules.notification.application;

import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.application.service.NotificationMessageFactory;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationChannelNames;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationTypeNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NotificationMessageFactory} — event review types only.
 *
 * <h3>Coverage</h3>
 * <ul>
 *   <li>EVENT_PUBLISHED — IN_APP body and subject</li>
 *   <li>EVENT_PUBLISHED — EMAIL body contains event name</li>
 *   <li>EVENT_REJECTED  — IN_APP body contains reason</li>
 *   <li>EVENT_REJECTED  — EMAIL body escapes HTML characters in reason</li>
 * </ul>
 */
@DisplayName("NotificationMessageFactory — event review types")
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
    }
}
