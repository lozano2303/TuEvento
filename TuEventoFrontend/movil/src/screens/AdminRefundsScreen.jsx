import { useState, useEffect, useCallback, useMemo } from "react";
import {
  View, Text, TextInput, FlatList, TouchableOpacity, ActivityIndicator,
  StatusBar, RefreshControl, Modal, ScrollView, KeyboardAvoidingView, Platform,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { adminText as t } from "../utils/adminText";
import { useTheme } from "../context/ThemeContext";
import { getAdminRefunds, getAdminStats, getAdminUsers, adminCreditWallet } from "../services/adminService";
import AppModal from "../components/AppModal";

const STATUS_FILTERS = ["", "PROCESSED", "REQUESTED", "FAILED"];
const STATUS_COLORS = { PROCESSED: "#34d399", REQUESTED: "#fbbf24", FAILED: "#f87171" };
const REASON_MIN = 5;
const REASON_MAX = 255;

const fmtCOP = (n) =>
  new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 })
    .format(Number(n) || 0);

/**
 * Panel de reembolsos (admin): resumen, listado con desglose pasarela/cartera y
 * acreditación manual de saldo con buscador de usuario (espejo de /admin-refunds).
 */
export default function AdminRefundsScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();

  const [refunds, setRefunds]       = useState([]);
  const [stats, setStats]           = useState(null);
  const [loading, setLoading]       = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError]           = useState(null);
  const [status, setStatus]         = useState("");
  const [query, setQuery]           = useState("");

  // Modal "Agregar crédito"
  const [creditOpen, setCreditOpen]   = useState(false);
  const [creditUser, setCreditUser]   = useState(null);
  const [userQuery, setUserQuery]     = useState("");
  const [userOptions, setUserOptions] = useState([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [amount, setAmount]           = useState("");
  const [reason, setReason]           = useState("");
  const [submitting, setSubmitting]   = useState(false);
  const [feedback, setFeedback]       = useState({ visible: false, type: "success", message: "" });

  const load = useCallback(async () => {
    setError(null);
    try {
      const [list, s] = await Promise.all([getAdminRefunds(), getAdminStats()]);
      setRefunds(list);
      setStats(s);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);
  const onRefresh = () => { setRefreshing(true); void load(); };

  // Buscador de usuarios del modal (debounce)
  useEffect(() => {
    if (!creditOpen || creditUser) return;
    setUsersLoading(true);
    const id = setTimeout(async () => {
      try {
        setUserOptions(await getAdminUsers({ q: userQuery, limit: 20 }));
      } catch {
        setUserOptions([]);
      } finally {
        setUsersLoading(false);
      }
    }, 250);
    return () => clearTimeout(id);
  }, [userQuery, creditOpen, creditUser]);

  const visible = useMemo(() => {
    const term = query.trim().toLowerCase();
    return refunds.filter((r) => {
      if (status && r.status !== status) return false;
      if (!term) return true;
      return [r.fullName, r.alias, r.email, r.eventName].filter(Boolean).join(" ").toLowerCase().includes(term);
    });
  }, [refunds, status, query]);

  const fmtDate = (iso) => iso
    ? new Date(iso).toLocaleString("es-CO", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" })
    : "—";

  const openCredit = () => {
    setCreditUser(null);
    setUserQuery("");
    setAmount("");
    setReason("");
    setCreditOpen(true);
  };

  const amountNumber = Number(amount.replace(/[^\d]/g, ""));
  const trimmedReason = reason.trim();
  const creditValid = !!creditUser && amountNumber > 0
    && trimmedReason.length >= REASON_MIN && trimmedReason.length <= REASON_MAX;

  const submitCredit = async () => {
    if (!creditValid) return;
    setSubmitting(true);
    try {
      await adminCreditWallet({ userId: creditUser.userId, amount: amountNumber, reason: trimmedReason });
      setCreditOpen(false);
      setFeedback({
        visible: true, type: "success",
        message: t("adminRefunds.credit.success", { amount: fmtCOP(amountNumber), name: creditUser.fullName || creditUser.alias }),
      });
    } catch (e) {
      setFeedback({ visible: true, type: "error", message: e.message || t("adminRefunds.credit.error") });
    } finally {
      setSubmitting(false);
    }
  };

  const STAT_CARDS = [
    { icon: "time-outline",             color: "#fbbf24", label: t("adminRefunds.stats.pending"),   value: String(stats?.pendingRefunds ?? 0) },
    { icon: "checkmark-circle-outline", color: "#34d399", label: t("adminRefunds.stats.processed"), value: String(stats?.processedRefunds ?? 0) },
    { icon: "swap-horizontal-outline",  color: "#f87171", label: t("adminRefunds.stats.total"),     value: fmtCOP(stats?.totalRefundedAmount) },
  ];

  const renderHeader = () => (
    <View style={{ gap: 12, marginBottom: 6 }}>
      {/* Resumen */}
      <View style={{ flexDirection: "row", gap: 8 }}>
        {STAT_CARDS.map((c) => (
          <View key={c.label} style={{
            flex: 1, backgroundColor: colors.surface, borderRadius: 14, padding: 12, gap: 6,
            borderWidth: 1, borderColor: colors.primary + "25",
          }}>
            <Ionicons name={c.icon} size={18} color={c.color} />
            <Text style={{ color: colors.textMuted, fontSize: 10, fontWeight: "700", textTransform: "uppercase" }} numberOfLines={1}>
              {c.label}
            </Text>
            <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "800" }} numberOfLines={1} adjustsFontSizeToFit>
              {c.value}
            </Text>
          </View>
        ))}
      </View>

      {/* Agregar crédito */}
      <TouchableOpacity
        onPress={openCredit}
        activeOpacity={0.85}
        style={{
          flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 8,
          backgroundColor: colors.primary, borderRadius: 13, paddingVertical: 13,
        }}
      >
        <Ionicons name="add-circle-outline" size={18} color="#fff" />
        <Text style={{ color: "#fff", fontWeight: "800" }}>{t("adminRefunds.credit.open")}</Text>
      </TouchableOpacity>

      {/* Buscador + estados */}
      <View style={{
        flexDirection: "row", alignItems: "center", gap: 8,
        backgroundColor: colors.surface, borderRadius: 12, paddingHorizontal: 12,
        borderWidth: 1, borderColor: colors.primary + "30",
      }}>
        <Ionicons name="search" size={18} color={colors.textMuted} />
        <TextInput
          value={query}
          onChangeText={setQuery}
          placeholder={t("adminRefunds.searchPlaceholder")}
          placeholderTextColor={colors.textMuted}
          autoCorrect={false}
          style={{ flex: 1, color: colors.textPrimary, paddingVertical: 11, fontSize: 14 }}
        />
      </View>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8 }}>
        {STATUS_FILTERS.map((s) => {
          const active = status === s;
          return (
            <TouchableOpacity
              key={s || "all"}
              onPress={() => setStatus(s)}
              activeOpacity={0.75}
              style={{
                paddingHorizontal: 14, paddingVertical: 7, borderRadius: 20,
                backgroundColor: active ? colors.primary : colors.surface,
                borderWidth: 1, borderColor: active ? colors.primary : colors.primary + "30",
              }}
            >
              <Text style={{ color: active ? "#FFFFFF" : colors.textSecondary, fontSize: 12, fontWeight: "700" }}>
                {t(`adminRefunds.status.${s || "ALL"}`)}
              </Text>
            </TouchableOpacity>
          );
        })}
      </ScrollView>
    </View>
  );

  const renderRefund = ({ item: r }) => {
    const gateway = Number(r.gatewayAmount || 0);
    const wallet = Number(r.walletAmount || 0);
    const color = STATUS_COLORS[r.status] ?? STATUS_COLORS.REQUESTED;
    return (
      <View style={{
        backgroundColor: colors.surface, borderRadius: 16, padding: 14, marginBottom: 10, gap: 8,
        borderWidth: 1, borderColor: colors.primary + "25",
      }}>
        <View style={{ flexDirection: "row", alignItems: "flex-start", gap: 10 }}>
          <View style={{ flex: 1 }}>
            <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700" }} numberOfLines={1}>
              {r.fullName || r.alias || `#${r.userId}`}
            </Text>
            <Text style={{ color: colors.textMuted, fontSize: 12 }} numberOfLines={1}>
              {r.eventName || `#${r.eventId}`} · {t("adminRefunds.order", { id: r.orderId })}
            </Text>
          </View>
          <View style={{
            paddingHorizontal: 8, paddingVertical: 3, borderRadius: 8,
            backgroundColor: color + "22", borderWidth: 1, borderColor: color + "55",
          }}>
            <Text style={{ color, fontSize: 10, fontWeight: "800" }}>
              {t(`adminRefunds.status.${r.status}`, { defaultValue: r.status }).toUpperCase()}
            </Text>
          </View>
        </View>
        <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "flex-end" }}>
          <View>
            <Text style={{ color: colors.textPrimary, fontSize: 17, fontWeight: "800" }}>{fmtCOP(gateway + wallet)}</Text>
            {gateway > 0 && <Text style={{ color: colors.textMuted, fontSize: 11 }}>{t("adminRefunds.gatewayPart", { amount: fmtCOP(gateway) })}</Text>}
            {wallet > 0 && <Text style={{ color: colors.textMuted, fontSize: 11 }}>{t("adminRefunds.walletPart", { amount: fmtCOP(wallet) })}</Text>}
          </View>
          <Text style={{ color: colors.textSecondary, fontSize: 11 }}>{fmtDate(r.requestedAt)}</Text>
        </View>
        {r.reason ? (
          <Text style={{ color: colors.textSecondary, fontSize: 12, lineHeight: 17 }}>{r.reason}</Text>
        ) : null}
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
          {t("adminRefunds.title")}
        </Text>
      </View>

      {loading ? (
        <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      ) : (
        <FlatList
          data={visible}
          keyExtractor={(r) => String(r.refundId)}
          renderItem={renderRefund}
          ListHeaderComponent={renderHeader()}
          contentContainerStyle={{ padding: 16, paddingBottom: insets.bottom + 24 }}
          keyboardShouldPersistTaps="handled"
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}
          ListEmptyComponent={
            <View style={{ alignItems: "center", paddingTop: 40, gap: 10 }}>
              <Ionicons name={error ? "alert-circle-outline" : "swap-horizontal-outline"} size={44} color={colors.textMuted} />
              <Text style={{ color: colors.textSecondary, fontSize: 14, textAlign: "center" }}>
                {error || t("adminRefunds.empty")}
              </Text>
            </View>
          }
        />
      )}

      {/* Modal: agregar crédito */}
      <Modal visible={creditOpen} transparent animationType="slide" onRequestClose={() => !submitting && setCreditOpen(false)}>
        <KeyboardAvoidingView
          behavior={Platform.OS === "ios" ? "padding" : undefined}
          style={{ flex: 1, backgroundColor: "rgba(0,0,0,0.7)", justifyContent: "flex-end" }}
        >
          <View style={{
            backgroundColor: colors.background, borderTopLeftRadius: 24, borderTopRightRadius: 24,
            padding: 20, paddingBottom: insets.bottom + 20, gap: 10, maxHeight: "90%",
            borderWidth: 1, borderColor: colors.primary + "30",
          }}>
            <Text style={{ color: colors.textPrimary, fontSize: 17, fontWeight: "800" }}>{t("adminRefunds.credit.title")}</Text>
            <Text style={{ color: colors.textSecondary, fontSize: 13, lineHeight: 19 }}>{t("adminRefunds.credit.subtitle")}</Text>

            {/* Usuario */}
            <Text style={{ color: colors.textMuted, fontSize: 11, fontWeight: "700", textTransform: "uppercase" }}>
              {t("adminRefunds.credit.user")}
            </Text>
            {creditUser ? (
              <View style={{
                flexDirection: "row", alignItems: "center", gap: 10, padding: 12, borderRadius: 12,
                backgroundColor: colors.primary + "18", borderWidth: 1, borderColor: colors.primary + "50",
              }}>
                <View style={{ flex: 1 }}>
                  <Text style={{ color: colors.textPrimary, fontWeight: "700" }} numberOfLines={1}>
                    {creditUser.fullName || creditUser.alias}
                  </Text>
                  <Text style={{ color: colors.textMuted, fontSize: 12 }} numberOfLines={1}>
                    @{creditUser.alias}{creditUser.email ? ` · ${creditUser.email}` : ""}
                  </Text>
                </View>
                <TouchableOpacity onPress={() => setCreditUser(null)} hitSlop={10}>
                  <Ionicons name="close-circle" size={20} color={colors.textMuted} />
                </TouchableOpacity>
              </View>
            ) : (
              <View style={{ gap: 6 }}>
                <View style={{
                  flexDirection: "row", alignItems: "center", gap: 8,
                  backgroundColor: colors.surface, borderRadius: 12, paddingHorizontal: 12,
                  borderWidth: 1, borderColor: colors.primary + "30",
                }}>
                  <Ionicons name="search" size={16} color={colors.textMuted} />
                  <TextInput
                    value={userQuery}
                    onChangeText={setUserQuery}
                    placeholder={t("adminRefunds.credit.userSearch")}
                    placeholderTextColor={colors.textMuted}
                    autoCapitalize="none"
                    autoCorrect={false}
                    style={{ flex: 1, color: colors.textPrimary, paddingVertical: 10, fontSize: 14 }}
                  />
                </View>
                <ScrollView style={{ maxHeight: 170 }} keyboardShouldPersistTaps="handled" nestedScrollEnabled>
                  {usersLoading ? (
                    <ActivityIndicator size="small" color={colors.primary} style={{ marginVertical: 10 }} />
                  ) : userOptions.length === 0 ? (
                    <Text style={{ color: colors.textMuted, fontSize: 12, paddingVertical: 8 }}>{t("adminUsers.empty")}</Text>
                  ) : userOptions.map((u) => (
                    <TouchableOpacity
                      key={u.userId}
                      onPress={() => setCreditUser(u)}
                      activeOpacity={0.75}
                      style={{ paddingVertical: 9, borderBottomWidth: 1, borderBottomColor: colors.primary + "15" }}
                    >
                      <Text style={{ color: colors.textPrimary, fontWeight: "600" }} numberOfLines={1}>{u.fullName || u.alias}</Text>
                      <Text style={{ color: colors.textMuted, fontSize: 12 }} numberOfLines={1}>
                        @{u.alias}{u.email ? ` · ${u.email}` : ""}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>
              </View>
            )}

            {/* Monto */}
            <Text style={{ color: colors.textMuted, fontSize: 11, fontWeight: "700", textTransform: "uppercase" }}>
              {t("adminRefunds.credit.amount")}
            </Text>
            <TextInput
              value={amount}
              onChangeText={setAmount}
              keyboardType="number-pad"
              placeholder="50000"
              placeholderTextColor={colors.textMuted}
              style={{
                backgroundColor: colors.surface, borderRadius: 12, paddingHorizontal: 14, paddingVertical: 11,
                color: colors.textPrimary, fontSize: 15, borderWidth: 1, borderColor: colors.primary + "30",
              }}
            />
            {amountNumber > 0 && <Text style={{ color: colors.textMuted, fontSize: 12 }}>{fmtCOP(amountNumber)}</Text>}

            {/* Motivo */}
            <Text style={{ color: colors.textMuted, fontSize: 11, fontWeight: "700", textTransform: "uppercase" }}>
              {t("adminRefunds.credit.reason")}
            </Text>
            <TextInput
              value={reason}
              onChangeText={setReason}
              maxLength={REASON_MAX}
              multiline
              placeholder={t("adminRefunds.credit.reasonPlaceholder")}
              placeholderTextColor={colors.textMuted}
              style={{
                backgroundColor: colors.surface, borderRadius: 12, padding: 14, minHeight: 80,
                color: colors.textPrimary, fontSize: 14, textAlignVertical: "top",
                borderWidth: 1, borderColor: colors.primary + "30",
              }}
            />

            <View style={{ flexDirection: "row", gap: 10, marginTop: 4 }}>
              <TouchableOpacity
                onPress={() => setCreditOpen(false)}
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
                onPress={submitCredit}
                disabled={!creditValid || submitting}
                activeOpacity={0.8}
                style={{
                  flex: 1, paddingVertical: 14, borderRadius: 13, alignItems: "center",
                  backgroundColor: colors.primary, opacity: !creditValid ? 0.5 : 1,
                }}
              >
                {submitting
                  ? <ActivityIndicator size="small" color="#fff" />
                  : <Text style={{ color: "#fff", fontWeight: "700" }}>{t("adminRefunds.credit.submit")}</Text>}
              </TouchableOpacity>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>

      <AppModal
        visible={feedback.visible}
        type={feedback.type}
        title={feedback.type === "success" ? t("adminRefunds.title") : t("adminRefunds.credit.error")}
        message={feedback.message}
        confirmText="OK"
        onConfirm={() => setFeedback((p) => ({ ...p, visible: false }))}
      />
    </View>
  );
}
