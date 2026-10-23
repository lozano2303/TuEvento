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
const pct = (part, total) => {
  const t = Number(total) || 0;
  if (t === 0) return 0;
  return Math.round((Number(part || 0) / t) * 100);
};

export default function AdminReportsScreen() {
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
      setError("No se pudieron cargar los reportes.");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);
  const onRefresh = () => { setRefreshing(true); load(); };

  const totalEvents = stats?.totalEvents ?? 0;

  const EVENT_BREAKDOWN = [
    { label: "Publicados",  value: stats?.publishedEvents,     color: "#34d399" },
    { label: "Finalizados", value: stats?.completedEvents,     color: "#a78bfa" },
    { label: "En revisión", value: stats?.pendingReviewEvents, color: "#fbbf24" },
    { label: "Rechazados",  value: stats?.rejectedEvents,      color: "#f87171" },
    { label: "Cancelados",  value: stats?.cancelledEvents,     color: "#f87171" },
    { label: "Borradores",  value: stats?.draftEvents,         color: "#64748b" },
  ];

  const SUMMARY = [
    { icon: "trending-up", color: "#34d399", label: "Ingresos totales", value: fmtCOP(stats?.totalRevenue) },
    { icon: "people",      color: "#a78bfa", label: "Usuarios",         value: fmtNum(stats?.totalUsers), sub: `${fmtNum(stats?.approvedOrganizerRequests)} organizadores` },
    { icon: "ticket",      color: "#f472b6", label: "Boletas vendidas", value: fmtNum(stats?.totalTickets), sub: `${fmtNum(stats?.publishedEvents)} eventos activos` },
    { icon: "wallet",      color: "#60a5fa", label: "Total reembolsado",value: fmtCOP(stats?.totalRefundedAmount), sub: `${fmtNum(stats?.processedRefunds)} procesados` },
  ];

  // Indicadores clave (tasas)
  const publishRate  = pct(stats?.publishedEvents, stats?.totalEvents);
  const approveRate  = pct(stats?.approvedOrganizerRequests, (stats?.approvedOrganizerRequests || 0) + (stats?.pendingOrganizerRequests || 0));
  const refundRate   = pct((stats?.processedRefunds || 0) + (stats?.pendingRefunds || 0), stats?.approvedPayments);

  const INDICATORS = [
    { label: "Tasa de publicación",     value: publishRate },
    { label: "Aprobación organizadores",value: approveRate },
    { label: "Tasa de reembolso",       value: refundRate },
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
          Reportes
        </Text>
      </View>

      {loading ? (
        <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={{ padding: 20, paddingBottom: insets.bottom + 32, gap: 20 }}
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

          {/* Resumen (grid 2 col) */}
          <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 12 }}>
            {SUMMARY.map((c) => (
              <View key={c.label} style={{
                width: "47.5%", flexGrow: 1,
                backgroundColor: colors.surface + "CC",
                borderRadius: 16, borderWidth: 1, borderColor: colors.primary + "20",
                padding: 14, gap: 8,
              }}>
                <View style={{
                  width: 38, height: 38, borderRadius: 11,
                  backgroundColor: c.color + "22",
                  alignItems: "center", justifyContent: "center",
                }}>
                  <Ionicons name={c.icon} size={19} color={c.color} />
                </View>
                <Text style={{ color: colors.textMuted, fontSize: 10, fontWeight: "700", textTransform: "uppercase", letterSpacing: 0.6 }}>
                  {c.label}
                </Text>
                <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "900" }}>{c.value}</Text>
                {c.sub && <Text style={{ color: colors.textMuted, fontSize: 11 }}>{c.sub}</Text>}
              </View>
            ))}
          </View>

          {/* Eventos por estado */}
          <View style={{
            backgroundColor: colors.surface + "CC", borderRadius: 16,
            borderWidth: 1, borderColor: colors.primary + "20", padding: 16,
          }}>
            <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginBottom: 14 }}>
              <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "800" }}>
                Eventos por estado
              </Text>
              <Text style={{ color: colors.textMuted, fontSize: 12 }}>{fmtNum(totalEvents)} total</Text>
            </View>
            <View style={{ gap: 12 }}>
              {EVENT_BREAKDOWN.map((row) => {
                const percentage = pct(row.value, totalEvents);
                return (
                  <View key={row.label} style={{ gap: 5 }}>
                    <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
                      <Text style={{ color: colors.textSecondary, fontSize: 13 }}>{row.label}</Text>
                      <Text style={{ color: colors.textPrimary, fontSize: 13, fontWeight: "700" }}>
                        {fmtNum(row.value)} · {percentage}%
                      </Text>
                    </View>
                    <View style={{ height: 6, borderRadius: 3, backgroundColor: colors.primary + "15", overflow: "hidden" }}>
                      <View style={{ width: `${percentage}%`, height: "100%", backgroundColor: row.color, borderRadius: 3 }} />
                    </View>
                  </View>
                );
              })}
            </View>
          </View>

          {/* Indicadores clave */}
          <View style={{
            backgroundColor: colors.surface + "CC", borderRadius: 16,
            borderWidth: 1, borderColor: colors.primary + "20", padding: 16,
          }}>
            <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "800", marginBottom: 14 }}>
              Indicadores clave
            </Text>
            <View style={{ flexDirection: "row", gap: 10 }}>
              {INDICATORS.map((ind) => (
                <View key={ind.label} style={{
                  flex: 1, backgroundColor: colors.primary + "10",
                  borderRadius: 12, padding: 12, alignItems: "center",
                  borderWidth: 1, borderColor: colors.primary + "25",
                }}>
                  <Text style={{ color: colors.accent, fontSize: 24, fontWeight: "900" }}>{ind.value}%</Text>
                  <Text style={{ color: colors.textMuted, fontSize: 10, textAlign: "center", marginTop: 4 }}>
                    {ind.label}
                  </Text>
                </View>
              ))}
            </View>
          </View>
        </ScrollView>
      )}
    </View>
  );
}
