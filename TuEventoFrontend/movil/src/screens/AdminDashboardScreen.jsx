import { useState, useEffect, useCallback } from "react";
import {
  View, Text, ScrollView, TouchableOpacity, ActivityIndicator,
  StatusBar, RefreshControl,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import { getAdminStats, getAdminActivity } from "../services/adminService";

// ─── Helpers ──────────────────────────────────────────────────────────────────

const fmtNum = (n) => new Intl.NumberFormat("es-CO").format(Number(n) || 0);
const fmtCOP = (n) =>
  new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 })
    .format(Number(n) || 0);

function relativeTime(iso) {
  if (!iso) return "";
  const diff = Date.now() - new Date(iso).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1) return "hace un momento";
  if (m < 60) return `hace ${m} min`;
  const h = Math.floor(m / 60);
  if (h < 24) return `hace ${h} h`;
  const d = Math.floor(h / 24);
  if (d < 7) return `hace ${d} d`;
  return new Date(iso).toLocaleDateString("es-CO", { day: "2-digit", month: "short" });
}

const ACTIVITY_TITLE_MAP = {
  "event approved and published": "Evento aprobado y publicado",
  "event approved": "Evento aprobado",
  "event published": "Evento publicado",
  "event rejected": "Evento rechazado",
  "event cancelled": "Evento cancelado",
  "event completed": "Evento finalizado",
  "event created as draft": "Evento creado como borrador",
  "event sent to review": "Evento enviado a revisión",
  "event pending review": "Evento en revisión",
  "event updated": "Evento actualizado",
  "new user registered": "Nuevo usuario registrado",
  "user registered": "Usuario registrado",
  "payment approved": "Pago aprobado",
  "payment rejected": "Pago rechazado",
  "payment refunded": "Reembolso procesado",
  "refund processed": "Reembolso procesado",
  "event status changed": "Estado del evento actualizado",
};
const translateTitle = (raw) =>
  !raw ? raw : (ACTIVITY_TITLE_MAP[raw.trim().toLowerCase()] ?? raw);

function activityIcon(type) {
  if (type === "USER_REGISTERED") return { icon: "person-add", color: "#a78bfa" };
  if (type === "PAYMENT")         return { icon: "card",        color: "#34d399" };
  return { icon: "calendar",       color: "#60a5fa" }; // EVENT_STATUS_CHANGE / default
}

// ─── Componente ───────────────────────────────────────────────────────────────

export default function AdminDashboardScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();

  const [stats, setStats]         = useState(null);
  const [activity, setActivity]   = useState([]);
  const [loading, setLoading]     = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError]         = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [s, a] = await Promise.allSettled([getAdminStats(), getAdminActivity(5)]);
      if (s.status === "fulfilled") setStats(s.value);
      else setError("No se pudieron cargar las estadísticas.");
      if (a.status === "fulfilled") setActivity(a.value);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const onRefresh = () => { setRefreshing(true); load(); };

  const STAT_CARDS = [
    { icon: "people",        color: "#a78bfa", label: "Usuarios",        value: fmtNum(stats?.totalUsers),          sub: `${fmtNum(stats?.approvedOrganizerRequests)} organizadores` },
    { icon: "calendar",      color: "#60a5fa", label: "Total eventos",   value: fmtNum(stats?.totalEvents),         sub: `${fmtNum(stats?.draftEvents)} borradores` },
    { icon: "flash",         color: "#34d399", label: "Eventos activos", value: fmtNum(stats?.publishedEvents),     sub: `${fmtNum(stats?.completedEvents)} finalizados` },
    { icon: "eye",           color: "#fbbf24", label: "En revisión",     value: fmtNum(stats?.pendingReviewEvents), sub: `${fmtNum(stats?.rejectedEvents)} rechazados` },
    { icon: "ticket",        color: "#f472b6", label: "Boletas vendidas",value: fmtNum(stats?.totalTickets),        sub: `${fmtNum(stats?.approvedPayments)} pagos` },
    { icon: "person-add",    color: "#f59e0b", label: "Solicitudes",     value: fmtNum(stats?.pendingOrganizerRequests), sub: "pendientes" },
  ];

  const FIN_CARDS = [
    { icon: "trending-up",   color: "#34d399", label: "Ingresos totales", value: fmtCOP(stats?.totalRevenue) },
    { icon: "ticket",        color: "#60a5fa", label: "Ventas activas",   value: fmtNum(stats?.approvedPayments) },
    { icon: "alert-circle",  color: "#f87171", label: "Reembolsos pend.", value: fmtNum(stats?.pendingRefunds) },
    { icon: "wallet",        color: "#a78bfa", label: "Total reembolsado",value: fmtCOP(stats?.totalRefundedAmount) },
  ];

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <StatusBar barStyle="light-content" backgroundColor={colors.surface} />

      {/* Header */}
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
          Dashboard
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

          {/* ── Stat cards (grid 2 col) ── */}
          <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 12 }}>
            {STAT_CARDS.map((c) => (
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
                <Text style={{ color: colors.textPrimary, fontSize: 22, fontWeight: "900" }}>{c.value}</Text>
                <Text style={{ color: colors.textMuted, fontSize: 11 }}>{c.sub}</Text>
              </View>
            ))}
          </View>

          {/* ── Control financiero resumido ── */}
          <View>
            <Text style={{ color: colors.textPrimary, fontSize: 16, fontWeight: "800", marginBottom: 12 }}>
              Control financiero
            </Text>
            <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 12 }}>
              {FIN_CARDS.map((c) => (
                <View key={c.label} style={{
                  width: "47.5%", flexGrow: 1,
                  backgroundColor: colors.surface + "CC",
                  borderRadius: 14, borderWidth: 1, borderColor: colors.primary + "20",
                  padding: 14, gap: 6,
                }}>
                  <View style={{
                    width: 34, height: 34, borderRadius: 10,
                    backgroundColor: c.color + "22",
                    alignItems: "center", justifyContent: "center",
                  }}>
                    <Ionicons name={c.icon} size={17} color={c.color} />
                  </View>
                  <Text style={{ color: colors.textMuted, fontSize: 10, fontWeight: "700", textTransform: "uppercase", letterSpacing: 0.5 }}>
                    {c.label}
                  </Text>
                  <Text style={{ color: colors.textPrimary, fontSize: 16, fontWeight: "800" }}>{c.value}</Text>
                </View>
              ))}
            </View>
          </View>

          {/* ── Actividad reciente ── */}
          <View>
            <Text style={{ color: colors.textPrimary, fontSize: 16, fontWeight: "800", marginBottom: 12 }}>
              Actividad reciente
            </Text>
            {activity.length === 0 ? (
              <View style={{
                backgroundColor: colors.surface + "CC", borderRadius: 14,
                padding: 24, alignItems: "center",
                borderWidth: 1, borderColor: colors.primary + "15",
              }}>
                <Ionicons name="time-outline" size={32} color={colors.textMuted} />
                <Text style={{ color: colors.textMuted, fontSize: 13, marginTop: 8 }}>
                  No hay actividad reciente
                </Text>
              </View>
            ) : (
              <View style={{ gap: 10 }}>
                {activity.map((item, idx) => {
                  const { icon, color } = activityIcon(item.type);
                  return (
                    <View key={idx} style={{
                      flexDirection: "row", alignItems: "flex-start", gap: 12,
                      backgroundColor: colors.surface + "CC",
                      borderRadius: 14, padding: 14,
                      borderWidth: 1, borderColor: colors.primary + "20",
                    }}>
                      <View style={{
                        width: 38, height: 38, borderRadius: 11,
                        backgroundColor: color + "22",
                        alignItems: "center", justifyContent: "center",
                      }}>
                        <Ionicons name={icon} size={18} color={color} />
                      </View>
                      <View style={{ flex: 1 }}>
                        <Text style={{ color: colors.textPrimary, fontSize: 13, fontWeight: "700" }}>
                          {translateTitle(item.title)}
                        </Text>
                        {!!(item.actor || item.description) && (
                          <Text style={{ color: colors.textSecondary, fontSize: 12, marginTop: 2 }} numberOfLines={2}>
                            {item.actor || item.description}
                          </Text>
                        )}
                        <Text style={{ color: colors.textMuted, fontSize: 11, marginTop: 3 }}>
                          {relativeTime(item.occurredAt)}
                        </Text>
                      </View>
                    </View>
                  );
                })}
              </View>
            )}
          </View>
        </ScrollView>
      )}
    </View>
  );
}
