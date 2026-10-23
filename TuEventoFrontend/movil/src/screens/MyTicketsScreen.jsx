import { useState, useCallback } from "react";
import {
  View, Text, ScrollView, TouchableOpacity, ActivityIndicator,
  StatusBar, RefreshControl,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useFocusEffect } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTranslation } from "react-i18next";
import { useTheme } from "../context/ThemeContext";
import { getMyOrders } from "../services/ticketService";

// ─── Helpers ──────────────────────────────────────────────────────────────────

const fmtCOP = (n, currency = "COP") =>
  new Intl.NumberFormat("es-CO", { style: "currency", currency, maximumFractionDigits: 0 })
    .format(Number(n) || 0);

const fmtDate = (iso) =>
  !iso ? "" : new Date(iso).toLocaleDateString("es-CO", { day: "2-digit", month: "short", year: "numeric" });

// Color por estado de ticket (el label viene de i18n).
const TICKET_STATUS_COLOR = {
  PAID: "#34d399", USED: "#64748b", REFUNDED: "#f59e0b",
  CANCELLED: "#f87171", PENDING: "#94a3b8", PROCESSING: "#94a3b8",
};

// Color/ícono por estado de evento (el mensaje viene de i18n).
const EVENT_STATE_STYLE = {
  CANCELLED:      { color: "#f87171", icon: "close-circle-outline" },
  COMPLETED:      { color: "#64748b", icon: "flag-outline" },
  REJECTED:       { color: "#f87171", icon: "ban-outline" },
  DRAFT:          { color: "#94a3b8", icon: "time-outline" },
  PENDING_REVIEW: { color: "#94a3b8", icon: "time-outline" },
};

export default function MyTicketsScreen() {
  const { colors } = useTheme();
  const { t } = useTranslation();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();

  const [orders, setOrders]       = useState([]);
  const [loading, setLoading]     = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError]         = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setOrders(await getMyOrders("PAID"));
    } catch (e) {
      setError(e.message || t("mobileTickets.loadError"));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  // Recarga cada vez que la pantalla recibe foco, para reflejar cambios de
  // estado (p. ej. un ticket que pasó a USED tras ser escaneado).
  useFocusEffect(useCallback(() => { load(); }, [load]));
  const onRefresh = () => { setRefreshing(true); load(); };

  const renderTicket = (order, ticket) => {
    const stColor = TICKET_STATUS_COLOR[ticket.status] ?? TICKET_STATUS_COLOR.PENDING;
    const stLabel = t(`mobileTickets.status.${ticket.status}`, { defaultValue: ticket.status });
    const st = { color: stColor, label: stLabel };
    const usable = ticket.status === "PAID";
    return (
      <View
        key={ticket.ticketId}
        style={{
          flexDirection: "row", alignItems: "center", gap: 12,
          paddingVertical: 12, paddingHorizontal: 14,
          borderTopWidth: 1, borderTopColor: colors.primary + "15",
        }}
      >
        <View style={{ width: 38, height: 38, borderRadius: 11, backgroundColor: colors.accent + "22", alignItems: "center", justifyContent: "center" }}>
          <Ionicons name="ticket-outline" size={19} color={colors.accent} />
        </View>
        <View style={{ flex: 1 }}>
          <Text style={{ color: colors.textPrimary, fontSize: 13, fontWeight: "700" }}>{ticket.code}</Text>
          <Text style={{ color: colors.textMuted, fontSize: 11, marginTop: 2 }}>
            {fmtCOP(ticket.totalPrice, ticket.currency)}
          </Text>
        </View>

        <View style={{ backgroundColor: st.color + "22", borderRadius: 8, paddingHorizontal: 8, paddingVertical: 3 }}>
          <Text style={{ color: st.color, fontSize: 10, fontWeight: "700" }}>{st.label}</Text>
        </View>

        <TouchableOpacity
          disabled={!usable}
          onPress={() => navigation.navigate("TicketQr", {
            mode: "ticket",
            id: ticket.ticketId,
            title: ticket.code,
            subtitle: fmtCOP(ticket.totalPrice, ticket.currency),
          })}
          activeOpacity={0.8}
          style={{
            flexDirection: "row", alignItems: "center", gap: 4,
            backgroundColor: usable ? colors.primary : colors.primary + "25",
            borderRadius: 10, paddingHorizontal: 10, paddingVertical: 8,
            opacity: usable ? 1 : 0.5,
          }}
        >
          <Ionicons name="qr-code-outline" size={14} color={colors.textPrimary} />
          <Text style={{ color: colors.textPrimary, fontSize: 12, fontWeight: "700" }}>QR</Text>
        </TouchableOpacity>
      </View>
    );
  };

  const renderOrder = (order) => {
    const tickets = order.tickets ?? [];
    const remaining = tickets.filter((t) => t.status === "PAID").length;

    // El evento debe estar publicado para poder usar las entradas / generar QR.
    const eventActive = order.eventStatus === "PUBLISHED";
    const eventState = EVENT_STATE_STYLE[order.eventStatus] ?? null;
    const eventStateMsg = t(`mobileTickets.eventState.${order.eventStatus}`, {
      defaultValue: t("mobileTickets.eventState.unavailable"),
    });
    const canOrderQr = eventActive && remaining > 0;

    return (
      <View
        key={order.orderId}
        style={{
          backgroundColor: colors.surface + "CC", borderRadius: 16,
          borderWidth: 1, borderColor: colors.primary + "20", overflow: "hidden",
        }}
      >
        {/* Cabecera de la orden */}
        <View style={{ padding: 14, flexDirection: "row", alignItems: "center", gap: 12 }}>
          <View style={{ flex: 1 }}>
            <Text style={{ color: colors.textMuted, fontSize: 10, fontWeight: "700", textTransform: "uppercase", letterSpacing: 0.6 }}>
              {t("mobileTickets.order")} #{order.orderId}
            </Text>
            {/* Nombre del evento */}
            <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "800", marginTop: 2 }} numberOfLines={1}>
              {order.eventName || t("mobileTickets.event")}
            </Text>
            <Text style={{ color: colors.textSecondary, fontSize: 12, marginTop: 1 }}>
              {fmtCOP(order.totalAmount, order.currency)}
            </Text>
            <Text style={{ color: colors.textMuted, fontSize: 11, marginTop: 2 }}>
              {fmtDate(order.createdAt)} · {t("mobileTickets.ticketsCount", { count: tickets.length })}
            </Text>
          </View>

          {/* QR de la orden completa — solo si el evento está activo */}
          {eventActive && (
            <TouchableOpacity
              disabled={!canOrderQr}
              onPress={() => navigation.navigate("TicketQr", {
                mode: "order",
                id: order.orderId,
                title: order.eventName || `${t("mobileTickets.order")} #${order.orderId}`,
                subtitle: t("mobileTickets.toValidate", { count: remaining }),
              })}
              activeOpacity={0.85}
              style={{
                flexDirection: "row", alignItems: "center", gap: 6,
                backgroundColor: canOrderQr ? colors.primary : colors.primary + "25",
                borderRadius: 12, paddingHorizontal: 12, paddingVertical: 10,
                opacity: canOrderQr ? 1 : 0.5,
              }}
            >
              <Ionicons name="qr-code" size={16} color={colors.textPrimary} />
              <Text style={{ color: colors.textPrimary, fontSize: 12, fontWeight: "700" }}>{t("mobileTickets.qrOrder")}</Text>
            </TouchableOpacity>
          )}
        </View>

        {eventActive ? (
          /* Tickets individuales */
          tickets.map((t) => renderTicket(order, t))
        ) : (
          /* Evento no activo: ocultar tickets y opciones de QR, explicar por qué */
          <View style={{
            flexDirection: "row", alignItems: "center", gap: 10,
            paddingHorizontal: 14, paddingVertical: 14,
            borderTopWidth: 1, borderTopColor: colors.primary + "15",
            backgroundColor: (eventState?.color ?? colors.textMuted) + "12",
          }}>
            <Ionicons name={eventState?.icon ?? "information-circle-outline"} size={18} color={eventState?.color ?? colors.textMuted} />
            <Text style={{ color: colors.textSecondary, fontSize: 12, flex: 1, lineHeight: 17 }}>
              {eventStateMsg}
            </Text>
          </View>
        )}
      </View>
    );
  };

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <StatusBar barStyle="light-content" backgroundColor={colors.surface} />

      {/* Header */}
      <View style={{
        flexDirection: "row", alignItems: "center", gap: 12,
        paddingTop: insets.top + 16, paddingBottom: 16, paddingHorizontal: 20,
        backgroundColor: colors.surface, borderBottomWidth: 1, borderBottomColor: colors.primary + "30",
      }}>
        <TouchableOpacity
          onPress={() => navigation.goBack()} activeOpacity={0.75}
          style={{ width: 38, height: 38, borderRadius: 12, backgroundColor: colors.primary + "28", borderWidth: 1, borderColor: colors.primary + "40", alignItems: "center", justifyContent: "center" }}
        >
          <Ionicons name="arrow-back" size={20} color={colors.accent} />
        </TouchableOpacity>
        <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "800", flex: 1 }}>{t("mobileTickets.title")}</Text>
      </View>

      {loading ? (
        <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={orders.length === 0 ? { flexGrow: 1, justifyContent: "center" } : { padding: 20, paddingBottom: insets.bottom + 32, gap: 16 }}
          showsVerticalScrollIndicator={false}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}
        >
          {error && (
            <View style={{ flexDirection: "row", alignItems: "center", gap: 8, backgroundColor: colors.error + "15", borderRadius: 12, padding: 14, borderWidth: 1, borderColor: colors.error + "40", margin: 20 }}>
              <Ionicons name="alert-circle-outline" size={18} color={colors.error} />
              <Text style={{ color: colors.error, fontSize: 13, flex: 1 }}>{error}</Text>
            </View>
          )}

          {!error && orders.length === 0 ? (
            <View style={{ alignItems: "center", gap: 12, paddingHorizontal: 40 }}>
              <Ionicons name="ticket-outline" size={64} color={colors.textMuted} />
              <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "700" }}>{t("mobileTickets.emptyTitle")}</Text>
              <Text style={{ color: colors.textSecondary, fontSize: 13, textAlign: "center", lineHeight: 20 }}>
                {t("mobileTickets.emptyDesc")}
              </Text>
            </View>
          ) : (
            orders.map(renderOrder)
          )}
        </ScrollView>
      )}
    </View>
  );
}
