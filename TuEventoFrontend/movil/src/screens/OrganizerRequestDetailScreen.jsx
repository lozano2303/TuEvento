import { useState, useEffect, useCallback } from "react";
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  StatusBar,
  StyleSheet,
  Alert,
  Linking,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useRoute } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useTheme } from "../context/ThemeContext";
import {
  getFilePresignedUrl,
  approveOrganizerRequest,
  rejectOrganizerRequest,
} from "../services/adminService";

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatDate(isoString) {
  if (!isoString) return "—";
  const d = new Date(isoString);
  if (isNaN(d.getTime())) return isoString;
  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// ─── Componente principal ─────────────────────────────────────────────────────

export default function OrganizerRequestDetailScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const route = useRoute();

  const { petitionId, alias, applicationDate, status, storedFileId } =
    route.params ?? {};

  // ── Estado del documento ──────────────────────────────────────────────────
  const [pdfUrl, setPdfUrl] = useState(null);
  const [pdfLoading, setPdfLoading] = useState(!!storedFileId);
  const [pdfError, setPdfError] = useState(null);

  // ── Estado de las acciones ────────────────────────────────────────────────
  const [actionLoading, setActionLoading] = useState(false);
  // Estado local del status para reflejar cambios tras aprobar/rechazar
  const [currentStatus, setCurrentStatus] = useState(status);

  // ── StyleSheet dentro del componente para acceder a colors ───────────────
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
    },
    // ── Info card ──
    infoCard: {
      backgroundColor: colors.surface + "CC",
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.primary + "30",
      padding: 16,
      marginBottom: 20,
      gap: 10,
    },
    infoRow: {
      flexDirection: "row",
      alignItems: "center",
      gap: 10,
    },
    infoIconWrap: {
      width: 36,
      height: 36,
      borderRadius: 10,
      backgroundColor: colors.primary + "28",
      borderWidth: 1,
      borderColor: colors.primary + "40",
      alignItems: "center",
      justifyContent: "center",
    },
    infoLabel: {
      color: colors.textMuted,
      fontSize: 11,
      fontWeight: "600",
      textTransform: "uppercase",
      letterSpacing: 0.8,
    },
    infoValue: {
      color: colors.textPrimary,
      fontSize: 15,
      fontWeight: "700",
      marginTop: 1,
    },
    // ── Sección PDF ──
    pdfSection: {
      marginBottom: 8,
    },
    pdfSectionTitle: {
      color: colors.textSecondary,
      fontSize: 11,
      fontWeight: "700",
      letterSpacing: 1.2,
      textTransform: "uppercase",
      marginBottom: 12,
    },
    pdfPlaceholder: {
      height: 420,
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.primary + "30",
      backgroundColor: colors.surface + "CC",
      alignItems: "center",
      justifyContent: "center",
      gap: 10,
      paddingHorizontal: 24,
    },
    pdfPlaceholderText: {
      color: colors.textSecondary,
      fontSize: 14,
      textAlign: "center",
    },
    pdfErrorText: {
      color: colors.error,
      fontSize: 13,
      textAlign: "center",
      marginTop: 4,
    },
    btnViewDoc: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      gap: 8,
      borderRadius: 14,
      paddingVertical: 14,
      backgroundColor: colors.primary,
    },
    btnViewDocText: {
      color: "#FFFFFF",
      fontSize: 15,
      fontWeight: "700",
    },
    // ── Barra de acciones ──
    actionBar: {
      paddingHorizontal: 20,
      paddingTop: 12,
      paddingBottom: insets.bottom + 16,
      backgroundColor: colors.surface,
      borderTopWidth: 1,
      borderTopColor: colors.primary + "30",
      gap: 10,
    },
    processedText: {
      color: colors.textSecondary,
      fontSize: 14,
      textAlign: "center",
      paddingVertical: 8,
    },
    btnRow: {
      flexDirection: "row",
      gap: 12,
    },
    btnReject: {
      flex: 1,
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      gap: 6,
      borderRadius: 14,
      paddingVertical: 14,
      backgroundColor: colors.errorBg,
      borderWidth: 1,
      borderColor: colors.error + "55",
    },
    btnRejectText: {
      color: colors.error,
      fontSize: 15,
      fontWeight: "700",
    },
    btnApprove: {
      flex: 1,
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      gap: 6,
      borderRadius: 14,
      paddingVertical: 14,
      backgroundColor: colors.success,
    },
    btnApproveText: {
      color: "#FFFFFF",
      fontSize: 15,
      fontWeight: "700",
    },
  });

  // ── Badge de estado — idéntico a OrganizerRequestsScreen ─────────────────
  const getBadgeStyle = (s) => {
    if (s === "APPROVED") return { bg: colors.success + "22", border: colors.success + "55", text: colors.success, label: "Aprobada" };
    if (s === "REJECTED") return { bg: colors.error + "22",   border: colors.error + "55",   text: colors.error,   label: "Rechazada" };
    return { bg: "#F59E0B22", border: "#F59E0B55", text: "#F59E0B", label: "Pendiente" };
  };

  // ── Carga de URL presignada ───────────────────────────────────────────────
  const loadPdfUrl = useCallback(async () => {
    if (!storedFileId) return; // sin documento — no llamar al backend
    setPdfLoading(true);
    setPdfError(null);
    try {
      const token = await AsyncStorage.getItem("accessToken");
      const data = await getFilePresignedUrl(storedFileId, token);
      const fixedUrl = data?.publicUrl?.replace('localhost', process.env.EXPO_PUBLIC_MINIO_HOST);
      setPdfUrl(fixedUrl ?? null);
    } catch (e) {
      setPdfError("No se pudo cargar el documento. Intenta de nuevo.");
    } finally {
      setPdfLoading(false);
    }
  }, [storedFileId]);

  useEffect(() => {
    loadPdfUrl();
  }, [loadPdfUrl]);

  // ── Acciones ─────────────────────────────────────────────────────────────
  const handleReject = () => {
    Alert.alert(
      "¿Rechazar solicitud?",
      "Esta acción no se puede deshacer.",
      [
        { text: "Cancelar", style: "cancel" },
        {
          text: "Rechazar",
          style: "destructive",
          onPress: async () => {
            setActionLoading(true);
            try {
              const token = await AsyncStorage.getItem("accessToken");
              await rejectOrganizerRequest(petitionId, token);
              setCurrentStatus("REJECTED");
              Alert.alert(
                "Solicitud rechazada",
                "La solicitud ha sido rechazada correctamente.",
                [{ text: "OK", onPress: () => navigation.goBack() }]
              );
            } catch (e) {
              Alert.alert(
                "Error",
                "No se pudo rechazar la solicitud. Intenta de nuevo."
              );
            } finally {
              setActionLoading(false);
            }
          },
        },
      ]
    );
  };

  const handleApprove = () => {
    Alert.alert(
      "¿Aprobar solicitud?",
      "El usuario recibirá el rol de organizador.",
      [
        { text: "Cancelar", style: "cancel" },
        {
          text: "Aprobar",
          onPress: async () => {
            setActionLoading(true);
            try {
              const token = await AsyncStorage.getItem("accessToken");
              await approveOrganizerRequest(petitionId, token);
              setCurrentStatus("APPROVED");
              Alert.alert(
                "Solicitud aprobada",
                "El usuario ahora tiene el rol de organizador.",
                [{ text: "OK", onPress: () => navigation.goBack() }]
              );
            } catch (e) {
              Alert.alert(
                "Error",
                "No se pudo aprobar la solicitud. Intenta de nuevo."
              );
            } finally {
              setActionLoading(false);
            }
          },
        },
      ]
    );
  };

  // ── Render sección PDF ────────────────────────────────────────────────────
  const renderPdfSection = () => {
    // Sin documento adjunto
    if (!storedFileId) {
      return (
        <View style={styles.pdfPlaceholder}>
          <Ionicons name="document-outline" size={44} color={colors.textMuted} />
          <Text style={styles.pdfPlaceholderText}>
            Esta solicitud no tiene documento adjunto
          </Text>
        </View>
      );
    }

    // Cargando URL
    if (pdfLoading) {
      return (
        <View style={styles.pdfPlaceholder}>
          <ActivityIndicator size="large" color={colors.primary} />
          <Text style={styles.pdfPlaceholderText}>Cargando documento...</Text>
        </View>
      );
    }

    // Error al obtener URL
    if (pdfError) {
      return (
        <View style={styles.pdfPlaceholder}>
          <Ionicons name="alert-circle-outline" size={44} color={colors.error} />
          <Text style={styles.pdfErrorText}>{pdfError}</Text>
          <TouchableOpacity
            onPress={loadPdfUrl}
            activeOpacity={0.75}
            style={{
              marginTop: 8,
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

    // URL lista — abrir con Linking
    return (
      <View style={styles.pdfPlaceholder}>
        <Ionicons name="document-outline" size={44} color={colors.primary} />
        <TouchableOpacity
          onPress={() => Linking.openURL(pdfUrl)}
          activeOpacity={0.75}
          style={styles.btnViewDoc}
        >
          <Ionicons name="document-outline" size={18} color="#FFFFFF" />
          <Text style={styles.btnViewDocText}>Ver documento</Text>
        </TouchableOpacity>
      </View>
    );
  };

  // ── Render barra de acciones ──────────────────────────────────────────────
  const renderActionBar = () => {
    // Solicitud ya procesada
    if (currentStatus !== "PENDING") {
      return (
        <View style={styles.actionBar}>
          <Text style={styles.processedText}>Esta solicitud ya fue procesada</Text>
        </View>
      );
    }

    // Procesando acción
    if (actionLoading) {
      return (
        <View style={[styles.actionBar, { alignItems: "center" }]}>
          <ActivityIndicator size="small" color={colors.primary} />
        </View>
      );
    }

    // Botones de acción
    return (
      <View style={styles.actionBar}>
        <View style={styles.btnRow}>
          <TouchableOpacity
            onPress={handleReject}
            activeOpacity={0.75}
            style={styles.btnReject}
            disabled={actionLoading}
          >
            <Ionicons name="close-circle-outline" size={18} color={colors.error} />
            <Text style={styles.btnRejectText}>Rechazar</Text>
          </TouchableOpacity>

          <TouchableOpacity
            onPress={handleApprove}
            activeOpacity={0.75}
            style={styles.btnApprove}
            disabled={actionLoading}
          >
            <Ionicons name="checkmark-circle-outline" size={18} color="#FFFFFF" />
            <Text style={styles.btnApproveText}>Aprobar</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  };

  // ── Render principal ──────────────────────────────────────────────────────
  const badge = getBadgeStyle(currentStatus);

  return (
    <View style={styles.root}>
      <StatusBar barStyle="light-content" backgroundColor={colors.surface} />

      {/* ── Header ── */}
      <View style={styles.header}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          activeOpacity={0.75}
          style={styles.backButton}
        >
          <Ionicons name="arrow-back" size={20} color={colors.accent} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Detalle de solicitud</Text>
      </View>

      {/* ── Scroll: info card + PDF ── */}
      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* Info card */}
        <View style={styles.infoCard}>
          {/* Alias */}
          <View style={styles.infoRow}>
            <View style={styles.infoIconWrap}>
              <Ionicons name="person-outline" size={18} color={colors.accent} />
            </View>
            <View>
              <Text style={styles.infoLabel}>Solicitante</Text>
              <Text style={styles.infoValue}>{alias ?? "—"}</Text>
            </View>
          </View>

          {/* Fecha */}
          <View style={styles.infoRow}>
            <View style={styles.infoIconWrap}>
              <Ionicons name="calendar-outline" size={18} color={colors.accent} />
            </View>
            <View>
              <Text style={styles.infoLabel}>Fecha de solicitud</Text>
              <Text style={styles.infoValue}>{formatDate(applicationDate)}</Text>
            </View>
          </View>

          {/* Badge de estado */}
          <View style={styles.infoRow}>
            <View style={styles.infoIconWrap}>
              <Ionicons name="flag-outline" size={18} color={colors.accent} />
            </View>
            <View>
              <Text style={styles.infoLabel}>Estado</Text>
              <View style={{
                marginTop: 4,
                alignSelf: "flex-start",
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
        </View>

        {/* Sección PDF */}
        <View style={styles.pdfSection}>
          <Text style={styles.pdfSectionTitle}>Documento adjunto</Text>
          {renderPdfSection()}
        </View>
      </ScrollView>

      {/* ── Barra de acciones fija al fondo ── */}
      {renderActionBar()}
    </View>
  );
}
