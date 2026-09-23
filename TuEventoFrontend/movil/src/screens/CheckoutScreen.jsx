import { useState, useEffect } from "react";
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  StatusBar,
  StyleSheet,
  Alert,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useRoute } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import { createOrder } from "../services/orderService";
import { createPayment } from "../services/paymentService";

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

  // Parámetros recibidos desde EventDetailScreen
  const { eventId, seatIds = [], cartItems = [], eventTitle = "Evento" } =
    route.params ?? {};

  const [order, setOrder] = useState(null);
  const [loadingOrder, setLoadingOrder] = useState(true);
  const [paying, setPaying] = useState(false);
  const [error, setError] = useState(null);

  // StyleSheet dentro del componente para acceder a colors
  const styles = StyleSheet.create({
    root: {
      flex: 1,
      backgroundColor: colors.background,
    },
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
      width: 38,
      height: 38,
      borderRadius: 12,
      backgroundColor: colors.primary + "28",
      borderWidth: 1,
      borderColor: colors.primary + "40",
      alignItems: "center",
      justifyContent: "center",
    },
    headerTitle: {
      color: colors.textPrimary,
      fontSize: 18,
      fontWeight: "800",
      flex: 1,
    },
    scroll: {
      flex: 1,
    },
    scrollContent: {
      paddingHorizontal: 20,
      paddingTop: 20,
      paddingBottom: 16,
      gap: 16,
    },
    eventSubtitle: {
      color: colors.textSecondary,
      fontSize: 13,
      marginBottom: 4,
    },
    // ── Tarjetas ──
    card: {
      backgroundColor: colors.surface + "CC",
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.primary + "30",
      padding: 16,
    },
    cardHeader: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
      marginBottom: 14,
    },
    cardTitle: {
      color: colors.textPrimary,
      fontSize: 14,
      fontWeight: "600",
      flex: 1,
    },
    badge: {
      backgroundColor: colors.primary + "28",
      borderRadius: 20,
      paddingHorizontal: 10,
      paddingVertical: 3,
    },
    badgeText: {
      color: colors.accent,
      fontSize: 11,
      fontWeight: "700",
    },
    seatRow: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      paddingVertical: 6,
    },
    seatLeft: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
    },
    seatCode: {
      color: colors.textPrimary,
      fontSize: 14,
      fontWeight: "600",
    },
    seatSection: {
      color: colors.textMuted,
      fontSize: 12,
    },
    seatPrice: {
      color: colors.accent,
      fontSize: 14,
      fontWeight: "700",
    },
    divider: {
      height: 1,
      backgroundColor: colors.primary + "20",
      marginVertical: 12,
    },
    totalRow: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
    },
    totalLabel: {
      color: colors.textSecondary,
      fontSize: 14,
      fontWeight: "600",
    },
    totalAmount: {
      color: colors.primary,
      fontSize: 20,
      fontWeight: "800",
    },
    // ── Método de pago ──
    paymentMethod: {
      flexDirection: "row",
      alignItems: "center",
      gap: 12,
      backgroundColor: colors.primary + "10",
      borderRadius: 12,
      borderWidth: 1,
      borderColor: colors.primary + "30",
      padding: 12,
    },
    paymentMethodIcon: {
      width: 40,
      height: 40,
      borderRadius: 10,
      backgroundColor: colors.primary + "20",
      alignItems: "center",
      justifyContent: "center",
    },
    paymentMethodName: {
      color: colors.textPrimary,
      fontSize: 14,
      fontWeight: "600",
    },
    paymentMethodSub: {
      color: colors.textMuted,
      fontSize: 12,
      marginTop: 2,
    },
    radioOuter: {
      width: 20,
      height: 20,
      borderRadius: 10,
      borderWidth: 2,
      borderColor: colors.primary,
      alignItems: "center",
      justifyContent: "center",
      marginLeft: "auto",
    },
    radioInner: {
      width: 10,
      height: 10,
      borderRadius: 5,
      backgroundColor: colors.primary,
    },
    // ── Error ──
    errorBox: {
      flexDirection: "row",
      alignItems: "flex-start",
      gap: 10,
      backgroundColor: colors.errorBg,
      borderRadius: 12,
      borderWidth: 1,
      borderColor: colors.error + "55",
      padding: 14,
    },
    errorText: {
      color: colors.error,
      fontSize: 13,
      flex: 1,
    },
    // ── Bottom bar ──
    bottomBar: {
      paddingHorizontal: 20,
      paddingTop: 12,
      paddingBottom: insets.bottom + 16,
      backgroundColor: colors.surface,
      borderTopWidth: 1,
      borderTopColor: colors.primary + "30",
      gap: 8,
    },
    payBtn: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      gap: 8,
      backgroundColor: colors.success,
      borderRadius: 14,
      paddingVertical: 15,
    },
    payBtnDisabled: {
      opacity: 0.5,
    },
    payBtnText: {
      color: "#FFFFFF",
      fontSize: 16,
      fontWeight: "800",
    },
    hint: {
      color: colors.textMuted,
      fontSize: 11,
      textAlign: "center",
    },
    // ── Estados ──
    centered: {
      flex: 1,
      alignItems: "center",
      justifyContent: "center",
      gap: 12,
      paddingHorizontal: 32,
    },
    centeredText: {
      color: colors.textSecondary,
      fontSize: 14,
      textAlign: "center",
    },
  });

  // Volver si llegamos sin datos
  useEffect(() => {
    if (!eventId || !seatIds.length) {
      navigation.goBack();
    }
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
        setError(e.message || "No se pudo crear la orden. Puede que alguna silla ya no esté disponible.");
      } finally {
        setLoadingOrder(false);
      }
    };
    doCreate();
  }, [eventId]);

  const handlePay = async () => {
    if (!order) return;
    setPaying(true);
    setError(null);
    try {
      const payment = await createPayment({
        orderId: order.orderId ?? order.id,
        paymentMethod: "QR",
      });
      navigation.replace("PaymentPending", {
        paymentId: payment.paymentId ?? payment.id,
        orderId:   order.orderId ?? order.id,
        eventId,
        cartItems,
        eventTitle,
      });
    } catch (e) {
      setError(e.message || "No se pudo iniciar el pago. Intentá de nuevo.");
      setPaying(false);
    }
  };

  const localTotal = cartItems.reduce((sum, item) => sum + (item.price ?? 0), 0);
  const orderTotal = order?.totalAmount ?? order?.total ?? localTotal;

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
              marginTop: 8,
              backgroundColor: colors.surface,
              borderRadius: 12,
              paddingHorizontal: 24,
              paddingVertical: 10,
              borderWidth: 1,
              borderColor: colors.error + "55",
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

        {/* Sillas seleccionadas */}
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

        {/* Método de pago */}
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
              <Text style={styles.paymentMethodSub}>
                Escanea el QR con tu app bancaria
              </Text>
            </View>
            <View style={styles.radioOuter}>
              <View style={styles.radioInner} />
            </View>
          </View>
        </View>

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
            style={[styles.payBtn, paying && styles.payBtnDisabled]}
          >
            {paying ? (
              <ActivityIndicator size="small" color="#FFFFFF" />
            ) : (
              <Ionicons name="checkmark-circle-outline" size={20} color="#FFFFFF" />
            )}
            <Text style={styles.payBtnText}>
              {paying ? "Procesando…" : `Pagar ${formatCurrency(orderTotal)}`}
            </Text>
          </TouchableOpacity>
          <Text style={styles.hint}>
            Al confirmar, tus sillas quedan reservadas mientras se procesa el pago.
          </Text>
        </View>
      )}
    </View>
  );
}
