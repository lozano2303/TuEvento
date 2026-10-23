package com.capysoft.tuevento.modules.notification.domain.model;

public final class NotificationTypeNames {

    public static final String PAYMENT_APPROVED = "PAYMENT_APPROVED";
    public static final String PAYMENT_REFUNDED = "PAYMENT_REFUNDED";
    public static final String WALLET_CREDITED  = "WALLET_CREDITED";
    public static final String WELCOME          = "WELCOME";
    public static final String EVENT_PUBLISHED  = "EVENT_PUBLISHED";
    public static final String EVENT_REJECTED   = "EVENT_REJECTED";
    /** Sent to the event organizer when any user posts a top-level comment on their event. */
    public static final String NEW_COMMENT      = "NEW_COMMENT";
    /** Sent to a user when someone replies to their comment or mentions them in a thread. */
    public static final String NEW_REPLY        = "NEW_REPLY";

    private NotificationTypeNames() {
    }
}
