import { useState, useEffect } from "react";
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  StatusBar,
  StyleSheet,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useRoute } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import { createOrder } from "../services/orderService";
import { createPayment } from "../services/paymentService";
import { getMyWallet } from "../services/walletService";

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatCurrency(amount) {
  if (amount == null) return "$0";
  return `$${Number(amount).toLocaleString("es-CO")}`;
}

// ─── Componente principal ─────────────────────────────────────────────────────

export default function CheckoutScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const route = useRoute();

  const { eventId, seatIds = [], cartItems = [], eventTitle = "Evento" } =
    route.params ?? {};

  const [order, setOrder]               = useState(null);
  const [loadingOrder, setLoadingOrder] = useState(true);
  const [paying, setPaying]             = useState(false);
  const [error, setError]               = useState(null);
  const [walletBalance, setWalletBalance] = useState(null); // null = sin wallet / no cargado
  const [useWallet, setUseWallet]       = useState(false);

  const styles = StyleSheet.create({
    root: { flex: 1, backgroundColor: colors.background },
    header: {
      flexDirection: "row",
      alignItems: "center",
      paddingTop: insets.top + 16,
      paddingBottom: 16,
      paddingHorizontal: 20,
      backgroundColor: colors.surface,
      borderBottomWidth: 1,
      borderBottomColor: colors.primary + "30",
      gap: 12,
    },
    backButton: {
      width: 38, height: 38, borderRadius: 12,
      backgroundColor: colors.primary + "28",
      borderWidth: 1, borderColor: colors.primary + "40",
      alignItems: "center", justifyContent: "center",
    },
    headerTitle: {
      color: colors.textPrimary, fontSize: 18, fontWeight: "800", flex: 1,
    },
    scroll: { flex: 1 },
    scrollContent: {
      paddingHorizontal: 20, paddingTop: 20,
      paddingBottom: 16, gap: 16,
    },
    eventSubtitle: {
      color: colors.textSecondary, fontSize: 13, marginBottom: 4,
    },
    card: {
      backgroundColor: colors.surface + "CC",
      borderRadius: 14, borderWidth: 1,
      borderColor: colors.primary + "30", padding: 16,
    },
    cardHeader: {
      flexDirection: "row", alignItems: "center",
      gap: 8, marginBottom: 14,
    },
    cardTitle: {
      color: colors.textPrimary, fontSize: 14,
      fontWeight: "600", flex: 1,
    },
    badge: {
      backgroundColor: colors.primary + "28",
      borderRadius: 20, paddingHorizontal: 10, paddingVertical: 3,
    },
    badgeText: { color: colors.accent, fontSize: 11, fontWeight: "700" },
    seatRow: {
      flexDirection: "row", alignItems: "center",
      justifyContent: "space-between", paddingVertical: 6,
    },
    seatLeft: { flexDirection: "row", alignItems: "center", gap: 8 },
    seatCode: { color: colors.textPrimary, fontSize: 14, fontWeight: "600" },
    seatSection: { color: colors.textMuted, fontSize: 12 },
    seatPrice: { color: colors.accent, fontSize: 14, fontWeight: "700" },
    divider: {
      height: 1, backgroundColor: colors.primary + "20", marginVertical: 12,
    },
    totalRow: {
      flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    },
    totalLabel: {
      color: colors.textSecondary, fontSize: 14, fontWeight: "600",
    },
    totalAmount: {
      color: colors.primary, fontSize: 20, fontWeight: "800",
    },
    // ── Wallet card ──
    walletBalanceBadge: {
      backgroundColor: colors.primary + "22",
      borderRadius: 20, paddingHorizontal: 10, paddingVertical: 3,
    },
    walletBalanceText: { color: colors.accent, fontSize: 11, fontWeight: "700" },
    toggleRow: { flexDirection: "row", gap: 10, marginTop: 8 },
    toggleBtn: {
      flex: 1, borderRadius: 12, paddingVertical: 10,
      alignItems: "center", justifyContent: "center",
      borderWidth: 1,
    },
    toggleBtnText: { fontSize: 13, fontWeight: "700" },
    breakdownRow: {
      flexDirection: "row", justifyContent: "space-between",
      paddingVertical: 5,
    },
    breakdownLabel: { color: colors.textSecondary, fontSize: 13 },
    breakdownValue: { fontSize: 13, fontWeight: "600" },
    breakdownDivider: {
      height: 1, backgroundColor: colors.primary + "20", marginVertical: 8,
    },
    breakdownTotalLabel: {
      color: colors.textPrimary, fontSize: 14, fontWeight: "700",
    },
    breakdownTotalValue: {
      color: colors.primary, fontSize: 14, fontWeight: "800",
    },
    // ── Método de pago ──
    paymentMethod: {
      flexDirection: "row", alignItems: "center", gap: 12,
      backgroundColor: colors.primary + "10",
      borderRadius: 12, borderWidth: 1,
      borderColor: colors.primary + "30", padding: 12,
    },
    paymentMethodIcon: {
      width: 40, height: 40, borderRadius: 10,
      backgroundColor: colors.primary + "20",
      alignItems: "center", justifyContent: "center",
    },
    paymentMethodName: {
      color: colors.textPrimary, fontSize: 14, fontWeight: "600",
    },
    paymentMethodSub: {
      color: colors.textMuted, fontSize: 12, marginTop: 2,
    },
    radioOuter: {
      width: 20, height: 20, borderRadius: 10, borderWidth: 2,
      borderColor: colors.primary, alignItems: "center",
      justifyContent: "center", marginLeft: "auto",
    },
    radioInner: {
      width: 10, height: 10, borderRadius: 5,
      backgroundColor: colors.primary,
    },
    errorBox: {
      flexDirection: "row", alignItems: "flex-start", gap: 10,
      backgroundColor: colors.errorBg, borderRadius: 12,
      borderWidth: 1, borderColor: colors.error + "55", padding: 14,
    },
    errorText: { color: colors.error, fontSize: 13, flex: 1 },
    bottomBar: {
      paddingHorizontal: 20, paddingTop: 12,
      paddingBottom: insets.bottom + 16,
      backgroundColor: colors.surface, borderTopWidth: 1,
      borderTopColor: colors.primary + "30", gap: 8,
    },
    payBtn: {
      flexDirection: "row", alignItems: "center",
      justifyContent: "center", gap: 8,
      borderRadius: 14, paddingVertical: 15,
    },
    payBtnDisabled: { opacity: 0.5 },
    payBtnText: { color: "#FFFFFF", fontSize: 16, fontWeight: "800" },
    hint: { color: colors.textMuted, fontSize: 11, textAlign: "center" },
    centered: {
      flex: 1, alignItems: "center", justifyContent: "center",
      gap: 12, paddingHorizontal: 32,
    },
    centeredText: {
      color: colors.textSecondary, fontSize: 14, textAlign: "center",
    },
  });

  // Volver si llegamos sin datos
  useEffect(() => {
    if (!eventId || !seatIds.length) navigation.goBack();
  }, []);

  // Crear la orden al montar
  useEffect(() => {
    if (!eventId || !seatIds.length) return;
    const doCreate = async () => {
      setLoadingOrder(true);
      setError(null);
      try {
        const result = await createOrder({ eventId, seatIds });
        setOrder(result);
      } catch (e) {
        setError(e.message || "No se pudo crear la orden.");
      } finally {
        setLoadingOrder(false);
      }
    };
    doCreate();
  }, [eventId]);

  // Consultar wallet una vez que la orden esté lista
  useEffect(() => {
    if (!order) return;
    getMyWallet()
      .then((data) => setWalletBalance(data))
      .catch(() => setWalletBalance(null)); // sin wallet → no mostrar opción
  }, [order]);

  const localTotal     = cartItems.reduce((sum, item) => sum + (item.price ?? 0), 0);
  const orderTotal     = order?.totalAmount ?? order?.total ?? localTotal;
  const available      = walletBalance?.availableBalance ?? 0;
  const amountToApply  = Math.min(orderTotal, available);
  const remainder      = orderTotal - amountToApply;

  const handlePay = async () => {
    if (!order) return;
    setPaying(true);
    setError(null);
    try {
      const payment = await createPayment({
        orderId: order.orderId ?? order.id,
        paymentMethod: "QR",
        applyWalletCredit: useWallet,
      });

      const paymentId = payment.paymentId ?? payment.id;

      // Pago 100% wallet → aprobado al instante, ir directo a confirmación
      if (useWallet && (payment.amountToPayViaGateway ?? payment.amount) === 0) {
        navigation.replace("PaymentPending", {
          paymentId,
          orderId: order.orderId ?? order.id,
          eventId,
          cartItems,
          eventTitle,
          walletOnly: true,
        });
        return;
      }

      navigation.replace("PaymentPending", {
        paymentId,
        orderId: order.orderId ?? order.id,
        eventId,
        cartItems,
        eventTitle,
      });
    } catch (e) {
      setError(e.message || "No se pudo iniciar el pago. Intentá de nuevo.");
      setPaying(false);
    }
  };

  // ── Render ────────────────────────────────────────────────────────────────

  const renderContent = () => {
    if (loadingOrder) {
      return (
        <View style={styles.centered}>
          <ActivityIndicator size="large" color={colors.primary} />
          <Text style={styles.centeredText}>Preparando tu orden…</Text>
        </View>
      );
    }

    if (error && !order) {
      return (
        <View style={styles.centered}>
          <Ionicons name="alert-circle-outline" size={52} color={colors.error} />
          <Text style={[styles.centeredText, { color: colors.error }]}>{error}</Text>
          <TouchableOpacity
            onPress={() => navigation.goBack()}
            activeOpacity={0.75}
            style={{
              marginTop: 8, backgroundColor: colors.surface, borderRadius: 12,
              paddingHorizontal: 24, paddingVertical: 10,
              borderWidth: 1, borderColor: colors.error + "55",
            }}
          >
            <Text style={{ color: colors.textPrimary, fontWeight: "700" }}>
              Volver a selección
            </Text>
          </TouchableOpacity>
        </View>
      );
    }

    return (
      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.eventSubtitle}>{eventTitle}</Text>

        {/* ── Sillas seleccionadas ── */}
        <View style={styles.card}>
          <View style={styles.cardHeader}>
            <Ionicons name="ticket-outline" size={18} color={colors.accent} />
            <Text style={styles.cardTitle}>Sillas seleccionadas</Text>
            <View style={styles.badge}>
              <Text style={styles.badgeText}>
                {cartItems.length} {cartItems.length === 1 ? "silla" : "sillas"}
              </Text>
            </View>
          </View>

          {cartItems.map((item, index) => (
            <View key={item.seatId}>
              <View style={styles.seatRow}>
                <View style={styles.seatLeft}>
                  <Ionicons name="grid-outline" size={14} color={colors.textMuted} />
                  <View>
                    <Text style={styles.seatCode}>{item.code}</Text>
                    <Text style={styles.seatSection}>{item.sectionName}</Text>
                  </View>
                </View>
                <Text style={styles.seatPrice}>{formatCurrency(item.price)}</Text>
              </View>
              {index < cartItems.length - 1 && (
                <View style={{ height: 1, backgroundColor: colors.primary + "15" }} />
              )}
            </View>
          ))}

          <View style={styles.divider} />

          <View style={styles.totalRow}>
            <Text style={styles.totalLabel}>Total</Text>
            <Text style={styles.totalAmount}>{formatCurrency(orderTotal)}</Text>
          </View>
        </View>

        {/* ── Cartera — solo si hay saldo disponible ── */}
        {available > 0 && (
          <View style={styles.card}>
            <View style={styles.cardHeader}>
              <Ionicons name="wallet-outline" size={18} color={colors.accent} />
              <Text style={styles.cardTitle}>Cartera</Text>
              <View style={styles.walletBalanceBadge}>
                <Text style={styles.walletBalanceText}>
                  {formatCurrency(available)} disponibles
                </Text>
              </View>
            </View>

            {/* Toggle: Pago normal / Usar cartera */}
            <View style={styles.toggleRow}>
              <TouchableOpacity
                onPress={() => setUseWallet(false)}
                activeOpacity={0.8}
                style={[
                  styles.toggleBtn,
                  {
                    backgroundColor: !useWallet ? colors.primary : colors.primary + "15",
                    borderColor: !useWallet ? colors.primary : colors.primary + "30",
                  },
                ]}
              >
                <Text style={[
                  styles.toggleBtnText,
                  { color: !useWallet ? colors.textPrimary : colors.textSecondary },
                ]}>
                  Pago normal
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                onPress={() => setUseWallet(true)}
                activeOpacity={0.8}
                style={[
                  styles.toggleBtn,
                  {
                    backgroundColor: useWallet ? colors.primary : colors.primary + "15",
                    borderColor: useWallet ? colors.primary : colors.primary + "30",
                  },
                ]}
              >
                <Text style={[
                  styles.toggleBtnText,
                  { color: useWallet ? colors.textPrimary : colors.textSecondary },
                ]}>
                  Usar cartera
                </Text>
              </TouchableOpacity>
            </View>

            {/* Desglose cuando se usa wallet */}
            {useWallet && (
              <View style={{ marginTop: 14, gap: 0 }}>
                <View style={styles.breakdownRow}>
                  <Text style={styles.breakdownLabel}>Total de la orden</Text>
                  <Text style={[styles.breakdownValue, { color: colors.textPrimary }]}>
                    {formatCurrency(orderTotal)}
                  </Text>
                </View>
                <View style={styles.breakdownRow}>
                  <Text style={styles.breakdownLabel}>Descuento cartera</Text>
                  <Text style={[styles.breakdownValue, { color: colors.success }]}>
                    −{formatCurrency(amountToApply)}
                  </Text>
                </View>
                <View style={styles.breakdownDivider} />
                <View style={styles.breakdownRow}>
                  <Text style={styles.breakdownTotalLabel}>
                    {remainder === 0 ? "Cubierto por cartera" : "Restante por pasarela"}
                  </Text>
                  <Text style={styles.breakdownTotalValue}>
                    {formatCurrency(remainder)}
                  </Text>
                </View>
              </View>
            )}
          </View>
        )}

        {/* ── Método de pago — solo si hay que pagar algo por pasarela ── */}
        {(!useWallet || remainder > 0) && (
          <View style={styles.card}>
            <View style={styles.cardHeader}>
              <Ionicons name="card-outline" size={18} color={colors.accent} />
              <Text style={styles.cardTitle}>Método de pago</Text>
            </View>
            <View style={styles.paymentMethod}>
              <View style={styles.paymentMethodIcon}>
                <Text style={{ fontSize: 20 }}>📱</Text>
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.paymentMethodName}>Código QR</Text>
                <Text style={styles.paymentMethodSub}>Escanea el QR con tu app bancaria</Text>
              </View>
              <View style={styles.radioOuter}>
                <View style={styles.radioInner} />
              </View>
            </View>
          </View>
        )}

        {/* Error de pago */}
        {error && (
          <View style={styles.errorBox}>
            <Ionicons name="alert-circle-outline" size={18} color={colors.error} />
            <Text style={styles.errorText}>{error}</Text>
          </View>
        )}
      </ScrollView>
    );
  };

  // Texto y color del botón según modo
  const btnColor   = useWallet && remainder === 0 ? colors.primary : colors.success;
  const btnIcon    = useWallet && remainder === 0 ? "wallet-outline" : "checkmark-circle-outline";
  const btnLabel   = paying
    ? "Procesando…"
    : useWallet && remainder === 0
    ? `Pagar con cartera ${formatCurrency(orderTotal)}`
    : `Pagar ${formatCurrency(useWallet ? remainder : orderTotal)}`;

  return (
    <View style={styles.root}>
      <StatusBar barStyle="light-content" backgroundColor={colors.surface} />

      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          activeOpacity={0.75}
          style={styles.backButton}
        >
          <Ionicons name="arrow-back" size={20} color={colors.accent} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Confirmar compra</Text>
      </View>

      {renderContent()}

      {/* Barra de acción fija */}
      {!loadingOrder && order && (
        <View style={styles.bottomBar}>
          <TouchableOpacity
            onPress={handlePay}
            disabled={paying}
            activeOpacity={0.75}
            style={[styles.payBtn, { backgroundColor: btnColor }, paying && styles.payBtnDisabled]}
          >
            {paying ? (
              <ActivityIndicator size="small" color="#FFFFFF" />
            ) : (
              <Ionicons name={btnIcon} size={20} color="#FFFFFF" />
            )}
            <Text style={styles.payBtnText}>{btnLabel}</Text>
          </TouchableOpacity>
          <Text style={styles.hint}>
            Al confirmar, tus sillas quedan reservadas mientras se procesa el pago.
          </Text>
        </View>
      )}
    </View>
  );
}
