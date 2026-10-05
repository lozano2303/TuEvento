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
            default -> new MessageContent(
                    "Notificación",
                    email ? html("Tienes una nueva notificación en TuEvento.") : "Tienes una nueva notificación.");
        };
    }

    private static MessageContent paymentApproved(boolean email, SendNotificationCommand command) {
        String breakdown = paymentBreakdown(command);
        if (email) {
            return new MessageContent(
                    "TuEvento — Pago aprobado",
                    html("Tu pago fue aprobado.", breakdown));
        }
        return new MessageContent("Pago aprobado", "Tu pago fue aprobado. " + breakdown);
    }

    private static MessageContent paymentRefunded(boolean email, SendNotificationCommand command) {
        String breakdown = paymentBreakdown(command);
        if (email) {
            return new MessageContent(
                    "TuEvento — Reembolso procesado",
                    html("Tu reembolso fue procesado.", breakdown));
        }
        return new MessageContent("Reembolso procesado", "Tu reembolso fue procesado. " + breakdown);
    }

    private static MessageContent walletCredited(boolean email, SendNotificationCommand command) {
        String amount = money(command.getCreditedAmount(), command.getCurrency());
        String reason = (command.getReason() == null || command.getReason().isBlank())
                ? ""
                : " Motivo: " + command.getReason() + ".";
        String line = "Se acreditaron " + amount + " a tu cartera." + reason;
        if (email) {
            return new MessageContent("TuEvento — Crédito en tu cartera", html(line));
        }
        return new MessageContent("Crédito en tu cartera", line);
    }

    private static MessageContent welcome(boolean email, SendNotificationCommand command) {
        String userName = (command.getReason() != null && !command.getReason().isBlank()) 
                ? command.getReason() 
                : "Usuario";
        if (email) {
            return new MessageContent(
                    "Bienvenido a TuEvento",
                    html(
                        "¡Hola " + userName + "! Bienvenido a TuEvento, tu plataforma para descubrir y disfrutar eventos increíbles.",
                        "En TuEvento puedes:",
                        "• Explorar y comprar tickets para eventos de todo tipo",
                        "• Solicitar ser organizador presentando tu cédula/pasaporte y documentos adicionales",
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
                    html(
                        "¡Buenas noticias! El administrador aprobó tu evento <strong>" + name + "</strong>.",
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
                    html(
                        "Tu evento <strong>" + name + "</strong> no fue aprobado.",
                        "Motivo indicado por el administrador: " + reason,
                        "Puedes corregirlo y volver a enviarlo a revisión desde <em>Mis eventos</em>."
                    ));
        }
        // Plain-text body for IN_APP — strip any residual markdown/HTML from reason
        String plainReason = command.getReason() != null ? command.getReason() : "Sin motivo especificado.";
        return new MessageContent(
                "Evento rechazado",
                "Tu evento \"" + command.getEventName() + "\" fue rechazado. Motivo: " + plainReason
                + " Corrígelo desde Mis eventos.");
    }

    private static String paymentBreakdown(SendNotificationCommand command) {
        BigDecimal wallet = zeroIfNull(command.getWalletAmount());
        BigDecimal gateway = zeroIfNull(command.getGatewayAmount());
        String currency = command.getCurrency() == null ? "COP" : command.getCurrency();
        boolean hasWallet = wallet.compareTo(BigDecimal.ZERO) > 0;
        boolean hasGateway = gateway.compareTo(BigDecimal.ZERO) > 0;
        if (hasWallet && hasGateway) {
            return "Pagaste " + money(wallet, currency) + " con tu cartera y "
                    + money(gateway, currency) + " a través de la pasarela.";
        }
        if (hasWallet) {
            return "Pagaste " + money(wallet, currency) + " con tu cartera.";
        }
        if (hasGateway) {
            return "Pagaste " + money(gateway, currency) + " a través de la pasarela.";
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

    private static String html(String... paragraphs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        for (String p : paragraphs) {
            if (p != null && !p.isBlank()) {
                sb.append("<p>").append(p).append("</p>");
            }
        }
        sb.append("<p>TuEvento</p></body></html>");
        return sb.toString();
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
