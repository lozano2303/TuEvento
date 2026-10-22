import { useState, useEffect, useCallback } from "react";
import {
  View, Text, ScrollView, TouchableOpacity, ActivityIndicator,
  StatusBar, RefreshControl,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import { getAdminStats } from "../services/adminService";

const fmtNum = (n) => new Intl.NumberFormat("es-CO").format(Number(n) || 0);
const fmtCOP = (n) =>
  new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 })
    .format(Number(n) || 0);

export default function AdminFinancialScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();

  const [stats, setStats]       = useState(null);
  const [loading, setLoading]   = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError]       = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setStats(await getAdminStats());
    } catch {
      setError("No se pudieron cargar los datos financieros.");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);
  const onRefresh = () => { setRefreshing(true); load(); };

  const CARDS = [
    { icon: "trending-up",  color: "#34d399", label: "Ingresos totales",    value: fmtCOP(stats?.totalRevenue),        sub: "Pagos aprobados" },
    { icon: "ticket",       color: "#60a5fa", label: "Ventas activas",      value: fmtNum(stats?.approvedPayments),    sub: "Boletas vendidas" },
    { icon: "alert-circle", color: "#f87171", label: "Reembolsos pendientes", value: fmtNum(stats?.pendingRefunds),    sub: `${fmtNum(stats?.processedRefunds)} procesados` },
    { icon: "wallet",       color: "#a78bfa", label: "Total reembolsado",   value: fmtCOP(stats?.totalRefundedAmount), sub: "Monto devuelto" },
  ];

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <StatusBar barStyle="light-content" backgroundColor={colors.surface} />

      <View style={{
        flexDirection: "row", alignItems: "center", gap: 12,
        paddingTop: insets.top + 16, paddingBottom: 16, paddingHorizontal: 20,
        backgroundColor: colors.surface,
        borderBottomWidth: 1, borderBottomColor: colors.primary + "30",
      }}>
        <TouchableOpacity
          onPress={() => navigation.goBack()} activeOpacity={0.75}
          style={{
            width: 38, height: 38, borderRadius: 12,
            backgroundColor: colors.primary + "28",
            borderWidth: 1, borderColor: colors.primary + "40",
            alignItems: "center", justifyContent: "center",
          }}
        >
          <Ionicons name="arrow-back" size={20} color={colors.accent} />
        </TouchableOpacity>
        <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "800", flex: 1 }}>
          Control financiero
        </Text>
      </View>

      {loading ? (
        <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={{ padding: 20, paddingBottom: insets.bottom + 32, gap: 16 }}
          showsVerticalScrollIndicator={false}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}
        >
          {error && (
            <View style={{
              flexDirection: "row", alignItems: "center", gap: 8,
              backgroundColor: colors.error + "15", borderRadius: 12, padding: 14,
              borderWidth: 1, borderColor: colors.error + "40",
            }}>
              <Ionicons name="alert-circle-outline" size={18} color={colors.error} />
              <Text style={{ color: colors.error, fontSize: 13, flex: 1 }}>{error}</Text>
            </View>
          )}

          <Text style={{ color: colors.textSecondary, fontSize: 13, lineHeight: 20 }}>
            Monitoreo de transacciones y reembolsos de la plataforma.
          </Text>

          <View style={{ gap: 12 }}>
            {CARDS.map((c) => (
              <View key={c.label} style={{
                flexDirection: "row", alignItems: "center", gap: 14,
                backgroundColor: colors.surface + "CC",
                borderRadius: 16, borderWidth: 1, borderColor: colors.primary + "20",
                padding: 16,
              }}>
                <View style={{
                  width: 48, height: 48, borderRadius: 13,
                  backgroundColor: c.color + "22",
                  alignItems: "center", justifyContent: "center",
                }}>
                  <Ionicons name={c.icon} size={23} color={c.color} />
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={{ color: colors.textMuted, fontSize: 11, fontWeight: "700", textTransform: "uppercase", letterSpacing: 0.6 }}>
                    {c.label}
                  </Text>
                  <Text style={{ color: colors.textPrimary, fontSize: 22, fontWeight: "900", marginTop: 2 }}>
                    {c.value}
                  </Text>
                  <Text style={{ color: colors.textMuted, fontSize: 12, marginTop: 1 }}>{c.sub}</Text>
                </View>
              </View>
            ))}
          </View>
        </ScrollView>
      )}
    </View>
  );
}
