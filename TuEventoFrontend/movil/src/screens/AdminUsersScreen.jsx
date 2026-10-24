import { useState, useEffect, useCallback } from "react";
import {
  View, Text, TextInput, FlatList, TouchableOpacity, ActivityIndicator,
  StatusBar, RefreshControl, Modal, ScrollView, KeyboardAvoidingView, Platform,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { adminText as t } from "../utils/adminText";
import { useTheme } from "../context/ThemeContext";
import { getAdminUsers, revokeOrganizerRole } from "../services/adminService";
import AppModal from "../components/AppModal";

const ROLE_FILTERS = ["", "ORGANIZER", "USER", "ADMIN"];
const REASON_MIN = 10;
const REASON_MAX = 500;

const ROLE_COLORS = { ADMIN: "#fbbf24", ORGANIZER: "#a78bfa", USER: "#9ca3af" };

/**
 * Gestión de usuarios (admin): búsqueda, filtro por rol y revocación del rol de
 * organizador con motivo (espejo de /admin-users en la web).
 */
export default function AdminUsersScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();

  const [users, setUsers]           = useState([]);
  const [loading, setLoading]       = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError]           = useState(null);
  const [query, setQuery]           = useState("");
  const [role, setRole]             = useState("");

  const [target, setTarget]         = useState(null);
  const [reason, setReason]         = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback]     = useState({ visible: false, type: "success", message: "" });

  const load = useCallback(async (q, r) => {
    setError(null);
    try {
      setUsers(await getAdminUsers({ q, role: r }));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  // Búsqueda con debounce para no pedir en cada tecla
  useEffect(() => {
    const id = setTimeout(() => { void load(query, role); }, 300);
    return () => clearTimeout(id);
  }, [query, role, load]);

  const onRefresh = () => { setRefreshing(true); void load(query, role); };

  const trimmed = reason.trim();
  const reasonValid = trimmed.length >= REASON_MIN && trimmed.length <= REASON_MAX;
  const displayName = (u) => u?.fullName || u?.alias || "";

  const confirmRevoke = async () => {
    if (!target || !reasonValid) return;
    setSubmitting(true);
    try {
      await revokeOrganizerRole(target.userId, trimmed);
      const name = displayName(target);
      setTarget(null);
      setFeedback({ visible: true, type: "success", message: t("adminUsers.revokeSuccess", { name }) });
      void load(query, role);
    } catch (e) {
      setFeedback({ visible: true, type: "error", message: e.message || t("adminUsers.revokeError") });
    } finally {
      setSubmitting(false);
    }
  };

  const fmtDate = (iso) => iso
    ? new Date(iso).toLocaleDateString("es-CO", { day: "2-digit", month: "short", year: "numeric" })
    : "—";

  const renderUser = ({ item: u }) => {
    const roleColor = ROLE_COLORS[u.role] ?? ROLE_COLORS.USER;
    return (
      <View style={{
        backgroundColor: colors.surface, borderRadius: 16, padding: 14, marginBottom: 10,
        borderWidth: 1, borderColor: colors.primary + "25", gap: 10,
      }}>
        <View style={{ flexDirection: "row", alignItems: "center", gap: 12 }}>
          <View style={{
            width: 40, height: 40, borderRadius: 20, backgroundColor: colors.primary + "30",
            alignItems: "center", justifyContent: "center",
          }}>
            <Text style={{ color: colors.textPrimary, fontWeight: "800" }}>
              {displayName(u).charAt(0).toUpperCase()}
            </Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700" }} numberOfLines={1}>
              {displayName(u)}
            </Text>
            <Text style={{ color: colors.textMuted, fontSize: 12 }} numberOfLines={1}>
              @{u.alias}{u.email ? ` · ${u.email}` : ""}
            </Text>
          </View>
          <View style={{
            paddingHorizontal: 8, paddingVertical: 3, borderRadius: 8,
            backgroundColor: roleColor + "22", borderWidth: 1, borderColor: roleColor + "55",
          }}>
            <Text style={{ color: roleColor, fontSize: 10, fontWeight: "800" }}>
              {t(`adminUsers.roles.${u.role}`, { defaultValue: u.role }).toUpperCase()}
            </Text>
          </View>
        </View>
        <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
          <Text style={{ color: colors.textSecondary, fontSize: 12 }}>
            {t(`adminUsers.statuses.${u.status}`, { defaultValue: u.status })} · {fmtDate(u.createdAt)}
          </Text>
          {u.role === "ORGANIZER" && (
            <TouchableOpacity
              onPress={() => { setTarget(u); setReason(""); }}
              activeOpacity={0.8}
              style={{
                flexDirection: "row", alignItems: "center", gap: 6,
                paddingHorizontal: 12, paddingVertical: 7, borderRadius: 10,
                backgroundColor: "#f8717122", borderWidth: 1, borderColor: "#f8717155",
              }}
            >
              <Ionicons name="person-remove-outline" size={14} color="#f87171" />
              <Text style={{ color: "#f87171", fontSize: 12, fontWeight: "700" }}>
                {t("adminUsers.revokeButton")}
              </Text>
            </TouchableOpacity>
          )}
        </View>
      </View>
    );
  };

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
          {t("adminUsers.title")}
        </Text>
      </View>

      {/* Buscador + filtros */}
      <View style={{ paddingHorizontal: 16, paddingTop: 14, gap: 10 }}>
        <View style={{
          flexDirection: "row", alignItems: "center", gap: 8,
          backgroundColor: colors.surface, borderRadius: 12, paddingHorizontal: 12,
          borderWidth: 1, borderColor: colors.primary + "30",
        }}>
          <Ionicons name="search" size={18} color={colors.textMuted} />
          <TextInput
            value={query}
            onChangeText={setQuery}
            placeholder={t("adminUsers.searchPlaceholder")}
            placeholderTextColor={colors.textMuted}
            autoCapitalize="none"
            autoCorrect={false}
            style={{ flex: 1, color: colors.textPrimary, paddingVertical: 11, fontSize: 14 }}
          />
          {query ? (
            <TouchableOpacity onPress={() => setQuery("")} hitSlop={10}>
              <Ionicons name="close-circle" size={18} color={colors.textMuted} />
            </TouchableOpacity>
          ) : null}
        </View>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8, paddingBottom: 4 }}>
          {ROLE_FILTERS.map((r) => {
            const active = role === r;
            return (
              <TouchableOpacity
                key={r || "all"}
                onPress={() => setRole(r)}
                activeOpacity={0.75}
                style={{
                  paddingHorizontal: 14, paddingVertical: 7, borderRadius: 20,
                  backgroundColor: active ? colors.primary : colors.surface,
                  borderWidth: 1, borderColor: active ? colors.primary : colors.primary + "30",
                }}
              >
                <Text style={{ color: active ? "#FFFFFF" : colors.textSecondary, fontSize: 12, fontWeight: "700" }}>
                  {t(`adminUsers.roles.${r || "ALL"}`)}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      </View>

      {loading ? (
        <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      ) : (
        <FlatList
          data={users}
          keyExtractor={(u) => String(u.userId)}
          renderItem={renderUser}
          contentContainerStyle={{ padding: 16, paddingBottom: insets.bottom + 24 }}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}
          ListEmptyComponent={
            <View style={{ alignItems: "center", paddingTop: 60, gap: 10 }}>
              <Ionicons name={error ? "alert-circle-outline" : "people-outline"} size={44} color={colors.textMuted} />
              <Text style={{ color: colors.textSecondary, fontSize: 14, textAlign: "center" }}>
                {error || t("adminUsers.empty")}
              </Text>
            </View>
          }
        />
      )}

      {/* Modal: quitar rol de organizador */}
      <Modal visible={!!target} transparent animationType="slide" onRequestClose={() => !submitting && setTarget(null)}>
        <KeyboardAvoidingView
          behavior={Platform.OS === "ios" ? "padding" : undefined}
          style={{ flex: 1, backgroundColor: "rgba(0,0,0,0.7)", justifyContent: "flex-end" }}
        >
          <View style={{
            backgroundColor: colors.background, borderTopLeftRadius: 24, borderTopRightRadius: 24,
            padding: 20, paddingBottom: insets.bottom + 20, gap: 12,
            borderWidth: 1, borderColor: colors.primary + "30",
          }}>
            <Text style={{ color: colors.textPrimary, fontSize: 17, fontWeight: "800" }}>
              {t("adminUsers.revokeTitle", { name: displayName(target) })}
            </Text>
            <Text style={{ color: colors.textSecondary, fontSize: 13, lineHeight: 19 }}>
              {t("adminUsers.revokeMessage")}
            </Text>
            <Text style={{ color: colors.textMuted, fontSize: 11, fontWeight: "700", textTransform: "uppercase" }}>
              {t("adminUsers.reasonLabel")}
            </Text>
            <TextInput
              value={reason}
              onChangeText={setReason}
              maxLength={REASON_MAX}
              multiline
              placeholder={t("adminUsers.reasonPlaceholder")}
              placeholderTextColor={colors.textMuted}
              style={{
                backgroundColor: colors.surface, borderRadius: 12, padding: 14, minHeight: 100,
                color: colors.textPrimary, fontSize: 14, textAlignVertical: "top",
                borderWidth: 1, borderColor: colors.primary + "30",
              }}
            />
            <View style={{ flexDirection: "row", justifyContent: "space-between" }}>
              <Text style={{ fontSize: 11, color: trimmed.length > 0 && trimmed.length < REASON_MIN ? "#f87171" : colors.textMuted }}>
                {t("adminUsers.reasonMin", { count: REASON_MIN })}
              </Text>
              <Text style={{ fontSize: 11, color: colors.textMuted }}>{reason.length}/{REASON_MAX}</Text>
            </View>
            <View style={{ flexDirection: "row", gap: 10 }}>
              <TouchableOpacity
                onPress={() => setTarget(null)}
                disabled={submitting}
                activeOpacity={0.8}
                style={{
                  flex: 1, paddingVertical: 14, borderRadius: 13, alignItems: "center",
                  borderWidth: 1, borderColor: colors.primary + "30",
                }}
              >
                <Text style={{ color: colors.textSecondary, fontWeight: "700" }}>{t("adminUsers.cancel")}</Text>
              </TouchableOpacity>
              <TouchableOpacity
                onPress={confirmRevoke}
                disabled={!reasonValid || submitting}
                activeOpacity={0.8}
                style={{
                  flex: 1, paddingVertical: 14, borderRadius: 13, alignItems: "center",
                  backgroundColor: "#f87171", opacity: !reasonValid ? 0.5 : 1,
                }}
              >
                {submitting
                  ? <ActivityIndicator size="small" color="#fff" />
                  : <Text style={{ color: "#fff", fontWeight: "700" }}>{t("adminUsers.revokeConfirm")}</Text>}
              </TouchableOpacity>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>

      <AppModal
        visible={feedback.visible}
        type={feedback.type}
        title={feedback.type === "success" ? t("adminUsers.title") : t("adminUsers.revokeError")}
        message={feedback.message}
        confirmText="OK"
        onConfirm={() => setFeedback((p) => ({ ...p, visible: false }))}
      />
    </View>
  );
}
