package com.capysoft.tuevento.modules.notification.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.capysoft.tuevento.modules.notification.application.dto.SendNotificationCommand;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationChannelNames;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationTypeNames;

/**
 * Fábrica simple de textos en español (sin motor de plantillas).
 */
public final class NotificationMessageFactory {

    private NotificationMessageFactory() {
    }

    public static MessageContent build(String typeName, String channelName, SendNotificationCommand command) {
        boolean email = NotificationChannelNames.EMAIL.equals(channelName);
        return switch (typeName) {
            case NotificationTypeNames.PAYMENT_APPROVED -> paymentApproved(email, command);
            case NotificationTypeNames.PAYMENT_REFUNDED -> paymentRefunded(email, command);
            case NotificationTypeNames.WALLET_CREDITED  -> walletCredited(email, command);
            case NotificationTypeNames.WELCOME          -> welcome(email, command);
            case NotificationTypeNames.EVENT_PUBLISHED  -> eventPublished(email, command);
            case NotificationTypeNames.EVENT_REJECTED   -> eventRejected(email, command);
            case NotificationTypeNames.NEW_COMMENT      -> newComment(email, command);
            case NotificationTypeNames.NEW_REPLY        -> newReply(email, command);
            default -> new MessageContent(
                    "Notificación",
                    email ? html("&#128276;", "Tienes una nueva notificación en TuEvento.") : "Tienes una nueva notificación.");
        };
    }

    private static MessageContent paymentApproved(boolean email, SendNotificationCommand command) {
        String breakdown = paymentBreakdown(command);
        if (email) {
            return new MessageContent(
                    "TuEvento — Pago aprobado",
                    html("&#9989;", "Tu pago fue aprobado.", breakdown));
        }
        return new MessageContent("Pago aprobado", "Tu pago fue aprobado. " + breakdown);
    }

    private static MessageContent paymentRefunded(boolean email, SendNotificationCommand command) {
        String breakdown = paymentBreakdown(command);
        if (email) {
            return new MessageContent(
                    "TuEvento — Reembolso procesado",
                    html("&#128260;", "Tu reembolso fue procesado.", breakdown));
        }
        return new MessageContent("Reembolso procesado", "Tu reembolso fue procesado. " + breakdown);
    }

    private static MessageContent walletCredited(boolean email, SendNotificationCommand command) {
        String amount = money(command.getCreditedAmount(), command.getCurrency());
        String reason = (command.getReason() == null || command.getReason().isBlank())
                ? ""
                : " Motivo: " + command.getReason() + ".";
        String line = "Se acreditaron <strong style=\"color:#c4b5fd;\">" + amount + "</strong> a tu cartera." + reason;
        if (email) {
            return new MessageContent("TuEvento — Crédito en tu cartera", html("&#128176;", line));
        }
        return new MessageContent("Crédito en tu cartera", "Se acreditaron " + amount + " a tu cartera." + reason);
    }

    private static MessageContent welcome(boolean email, SendNotificationCommand command) {
        String userName = (command.getReason() != null && !command.getReason().isBlank())
                ? command.getReason()
                : "Usuario";
        if (email) {
            return new MessageContent(
                    "Bienvenido a TuEvento",
                    html("&#127881;",
                        "¡Hola <strong style=\"color:#c4b5fd;\">" + userName + "</strong>! Bienvenido a TuEvento, tu plataforma para descubrir y disfrutar eventos increíbles.",
                        "En TuEvento puedes:",
                        "&#8226; Explorar y comprar tickets para eventos de todo tipo",
                        "&#8226; Solicitar ser organizador presentando tu cédula/pasaporte y documentos adicionales",
                        "Estamos felices de tenerte con nosotros. ¡Comienza a explorar ahora!"
                    ));
        }
        return new MessageContent(
                "¡Bienvenido a TuEvento!",
                "Explora eventos y compra tickets. Solicita ser organizador con tu documentación para crear tus propios eventos.");
    }

    // ── Event review notifications ────────────────────────────────────────────

    private static MessageContent eventPublished(boolean email, SendNotificationCommand command) {
        String name = escapeHtml(command.getEventName() != null ? command.getEventName() : "tu evento");
        if (email) {
            return new MessageContent(
                    "TuEvento — Tu evento fue publicado",
                    html("&#127881;",
                        "¡Buenas noticias! El administrador aprobó tu evento <strong style=\"color:#c4b5fd;\">" + name + "</strong>.",
                        "Ya está visible para todos los usuarios en la plataforma.",
                        "Puedes verlo y gestionarlo desde <em>Mis eventos</em>."
                    ));
        }
        return new MessageContent(
                "Evento publicado",
                "¡Tu evento \"" + command.getEventName() + "\" fue aprobado y ya está visible para todos!");
    }

    private static MessageContent eventRejected(boolean email, SendNotificationCommand command) {
        String name   = escapeHtml(command.getEventName() != null ? command.getEventName() : "tu evento");
        String reason = escapeHtml(command.getReason() != null && !command.getReason().isBlank()
                ? command.getReason()
                : "El administrador no dejó un motivo detallado.");
        if (email) {
            return new MessageContent(
                    "TuEvento — Tu evento fue rechazado",
                    html("&#10060;",
                        "Tu evento <strong style=\"color:#c4b5fd;\">" + name + "</strong> no fue aprobado.",
                        "Motivo indicado por el administrador: " + reason,
                        "Puedes corregirlo y volver a enviarlo a revisión desde <em>Mis eventos</em>."
                    ));
        }
        String plainReason = command.getReason() != null ? command.getReason() : "Sin motivo especificado.";
        return new MessageContent(
                "Evento rechazado",
                "Tu evento \"" + command.getEventName() + "\" fue rechazado. Motivo: " + plainReason
                + " Corrígelo desde Mis eventos.");
    }

    // ── Comment / reply notifications ─────────────────────────────────────────

    private static MessageContent newComment(boolean email, SendNotificationCommand command) {
        String commenter = escapeHtml(command.getCommenterName() != null ? command.getCommenterName() : "Alguien");
        String event     = escapeHtml(command.getEventName()    != null ? command.getEventName()    : "tu evento");
        String comment   = escapeHtml(command.getReason()       != null ? command.getReason()       : "");
        if (email) {
            return new MessageContent(
                    "TuEvento — " + commenter + " comentó en tu evento",
                    html("&#128172;",
                        "¡Ey! <strong style=\"color:#c4b5fd;\">" + commenter + "</strong> dejó un comentario en tu evento <strong style=\"color:#c4b5fd;\">" + event + "</strong>.",
                        comment.isBlank() ? null : "Esto fue lo que dijo: <em>\"" + comment + "\"</em>",
                        "Entra y respóndele, ¡los organizadores que interactúan con su comunidad son los mejores!"
                    ));
        }
        String subject = commenter + " comentó en \"" + command.getEventName() + "\"";
        String body = comment.isBlank()
                ? commenter + " dejó un comentario en tu evento. ¡Entra a verlo!"
                : "\"" + command.getReason() + "\"";
        return new MessageContent(subject, body);
    }

    private static MessageContent newReply(boolean email, SendNotificationCommand command) {
        String commenter = escapeHtml(command.getCommenterName() != null ? command.getCommenterName() : "Alguien");
        String event     = escapeHtml(command.getEventName()    != null ? command.getEventName()    : "un evento");
        String comment   = escapeHtml(command.getReason()       != null ? command.getReason()       : "");
        if (email) {
            return new MessageContent(
                    "TuEvento — " + commenter + " respondió tu comentario",
                    html("&#128172;",
                        "¡Oye, te respondieron! <strong style=\"color:#c4b5fd;\">" + commenter + "</strong> contestó tu comentario en el evento <strong style=\"color:#c4b5fd;\">" + event + "</strong>.",
                        comment.isBlank() ? null : "Lo que te dijo: <em>\"" + comment + "\"</em>",
                        "No lo dejes hablando solo — entra y continúa la conversación."
                    ));
        }
        String subject = commenter + " respondió tu comentario";
        String body = comment.isBlank()
                ? commenter + " respondió tu comentario en \"" + command.getEventName() + "\". ¡Entra a ver!"
                : commenter + " en \"" + command.getEventName() + "\": \"" + command.getReason() + "\"";
        return new MessageContent(subject, body);
    }

    private static String paymentBreakdown(SendNotificationCommand command) {
        BigDecimal wallet = zeroIfNull(command.getWalletAmount());
        BigDecimal gateway = zeroIfNull(command.getGatewayAmount());
        String currency = command.getCurrency() == null ? "COP" : command.getCurrency();
        boolean hasWallet = wallet.compareTo(BigDecimal.ZERO) > 0;
        boolean hasGateway = gateway.compareTo(BigDecimal.ZERO) > 0;
        if (hasWallet && hasGateway) {
            return "Pagaste " + money(wallet, currency) + " con tu cartera y "
                    + money(gateway, currency) + " con pasarela de pago.";
        }
        if (hasWallet) {
            return "Pagaste " + money(wallet, currency) + " con tu cartera.";
        }
        if (hasGateway) {
            return "Pagaste " + money(gateway, currency) + " con pasarela de pago.";
        }
        return "El movimiento se registró correctamente.";
    }

    private static String money(BigDecimal amount, String currency) {
        BigDecimal value = zeroIfNull(amount).setScale(2, RoundingMode.HALF_UP);
        String code = currency == null || currency.isBlank() ? "COP" : currency;
        return value.toPlainString() + " " + code;
    }

    private static BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /**
     * Genera un correo HTML con el mismo diseño visual que los templates
     * account-activation.html y password-reset.html: fondo oscuro (#1a1030),
     * cabecera con gradiente morado, contenido y pie de página.
     *
     * @param icon       Emoji o símbolo que aparece en el recuadro superior derecho del header.
     * @param paragraphs Líneas de texto/HTML que componen el cuerpo del mensaje.
     */
    private static String html(String icon, String... paragraphs) {
        int year = java.time.Year.now().getValue();
        StringBuilder body = new StringBuilder();
        for (String p : paragraphs) {
            if (p != null && !p.isBlank()) {
                body.append(
                    "<tr>" +
                    "<td style=\"font-size:15px; line-height:22px; color:#c4b5e0; " +
                    "font-family: Arial, Helvetica, sans-serif; padding-bottom:16px;\">" +
                    p +
                    "</td>" +
                    "</tr>\n");
            }
        }
        return
            "<!DOCTYPE html>\n" +
            "<html lang=\"es\" xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\">\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "<style>\n" +
            "  body,table,td{font-family:Arial,Helvetica,sans-serif;}\n" +
            "  body{margin:0;padding:0;background-color:#f2f0f7;}\n" +
            "  table{border-collapse:collapse;}\n" +
            "  a{text-decoration:none;}\n" +
            "  @media only screen and (max-width:600px){\n" +
            "    .ec{width:100%!important;}\n" +
            "    .fp{padding-left:20px!important;padding-right:20px!important;}\n" +
            "  }\n" +
            "</style>\n" +
            "</head>\n" +
            "<body style=\"margin:0;padding:0;background-color:#f2f0f7;\">\n" +
            "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f2f0f7;\">\n" +
            "  <tr><td align=\"center\" style=\"padding:32px 16px;\">\n" +
            "    <table role=\"presentation\" class=\"ec\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\n" +
            "      style=\"width:600px;max-width:600px;background-color:#1a1030;border-radius:20px;overflow:hidden;box-shadow:0 8px 30px rgba(76,29,149,0.15);\">\n" +
            // HEADER
            "      <tr>\n" +
            "        <td style=\"background-image:linear-gradient(135deg,#9333ea 0%,#6d28d9 100%);padding:44px 40px 40px 40px;\">\n" +
            "          <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
            "            <tr>\n" +
            "              <td valign=\"top\">\n" +
            "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
            "                  <tr><td style=\"font-size:24px;font-weight:bold;color:#ffffff;\">&#10022;&nbsp; Tu Evento</td></tr>\n" +
            "                  <tr><td style=\"padding-top:6px;font-size:13px;color:#e9d8fd;\">Todo tu evento, en un solo lugar</td></tr>\n" +
            "                  <tr><td style=\"padding-top:40px;font-size:12px;letter-spacing:3px;color:rgba(255,255,255,0.25);\">&#8226;&#8226;&#8226;<br>&#8226;&#8226;&#8226;</td></tr>\n" +
            "                </table>\n" +
            "              </td>\n" +
            "              <td align=\"right\" valign=\"middle\" width=\"130\">\n" +
            "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
            "                  <tr><td align=\"center\" style=\"width:110px;height:100px;background-color:rgba(255,255,255,0.12);border-radius:22px;text-align:center;vertical-align:middle;font-size:42px;line-height:1;\">" + icon + "</td></tr>\n" +
            "                </table>\n" +
            "              </td>\n" +
            "            </tr>\n" +
            "          </table>\n" +
            "        </td>\n" +
            "      </tr>\n" +
            // SEPARADOR DECORATIVO
            "      <tr>\n" +
            "        <td class=\"fp\" style=\"padding:4px 40px 0 40px;\">\n" +
            "          <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
            "            <tr>\n" +
            "              <td style=\"border-top:1px solid #3d2a5c;\" width=\"45%\">&nbsp;</td>\n" +
            "              <td align=\"center\" style=\"font-size:16px;color:#a78bfa;padding:12px 12px;\" width=\"10%\">&#10022;</td>\n" +
            "              <td style=\"border-top:1px solid #3d2a5c;\" width=\"45%\">&nbsp;</td>\n" +
            "            </tr>\n" +
            "          </table>\n" +
            "        </td>\n" +
            "      </tr>\n" +
            // CONTENIDO
            "      <tr>\n" +
            "        <td class=\"fp\" style=\"padding:24px 40px 32px 40px;\">\n" +
            "          <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
            body.toString() +
            "          </table>\n" +
            "        </td>\n" +
            "      </tr>\n" +
            // AVISO SEGURIDAD
            "      <tr>\n" +
            "        <td class=\"fp\" style=\"padding:0 40px 32px 40px;\">\n" +
            "          <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"\n" +
            "            style=\"background-color:#241640;border:1px solid #3d2a5c;border-radius:14px;\">\n" +
            "            <tr><td style=\"padding:16px 20px;font-family:Arial,Helvetica,sans-serif;font-size:13px;color:#9d8cb8;line-height:19px;\">\n" +
            "              Este es un mensaje automático. Si no reconoces esta actividad, puedes ignorarlo.\n" +
            "            </td></tr>\n" +
            "          </table>\n" +
            "        </td>\n" +
            "      </tr>\n" +
            // FOOTER
            "      <tr>\n" +
            "        <td style=\"border-top:1px solid #2d1f47;padding:24px 40px 32px 40px;\">\n" +
            "          <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
            "            <tr><td align=\"center\" style=\"font-size:14px;font-weight:bold;color:#c4b5fd;padding-bottom:8px;\">&#10022;&nbsp; Tu Evento</td></tr>\n" +
            "            <tr><td align=\"center\" style=\"font-size:12px;line-height:18px;color:#7a6a93;padding-bottom:4px;\">Este es un mensaje automático. Por favor, no respondas a este correo.</td></tr>\n" +
            "            <tr><td align=\"center\" style=\"font-size:12px;color:#5f5279;\">&copy; " + year + " Tu Evento &mdash; Todos los derechos reservados</td></tr>\n" +
            "          </table>\n" +
            "        </td>\n" +
            "      </tr>\n" +
            "    </table>\n" +
            "  </td></tr>\n" +
            "</table>\n" +
            "</body>\n" +
            "</html>";
    }

    /**
     * Escapes the five XML/HTML special characters so that user-supplied text
     * (e.g. an admin-written rejection reason) cannot inject markup into the
     * HTML email body.
     */
    static String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }

    public record MessageContent(String subject, String body) {
    }
}
