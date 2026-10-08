import { useState, useEffect, useCallback } from "react";
import {
  View, Text, ScrollView, TouchableOpacity,
  ActivityIndicator, StatusBar, StyleSheet,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useTheme } from "../context/ThemeContext";
import { getOrganizerRequests } from "../services/adminService";

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatDate(isoString) {
  if (!isoString) return "—";
  const d = new Date(isoString);
  if (isNaN(d.getTime())) return isoString;
  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// ─── Filtros ──────────────────────────────────────────────────────────────────

const FILTERS = [
  { key: "ALL",      label: "Todas",     icon: "list-outline"           },
  { key: "PENDING",  label: "Pendientes",icon: "time-outline"           },
  { key: "APPROVED", label: "Aprobadas", icon: "checkmark-circle-outline" },
  { key: "REJECTED", label: "Rechazadas",icon: "close-circle-outline"  },
];

// ─── Componente principal ─────────────────────────────────────────────────────

export default function OrganizerRequestsScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();

  const [requests, setRequests] = useState([]);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState(null);
  const [filter, setFilter]     = useState("ALL");

  const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: colors.background },
    header: {
      flexDirection: "row", alignItems: "center",
      paddingTop: insets.top + 16, paddingBottom: 16,
      paddingHorizontal: 20,
      backgroundColor: colors.surface,
      borderBottomWidth: 1, borderBottomColor: colors.primary + "30",
      gap: 12,
    },
    backButton: {
      width: 38, height: 38, borderRadius: 12,
      backgroundColor: colors.primary + "28",
      borderWidth: 1, borderColor: colors.primary + "40",
      alignItems: "center", justifyContent: "center",
    },
    headerTitle: { color: colors.textPrimary, fontSize: 18, fontWeight: "800", flex: 1 },
    // ── Filtros ──
    filtersRow: {
      flexDirection: "row", paddingHorizontal: 20,
      paddingTop: 14, paddingBottom: 6, gap: 8,
    },
    chip: {
      flex: 1, flexDirection: "row", alignItems: "center",
      justifyContent: "center", gap: 5,
      paddingVertical: 9, borderRadius: 12, borderWidth: 1,
    },
    chipText: { fontSize: 11, fontWeight: "700" },
    // ── Lista ──
    centered: { flex: 1, alignItems: "center", justifyContent: "center", paddingHorizontal: 32, gap: 12 },
    emptyText: { color: colors.textSecondary, fontSize: 15, textAlign: "center", marginTop: 12 },
    errorText: { color: colors.error, fontSize: 14, textAlign: "center", marginTop: 8 },
    list: { paddingHorizontal: 20, paddingTop: 14, paddingBottom: insets.bottom + 32, gap: 10 },
    card: {
      backgroundColor: colors.surface + "CC",
      borderRadius: 14, borderWidth: 1,
      borderColor: colors.primary + "30",
      padding: 16, flexDirection: "row",
      alignItems: "center", gap: 14,
    },
    cardIconWrap: {
      width: 44, height: 44, borderRadius: 12,
      backgroundColor: colors.primary + "28",
      borderWidth: 1, borderColor: colors.primary + "40",
      alignItems: "center", justifyContent: "center",
    },
    cardBody: { flex: 1 },
    cardAlias: { color: colors.textPrimary, fontSize: 15, fontWeight: "700" },
    cardDate: { color: colors.textMuted, fontSize: 12, marginTop: 3 },
    cardFooter: { flexDirection: "row", alignItems: "center", marginTop: 8, gap: 8 },
  });

  const getBadgeStyle = (status) => {
    if (status === "APPROVED") return { bg: colors.success + "22", border: colors.success + "55", text: colors.success, label: "Aprobada" };
    if (status === "REJECTED") return { bg: colors.error + "22",   border: colors.error + "55",   text: colors.error,   label: "Rechazada" };
    return { bg: "#F59E0B22", border: "#F59E0B55", text: "#F59E0B", label: "Pendiente" };
  };

  const loadRequests = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const token = await AsyncStorage.getItem("accessToken");
      const data = await getOrganizerRequests(token);
      setRequests(data ?? []);
    } catch (e) {
      setError("No se pudieron cargar las solicitudes. Intenta de nuevo.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadRequests(); }, [loadRequests]);

  // Filtrado local (sin re-fetch — la API devuelve todo)
  const filtered = requests.filter((r) => {
    if (filter === "ALL")      return true;
    if (filter === "PENDING")  return r.status === "PENDING";
    if (filter === "APPROVED") return r.status === "APPROVED";
    if (filter === "REJECTED") return r.status === "REJECTED";
    return true;
  });

  // Contadores para los chips
  const counts = {
    ALL:      requests.length,
    PENDING:  requests.filter((r) => r.status === "PENDING").length,
    APPROVED: requests.filter((r) => r.status === "APPROVED").length,
    REJECTED: requests.filter((r) => r.status === "REJECTED").length,
  };

  const renderContent = () => {
    if (loading) {
      return <View style={styles.centered}><ActivityIndicator size="large" color={colors.primary} /></View>;
    }

    if (error) {
      return (
        <View style={styles.centered}>
          <Ionicons name="alert-circle-outline" size={48} color={colors.error} />
          <Text style={styles.errorText}>{error}</Text>
          <TouchableOpacity
            onPress={loadRequests} activeOpacity={0.75}
            style={{ marginTop: 16, backgroundColor: colors.primary, borderRadius: 12, paddingHorizontal: 24, paddingVertical: 10 }}
          >
            <Text style={{ color: colors.textPrimary, fontWeight: "700", fontSize: 14 }}>Reintentar</Text>
          </TouchableOpacity>
        </View>
      );
    }

    if (filtered.length === 0) {
      return (
        <View style={styles.centered}>
          <Ionicons name="people-outline" size={52} color={colors.textMuted} />
          <Text style={styles.emptyText}>
            {filter === "ALL" ? "No hay solicitudes" : `No hay solicitudes ${FILTERS.find(f => f.key === filter)?.label.toLowerCase()}`}
          </Text>
        </View>
      );
    }

    return (
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.list}>
        {filtered.map((item) => {
          const badge = getBadgeStyle(item.status);
          return (
            <TouchableOpacity
              key={item.organizerPetitionId}
              activeOpacity={0.75}
              style={styles.card}
              onPress={() =>
                navigation.navigate("OrganizerRequestDetail", {
                  petitionId:      item.organizerPetitionId,
                  alias:           item.alias,
                  applicationDate: item.applicationDate,
                  status:          item.status,
                  storedFileId:    item.storedFileId ?? null,
                })
              }
            >
              <View style={styles.cardIconWrap}>
                <Ionicons name="person-outline" size={22} color={colors.accent} />
              </View>

              <View style={styles.cardBody}>
                <Text style={styles.cardAlias}>{item.alias}</Text>
                <Text style={styles.cardDate}>{formatDate(item.applicationDate)}</Text>

                <View style={styles.cardFooter}>
                  <View style={{
                    backgroundColor: badge.bg, borderRadius: 20,
                    paddingHorizontal: 10, paddingVertical: 3,
                    borderWidth: 1, borderColor: badge.border,
                  }}>
                    <Text style={{ color: badge.text, fontSize: 11, fontWeight: "700" }}>{badge.label}</Text>
                  </View>
                </View>
              </View>

              <Ionicons name="chevron-forward" size={18} color={colors.textMuted} />
            </TouchableOpacity>
          );
        })}
      </ScrollView>
    );
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={colors.surface} />

      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} activeOpacity={0.75} style={styles.backButton}>
          <Ionicons name="arrow-back" size={20} color={colors.accent} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Solicitudes de organizador</Text>
        {counts.PENDING > 0 && (
          <View style={{
            backgroundColor: "#f59e0b", borderRadius: 10,
            minWidth: 22, height: 22, alignItems: "center",
            justifyContent: "center", paddingHorizontal: 5,
          }}>
            <Text style={{ color: "#fff", fontSize: 11, fontWeight: "800" }}>{counts.PENDING}</Text>
          </View>
        )}
      </View>

      {/* ── Chips de filtro ── */}
      {!loading && !error && (
        <View style={styles.filtersRow}>
          {FILTERS.map(({ key, label, icon }) => {
            const active = filter === key;
            const count = counts[key];

            // Color de acento por tipo
            const accentColor =
              key === "APPROVED" ? colors.success :
              key === "REJECTED" ? colors.error :
              key === "PENDING"  ? "#f59e0b" :
              colors.primary;

            return (
              <TouchableOpacity
                key={key}
                onPress={() => setFilter(key)}
                activeOpacity={0.8}
                style={[
                  styles.chip,
                  {
                    backgroundColor: active ? accentColor + "22" : colors.surface + "CC",
                    borderColor: active ? accentColor + "66" : colors.primary + "25",
                  },
                ]}
              >
                <Ionicons
                  name={icon}
                  size={13}
                  color={active ? accentColor : colors.textMuted}
                />
                <Text style={[
                  styles.chipText,
                  { color: active ? accentColor : colors.textSecondary },
                ]}>
                  {label}
                  {count > 0 ? ` (${count})` : ""}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>
      )}

      {renderContent()}
    </View>
  );
}
