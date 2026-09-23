import { useState, useEffect, useRef, useCallback } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  StatusBar,
  StyleSheet,
  ScrollView,
} from "react-native";
import * as WebBrowser from "expo-web-browser";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useRoute } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import { getPayment } from "../services/paymentService";
import { getOrderTickets } from "../services/orderService";

const GATEWAY_BASE_URL = process.env.EXPO_PUBLIC_GATEWAY_URL || "http://localhost:4001";
const POLL_INTERVAL_MS = 3000;

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatCurrency(amount) {
  if (amount == null) return "$0";
  return `$${Number(amount).toLocaleString("es-CO")}`;
}

// ─── Componente principal ─────────────────────────────────────────────────────

export default function PaymentPendingScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const route = useRoute();

  const { paymentId, orderId, eventId, cartItems = [], eventTitle = "Evento" } =
    route.params ?? {};

  const [status, setStatus] = useState("PENDING");
  const [gatewayTxId, setGatewayTxId] = useState(null);
  const [tickets, setTickets] = useState([]);
  const [browserOpen, setBrowserOpen] = useState(false);
  // 'rejected' | 'failed' | 'cancelled' | null
  const [failReason, setFailReason] = useState(null);

  const intervalRef = useRef(null);
  const gatewayOpenedRef = useRef(false);
  // Guardamos el último status conocido antes de que el browser se cierre
  const lastKnownStatusRef = useRef("PENDING");

  // StyleSheet dentro del componente para acceder a colors
  const styles = StyleSheet.create({
    root: {
      flex: 1,
      backgroundColor: colors.background,
    },
    centered: {
      flex: 1,
      alignItems: "center",
      justifyContent: "center",
      paddingHorizontal: 28,
      paddingBottom: insets.bottom + 32,
    },
    iconWrap: {
      width: 88,
      height: 88,
      borderRadius: 44,
      alignItems: "center",
      justifyContent: "center",
      marginBottom: 24,
    },
    title: {
      fontSize: 20,
      fontWeight: "800",
      textAlign: "center",
      marginBottom: 8,
      color: colors.textPrimary,
    },
    subtitle: {
      fontSize: 14,
      textAlign: "center",
      lineHeight: 22,
      color: colors.textSecondary,
    },
    infoCard: {
      width: "100%",
      backgroundColor: colors.primary + "0D",
      borderRadius: 12,
      borderWidth: 1,
      borderColor: colors.primary + "20",
      padding: 14,
      marginTop: 20,
      gap: 8,
    },
    infoRow: {
      flexDirection: "row",
      justifyContent: "space-between",
    },
    infoLabel: {
      color: colors.textMuted,
      fontSize: 12,
    },
    infoValue: {
      color: colors.textSecondary,
      fontSize: 12,
      fontWeight: "600",
    },
    reopenBtn: {
      marginTop: 20,
    },
    reopenBtnText: {
      color: colors.accent,
      fontSize: 13,
      textDecorationLine: "underline",
      textAlign: "center",
    },
    hint: {
      color: colors.textMuted,
      fontSize: 11,
      textAlign: "center",
      marginTop: 16,
    },
    // ── Tickets ──
    ticketsList: {
      width: "100%",
      marginTop: 16,
      gap: 8,
    },
    ticketItem: {
      flexDirection: "row",
      alignItems: "center",
      gap: 10,
      padding: 12,
      borderRadius: 12,
      backgroundColor: colors.success + "14",
      borderWidth: 1,
      borderColor: colors.success + "30",
    },
    ticketCode: {
      color: colors.textPrimary,
      fontSize: 13,
      fontWeight: "700",
    },
    ticketPrice: {
      color: colors.textMuted,
      fontSize: 11,
      marginTop: 2,
    },
    ticketQr: {
      marginLeft: "auto",
      backgroundColor: colors.success + "20",
      borderRadius: 6,
      paddingHorizontal: 8,
      paddingVertical: 3,
    },
    ticketQrText: {
      color: colors.success,
      fontSize: 10,
      fontFamily: "monospace",
    },
    // ── Botones ──
    primaryBtn: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      gap: 8,
      borderRadius: 14,
      paddingVertical: 14,
      paddingHorizontal: 24,
      marginTop: 24,
      width: "100%",
    },
    primaryBtnText: {
      color: "#FFFFFF",
      fontSize: 15,
      fontWeight: "700",
    },
    secondaryBtn: {
      marginTop: 12,
      paddingVertical: 10,
    },
    secondaryBtnText: {
      color: colors.textSecondary,
      fontSize: 14,
      textAlign: "center",
    },
  });

  // ── Parar polling ─────────────────────────────────────────────────────────
  const stopPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  // ── Cargar tickets cuando el pago se aprueba ──────────────────────────────
  const loadTickets = useCallback(async () => {
    if (!orderId) return;
    try {
      const data = await getOrderTickets(orderId);
      setTickets(Array.isArray(data) ? data : []);
    } catch {
      // no bloquear la pantalla de éxito
    }
  }, [orderId]);

  // ── Helpers para aplicar un estado terminal ──────────────────────────────
  const applyTerminalStatus = useCallback((s, reason = null) => {
    setStatus(s);
    if (reason) setFailReason(reason);
    lastKnownStatusRef.current = s;
  }, []);

  // ── Abrir pasarela en in-app browser ─────────────────────────────────────
  const openGateway = useCallback(async (txId) => {
    if (gatewayOpenedRef.current) return;
    gatewayOpenedRef.current = true;
    setBrowserOpen(true);

    const url = `${GATEWAY_BASE_URL}/pay.html?id=${txId}`;
    try {
      await WebBrowser.openBrowserAsync(url, {
        presentationStyle: WebBrowser.WebBrowserPresentationStyle.FORM_SHEET,
        toolbarColor: colors.surface,
        controlsColor: colors.primary,
      });
    } finally {
      setBrowserOpen(false);
      // Poll inmediato al cerrar el browser
      try {
        const payment = await getPayment(paymentId);
        const s = payment.status;
        if (s === "APPROVED") {
          stopPolling();
          applyTerminalStatus("APPROVED");
          loadTickets();
        } else if (s === "REJECTED") {
          stopPolling();
          applyTerminalStatus("REJECTED", "rejected");
        } else if (s === "ERROR") {
          stopPolling();
          // Si el browser se cerró sin aprobar/rechazar → el usuario canceló
          const reason = lastKnownStatusRef.current === "PENDING" ? "cancelled" : "failed";
          applyTerminalStatus("ERROR", reason);
        }
      } catch {
        // silencioso — el polling regular lo reintentará
      }
    }
  }, [paymentId, colors, stopPolling, loadTickets, applyTerminalStatus]);

  // ── Polling principal ─────────────────────────────────────────────────────
  useEffect(() => {
    if (!paymentId) {
      navigation.goBack();
      return;
    }

    const poll = async () => {
      try {
        const payment = await getPayment(paymentId);

        // Abrir gateway automáticamente cuando tengamos gatewayTransactionId
        if (payment.gatewayTransactionId && !gatewayOpenedRef.current) {
          setGatewayTxId(payment.gatewayTransactionId);
          openGateway(payment.gatewayTransactionId);
        }

        const s = payment.status;
        setStatus(s);
        lastKnownStatusRef.current = s;

        if (s === "APPROVED") {
          stopPolling();
          loadTickets();
        } else if (s === "REJECTED") {
          stopPolling();
          applyTerminalStatus("REJECTED", "rejected");
        } else if (s === "ERROR") {
          stopPolling();
          // Si llegó ERROR por polling sin que el browser lo haya manejado
          // asumir fallo técnico (no cancelación por usuario)
          if (!failReason) applyTerminalStatus("ERROR", "failed");
        }
      } catch (err) {
        console.warn("[PaymentPending] poll error:", err.message);
      }
    };

    poll();
    intervalRef.current = setInterval(poll, POLL_INTERVAL_MS);
    return () => stopPolling();
  }, [paymentId]);

  // ── Render por estado ─────────────────────────────────────────────────────

  if (status === "PENDING") {
    return (
      <View style={styles.root}>
        <StatusBar barStyle="light-content" backgroundColor={colors.background} />
        <View style={styles.centered}>
          <View style={[styles.iconWrap, { backgroundColor: colors.primary + "20" }]}>
            <ActivityIndicator size="large" color={colors.primary} />
          </View>

          <Text style={styles.title}>Procesando tu pago</Text>
          <Text style={styles.subtitle}>
            {browserOpen
              ? "Completá el pago en la ventana que se abrió."
              : "Abriendo la ventana de pago…"}
          </Text>

          <View style={styles.infoCard}>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Evento</Text>
              <Text style={styles.infoValue}>{eventTitle}</Text>
            </View>
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Sillas</Text>
              <Text style={styles.infoValue}>{cartItems.length}</Text>
            </View>
          </View>

          {/* Re-abrir si el browser fue cerrado manualmente */}
          {!browserOpen && gatewayOpenedRef.current && gatewayTxId && (
            <TouchableOpacity
              onPress={() => { gatewayOpenedRef.current = false; openGateway(gatewayTxId); }}
              activeOpacity={0.75}
              style={styles.reopenBtn}
            >
              <Text style={styles.reopenBtnText}>Volver a abrir la ventana de pago</Text>
            </TouchableOpacity>
          )}

          <Text style={styles.hint}>
            No cierres la app mientras se procesa el pago.
          </Text>
        </View>
      </View>
    );
  }

  if (status === "APPROVED") {
    return (
      <View style={styles.root}>
        <StatusBar barStyle="light-content" backgroundColor={colors.background} />
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{
            flexGrow: 1,
            alignItems: "center",
            paddingTop: insets.top + 40,
            paddingBottom: insets.bottom + 40,
            paddingHorizontal: 28,
          }}
        >
          <View style={[styles.iconWrap, { backgroundColor: colors.success + "22" }]}>
            <Ionicons name="checkmark-circle" size={52} color={colors.success} />
          </View>

          <Text style={styles.title}>¡Pago aprobado!</Text>
          <Text style={styles.subtitle}>
            Tu compra fue exitosa. Acá están tus tickets.
          </Text>

          {tickets.length > 0 ? (
            <View style={styles.ticketsList}>
              {tickets.map((ticket) => (
                <View key={ticket.ticketId} style={styles.ticketItem}>
                  <Ionicons name="ticket-outline" size={18} color={colors.success} />
                  <View style={{ flex: 1 }}>
                    <Text style={styles.ticketCode}>
                      {ticket.code ?? `Ticket #${ticket.ticketId}`}
                    </Text>
                    <Text style={styles.ticketPrice}>
                      {formatCurrency(ticket.totalPrice)} {ticket.currency ?? ""}
                    </Text>
                  </View>
                  {ticket.qrCode && (
                    <View style={styles.ticketQr}>
                      <Text style={styles.ticketQrText}>{ticket.qrCode}</Text>
                    </View>
                  )}
                </View>
              ))}
            </View>
          ) : (
            <View style={[styles.infoCard, { borderColor: colors.success + "40" }]}>
              <Text style={[styles.subtitle, { fontSize: 13 }]}>
                Tus tickets serán enviados a tu correo en breve.
              </Text>
            </View>
          )}

          <TouchableOpacity
            onPress={() => navigation.navigate("Main")}
            activeOpacity={0.75}
            style={[styles.primaryBtn, { backgroundColor: colors.success }]}
          >
            <Ionicons name="home-outline" size={18} color="#FFFFFF" />
            <Text style={styles.primaryBtnText}>Ver más eventos</Text>
          </TouchableOpacity>
        </ScrollView>
      </View>
    );
  }

  // REJECTED / ERROR — tres variantes según failReason
  const isRejected  = status === "REJECTED";
  const isCancelled = status === "ERROR" && failReason === "cancelled";
  const isFailed    = status === "ERROR" && failReason !== "cancelled";

  const terminalIcon  = isRejected ? "close-circle"       : isCancelled ? "ban-outline"      : "alert-circle";
  const terminalColor = isRejected ? colors.error          : isCancelled ? "#F59E0B"           : colors.error;
  const terminalBg    = isRejected ? colors.errorBg        : isCancelled ? "#F59E0B22"         : colors.errorBg;
  const terminalTitle = isRejected ? "Pago rechazado"      : isCancelled ? "Pago cancelado"    : "Error en el pago";
  const terminalText  = isRejected
    ? "El pago fue rechazado. Podés volver a intentarlo."
    : isCancelled
    ? "Cancelaste la transacción. Podés volver a intentarlo cuando quieras."
    : "Ocurrió un error al procesar el pago. Podés intentarlo nuevamente.";

  return (
    <View style={styles.root}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />
      <View style={styles.centered}>
        <View style={[styles.iconWrap, { backgroundColor: terminalBg }]}>
          <Ionicons name={terminalIcon} size={52} color={terminalColor} />
        </View>

        <Text style={styles.title}>{terminalTitle}</Text>
        <Text style={styles.subtitle}>{terminalText}</Text>

        <TouchableOpacity
          onPress={() => navigation.navigate("EventDetail", { eventId })}
          activeOpacity={0.75}
          style={[styles.primaryBtn, { backgroundColor: colors.primary }]}
        >
          <Ionicons name="refresh-outline" size={18} color="#FFFFFF" />
          <Text style={styles.primaryBtnText}>Reintentar</Text>
        </TouchableOpacity>

        <TouchableOpacity
          onPress={() => navigation.navigate("Main")}
          activeOpacity={0.75}
          style={styles.secondaryBtn}
        >
          <Text style={styles.secondaryBtnText}>Volver a eventos</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}
