import { useState, useEffect, useCallback } from "react";
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  StatusBar,
  StyleSheet,
  RefreshControl,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import { getMyWallet, getMyWalletTransactions } from "../services/walletService";

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatCurrency(amount) {
  if (amount == null) return "$0";
  return `$${Number(Math.abs(amount)).toLocaleString("es-CO")}`;
}

function formatDate(isoStr) {
  if (!isoStr) return "";
  return new Date(isoStr).toLocaleDateString("es-CO", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

// CREDIT/REVERSAL suman, PAYMENT resta, ADJUSTMENT puede ir en cualquier dirección
function isAdditive(type) {
  return type === "CREDIT" || type === "REVERSAL" || type === "ADJUSTMENT";
}

function txIcon(type) {
  switch (type) {
    case "CREDIT":    return "arrow-down-circle";
    case "REVERSAL":  return "refresh-circle";
    case "PAYMENT":   return "cart";
    case "ADJUSTMENT": return "construct";
    default:          return "ellipse";
  }
}

function txLabel(type) {
  switch (type) {
    case "CREDIT":     return "Crédito";
    case "REVERSAL":   return "Reversión";
    case "PAYMENT":    return "Compra";
    case "ADJUSTMENT": return "Ajuste";
    default:           return type;
  }
}

function statusLabel(status) {
  switch (status) {
    case "COMPLETED": return "Completado";
    case "PENDING":   return "Pendiente";
    case "FAILED":    return "Fallido";
    default:          return status;
  }
}

// ─── Componente principal ─────────────────────────────────────────────────────

export default function WalletScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();

  const [wallet, setWallet]           = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading]         = useState(true);
  const [refreshing, setRefreshing]   = useState(false);
  const [error, setError]             = useState(null);

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
      color: colors.textPrimary, fontSize: 18,
      fontWeight: "800", flex: 1,
    },
    scroll: { flex: 1 },
    scrollContent: {
      paddingHorizontal: 20,
      paddingTop: 20,
      paddingBottom: insets.bottom + 32,
      gap: 16,
    },
    // ── Balance card ──
    balanceCard: {
      backgroundColor: colors.surface + "CC",
      borderRadius: 18,
      borderWidth: 1,
      borderColor: colors.primary + "30",
      padding: 20,
      gap: 12,
    },
    balanceLabel: {
      color: colors.textMuted,
      fontSize: 11,
      fontWeight: "700",
      textTransform: "uppercase",
      letterSpacing: 1.2,
    },
    balanceAmount: {
      color: colors.textPrimary,
      fontSize: 38,
      fontWeight: "900",
      letterSpacing: -0.5,
    },
    balanceCurrency: {
      color: colors.textMuted,
      fontSize: 12,
      marginTop: 2,
    },
    availableRow: {
      flexDirection: "row",
      alignItems: "center",
      gap: 6,
      backgroundColor: colors.primary + "15",
      borderRadius: 10,
      paddingHorizontal: 12,
      paddingVertical: 8,
      alignSelf: "flex-start",
    },
    availableText: {
      color: colors.accent,
      fontSize: 13,
      fontWeight: "700",
    },
    protectedBadge: {
      flexDirection: "row",
      alignItems: "center",
      gap: 5,
      alignSelf: "flex-start",
    },
    protectedText: {
      color: colors.success,
      fontSize: 11,
      fontWeight: "600",
    },
    ctaBtn: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      gap: 8,
      backgroundColor: colors.primary,
      borderRadius: 13,
      paddingVertical: 13,
      marginTop: 4,
    },
    ctaBtnText: {
      color: colors.textPrimary,
      fontSize: 14,
      fontWeight: "700",
    },
    // ── Sección historial ──
    sectionHeader: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: 4,
    },
    sectionTitle: {
      color: colors.textPrimary,
      fontSize: 15,
      fontWeight: "700",
    },
    sectionCount: {
      color: colors.textMuted,
      fontSize: 12,
    },
    // ── Ítem de transacción ──
    txItem: {
      flexDirection: "row",
      alignItems: "center",
      gap: 12,
      backgroundColor: colors.surface + "CC",
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.primary + "20",
      padding: 14,
    },
    txIconWrap: {
      width: 40, height: 40, borderRadius: 12,
      alignItems: "center", justifyContent: "center",
    },
    txMeta: { flex: 1, gap: 2 },
    txLabel: {
      color: colors.textPrimary, fontSize: 14, fontWeight: "600",
    },
    txDate: { color: colors.textMuted, fontSize: 11 },
    txStatusBadge: {
      borderRadius: 8,
      paddingHorizontal: 8,
      paddingVertical: 3,
      alignSelf: "flex-start",
    },
    txStatusText: { fontSize: 10, fontWeight: "700" },
    txAmount: { fontSize: 15, fontWeight: "800", textAlign: "right" },
    // ── Estado vacío / error ──
    emptyWrap: {
      alignItems: "center",
      justifyContent: "center",
      paddingVertical: 60,
      gap: 12,
    },
    emptyTitle: {
      color: colors.textPrimary, fontSize: 18, fontWeight: "700",
      textAlign: "center",
    },
    emptyText: {
      color: colors.textSecondary, fontSize: 13,
      textAlign: "center", lineHeight: 20,
      maxWidth: 280,
    },
    centered: {
      flex: 1, alignItems: "center",
      justifyContent: "center", gap: 12,
    },
  });

  const load = useCallback(async () => {
    try {
      setError(null);
      const [walletData, txData] = await Promise.allSettled([
        getMyWallet(),
        getMyWalletTransactions(),
      ]);
      if (walletData.status === "fulfilled") setWallet(walletData.value);
      if (txData.status === "fulfilled") setTransactions(txData.value ?? []);
    } catch (e) {
      setError(e.message || "Error al cargar la cartera");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const onRefresh = () => {
    setRefreshing(true);
    load();
  };

  // ── Loading ───────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <View style={styles.root}>
        <StatusBar barStyle="light-content" backgroundColor={colors.surface} />
        <View style={styles.header}>
          <TouchableOpacity onPress={() => navigation.goBack()} activeOpacity={0.75} style={styles.backButton}>
            <Ionicons name="arrow-back" size={20} color={colors.accent} />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>Mi Cartera</Text>
        </View>
        <View style={styles.centered}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      </View>
    );
  }

  // ── Sin wallet aún ────────────────────────────────────────────────────────
  if (!wallet) {
    return (
      <View style={styles.root}>
        <StatusBar barStyle="light-content" backgroundColor={colors.surface} />
        <View style={styles.header}>
          <TouchableOpacity onPress={() => navigation.goBack()} activeOpacity={0.75} style={styles.backButton}>
            <Ionicons name="arrow-back" size={20} color={colors.accent} />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>Mi Cartera</Text>
        </View>
        <ScrollView
          contentContainerStyle={[styles.scrollContent, { flex: 1, justifyContent: "center" }]}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}
        >
          <View style={styles.emptyWrap}>
            <Ionicons name="wallet-outline" size={64} color={colors.textMuted} />
            <Text style={styles.emptyTitle}>Aún no tenés saldo</Text>
            <Text style={styles.emptyText}>
              Tu saldo de cartera se generará cuando recibas un reembolso o crédito por cancelación de evento.
            </Text>
          </View>
        </ScrollView>
      </View>
    );
  }

  // ── Vista principal ───────────────────────────────────────────────────────
  const available = wallet.availableBalance ?? 0;
  const balance   = wallet.balance ?? 0;
  const reserved  = balance - available;

  return (
    <View style={styles.root}>
      <StatusBar barStyle="light-content" backgroundColor={colors.surface} />

      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} activeOpacity={0.75} style={styles.backButton}>
          <Ionicons name="arrow-back" size={20} color={colors.accent} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Mi Cartera</Text>
      </View>

      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}
      >

        {/* ── Tarjeta de balance ────────────────────────────────────────── */}
        <View style={styles.balanceCard}>
          <Text style={styles.balanceLabel}>Saldo disponible</Text>

          <View>
            <Text style={styles.balanceAmount}>{formatCurrency(available)}</Text>
            <Text style={styles.balanceCurrency}>{wallet.currency ?? "COP"}</Text>
          </View>

          {reserved > 0 && (
            <View style={styles.availableRow}>
              <Ionicons name="time-outline" size={14} color={colors.accent} />
              <Text style={styles.availableText}>
                {formatCurrency(reserved)} en compras pendientes
              </Text>
            </View>
          )}

          <View style={styles.protectedBadge}>
            <Ionicons name="shield-checkmark-outline" size={13} color={colors.success} />
            <Text style={styles.protectedText}>Crédito interno protegido</Text>
          </View>

          {/* CTA: ir a explorar eventos */}
          <TouchableOpacity
            onPress={() => navigation.navigate("Main")}
            activeOpacity={0.85}
            style={styles.ctaBtn}
          >
            <Ionicons name="wallet-outline" size={16} color={colors.textPrimary} />
            <Text style={styles.ctaBtnText}>Usar saldo en una compra</Text>
            <Ionicons name="chevron-forward" size={14} color={colors.textPrimary} />
          </TouchableOpacity>
        </View>

        {/* ── Historial de movimientos ──────────────────────────────────── */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Historial</Text>
          <Text style={styles.sectionCount}>{transactions.length} movimientos</Text>
        </View>

        {transactions.length === 0 ? (
          <View style={[styles.emptyWrap, { paddingVertical: 32 }]}>
            <Ionicons name="receipt-outline" size={40} color={colors.textMuted} />
            <Text style={[styles.emptyText, { fontSize: 13 }]}>
              Aún no hay movimientos en tu cartera.
            </Text>
          </View>
        ) : (
          transactions.map((tx) => {
            const additive    = isAdditive(tx.type);
            const iconName    = txIcon(tx.type);
            const iconColor   = additive ? colors.success : colors.accent;
            const iconBg      = additive ? colors.success + "22" : colors.accent + "22";
            const amountColor = additive ? colors.success : colors.error;
            const amountSign  = additive ? "+" : "−";

            // status badge colors
            const statusColors = {
              COMPLETED: { bg: colors.success + "20", text: colors.success },
              PENDING:   { bg: colors.accent  + "20", text: colors.accent  },
              FAILED:    { bg: colors.error   + "20", text: colors.error   },
            };
            const sc = statusColors[tx.status] ?? statusColors.PENDING;

            return (
              <View key={tx.transactionId} style={styles.txItem}>
                {/* Ícono */}
                <View style={[styles.txIconWrap, { backgroundColor: iconBg }]}>
                  <Ionicons name={iconName} size={20} color={iconColor} />
                </View>

                {/* Texto */}
                <View style={styles.txMeta}>
                  <Text style={styles.txLabel}>{txLabel(tx.type)}</Text>
                  <Text style={styles.txDate}>{formatDate(tx.createdAt)}</Text>
                  <View style={[styles.txStatusBadge, { backgroundColor: sc.bg }]}>
                    <Text style={[styles.txStatusText, { color: sc.text }]}>
                      {statusLabel(tx.status)}
                    </Text>
                  </View>
                </View>

                {/* Monto */}
                <Text style={[styles.txAmount, { color: amountColor }]}>
                  {amountSign}{formatCurrency(tx.amount)}
                </Text>
              </View>
            );
          })
        )}

      </ScrollView>
    </View>
  );
}
