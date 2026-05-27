import { useState, useEffect, useCallback } from "react";
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
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useTheme } from "../context/ThemeContext";
import { getOrganizerRequests } from "../services/adminService";

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Formatea una fecha ISO a "DD/MM/YYYY HH:mm" sin librerías externas.
 * applicationDate llega como string ISO desde el backend (LocalDateTime serializado).
 */
function formatDate(isoString) {
  if (!isoString) return "—";
  const d = new Date(isoString);
  if (isNaN(d.getTime())) return isoString;
  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// ─── Componente principal ─────────────────────────────────────────────────────

export default function OrganizerRequestsScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();

  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // StyleSheet dentro del componente para acceder a colors del tema
  const styles = StyleSheet.create({
    container: {
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
    centered: {
      flex: 1,
      alignItems: "center",
      justifyContent: "center",
      paddingHorizontal: 32,
    },
    emptyText: {
      color: colors.textSecondary,
      fontSize: 15,
      textAlign: "center",
      marginTop: 12,
    },
    errorText: {
      color: colors.error,
      fontSize: 14,
      textAlign: "center",
      marginTop: 8,
    },
    list: {
      paddingHorizontal: 20,
      paddingTop: 20,
      paddingBottom: insets.bottom + 32,
    },
    card: {
      backgroundColor: colors.surface + "CC",
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.primary + "30",
      padding: 16,
      marginBottom: 12,
      flexDirection: "row",
      alignItems: "center",
      gap: 14,
    },
    cardIconWrap: {
      width: 44,
      height: 44,
      borderRadius: 12,
      backgroundColor: colors.primary + "28",
      borderWidth: 1,
      borderColor: colors.primary + "40",
      alignItems: "center",
      justifyContent: "center",
    },
    cardBody: {
      flex: 1,
    },
    cardAlias: {
      color: colors.textPrimary,
      fontSize: 15,
      fontWeight: "700",
    },
    cardDate: {
      color: colors.textMuted,
      fontSize: 12,
      marginTop: 3,
    },
    cardFooter: {
      flexDirection: "row",
      alignItems: "center",
      marginTop: 8,
      gap: 8,
    },
  });

  // Badge de estado — colores semánticos
  const getBadgeStyle = (status) => {
    if (status === "APPROVED") return { bg: colors.success + "22", border: colors.success + "55", text: colors.success, label: "Aprobada" };
    if (status === "REJECTED") return { bg: colors.error + "22",   border: colors.error + "55",   text: colors.error,   label: "Rechazada" };
    // PENDING y cualquier otro
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

  useEffect(() => {
    loadRequests();
  }, [loadRequests]);

  // ── Render ──────────────────────────────────────────────────────────────────

  const renderContent = () => {
    if (loading) {
      return (
        <View style={styles.centered}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      );
    }

    if (error) {
      return (
        <View style={styles.centered}>
          <Ionicons name="alert-circle-outline" size={48} color={colors.error} />
          <Text style={styles.errorText}>{error}</Text>
          <TouchableOpacity
            onPress={loadRequests}
            activeOpacity={0.75}
            style={{
              marginTop: 16,
              backgroundColor: colors.primary,
              borderRadius: 12,
              paddingHorizontal: 24,
              paddingVertical: 10,
            }}
          >
            <Text style={{ color: colors.textPrimary, fontWeight: "700", fontSize: 14 }}>
              Reintentar
            </Text>
          </TouchableOpacity>
        </View>
      );
    }

    if (requests.length === 0) {
      return (
        <View style={styles.centered}>
          <Ionicons name="people-outline" size={52} color={colors.textMuted} />
          <Text style={styles.emptyText}>No hay solicitudes pendientes</Text>
        </View>
      );
    }

    return (
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.list}
      >
        {requests.map((item) => {
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
              {/* Ícono */}
              <View style={styles.cardIconWrap}>
                <Ionicons name="person-outline" size={22} color={colors.accent} />
              </View>

              {/* Cuerpo */}
              <View style={styles.cardBody}>
                <Text style={styles.cardAlias}>{item.alias}</Text>
                <Text style={styles.cardDate}>{formatDate(item.applicationDate)}</Text>

                {/* Badge de estado */}
                <View style={styles.cardFooter}>
                  <View style={{
                    backgroundColor: badge.bg,
                    borderRadius: 20,
                    paddingHorizontal: 10,
                    paddingVertical: 3,
                    borderWidth: 1,
                    borderColor: badge.border,
                  }}>
                    <Text style={{ color: badge.text, fontSize: 11, fontWeight: "700" }}>
                      {badge.label}
                    </Text>
                  </View>
                </View>
              </View>

              {/* Chevron */}
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
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          activeOpacity={0.75}
          style={styles.backButton}
        >
          <Ionicons name="arrow-back" size={20} color={colors.accent} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Solicitudes de organizador</Text>
      </View>

      {renderContent()}
    </View>
  );
}
