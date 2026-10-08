import { useState } from "react";
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  StatusBar,
  StyleSheet,
  Modal,
  RefreshControl,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import { useNotifications } from "../context/NotificationContext";

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatRelativeTime(isoStr) {
  if (!isoStr) return "";
  const diff = Date.now() - new Date(isoStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1)  return "hace un momento";
  if (mins < 60) return `hace ${mins} min`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24)  return `hace ${hrs} h`;
  const days = Math.floor(hrs / 24);
  if (days < 7)  return `hace ${days} d`;
  return new Date(isoStr).toLocaleDateString("es-CO", {
    day: "2-digit", month: "short", year: "numeric",
  });
}

function formatFullDate(isoStr) {
  if (!isoStr) return "";
  return new Date(isoStr).toLocaleString("es-CO", {
    day: "2-digit", month: "long", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  });
}

function stripHtml(html) {
  if (!html) return "";
  return html.replace(/<[^>]*>/g, "");
}

// ─── Mapa de iconos por tipo ──────────────────────────────────────────────────

const TYPE_CONFIG = {
  PAYMENT_APPROVED: { icon: "checkmark-circle", color: "#34d399" }, // success
  PAYMENT_REFUNDED: { icon: "refresh-circle",   color: "#fbbf24" }, // warning
  WALLET_CREDITED:  { icon: "wallet",            color: "#60a5fa" }, // info
  WELCOME:          { icon: "gift",              color: "#a78bfa" }, // accent
  EVENT_PUBLISHED:  { icon: "rocket",            color: "#34d399" },
  EVENT_REJECTED:   { icon: "ban",               color: "#f87171" },
  DEFAULT:          { icon: "information-circle",color: "#a78bfa" },
};

function getTypeConfig(type) {
  return TYPE_CONFIG[type] ?? TYPE_CONFIG.DEFAULT;
}

// ─── Modal de detalle ─────────────────────────────────────────────────────────

function NotificationDetailModal({ notification, onClose, colors }) {
  const { icon, color } = getTypeConfig(notification.type);
  const body = stripHtml(notification.body);
  const isUnread = !notification.readAt;

  const tips = {
    PAYMENT_APPROVED: "💡 Podés ver tus tickets en tu perfil.",
    PAYMENT_REFUNDED: "💡 Los fondos estarán disponibles según el método de pago original.",
    WALLET_CREDITED:  "💡 El saldo está disponible inmediatamente en tu cartera.",
    WELCOME:          "💡 Explorá eventos y comprá tickets directamente desde la app.",
  };

  return (
    <Modal visible transparent animationType="fade" onRequestClose={onClose}>
      <View style={{
        flex: 1, backgroundColor: "rgba(0,0,0,0.7)",
        justifyContent: "center", alignItems: "center", padding: 20,
      }}>
        <View style={{
          backgroundColor: colors.background, borderRadius: 20, width: "100%",
          maxHeight: "85%", overflow: "hidden",
          borderWidth: 2, borderColor: colors.primary + "33",
        }}>
          {/* Header */}
          <View style={{
            flexDirection: "row", alignItems: "flex-start", gap: 12,
            padding: 20, borderBottomWidth: 1, borderBottomColor: colors.primary + "20",
          }}>
            <View style={{
              width: 48, height: 48, borderRadius: 14,
              backgroundColor: colors.surface,
              alignItems: "center", justifyContent: "center",
            }}>
              <Ionicons name={icon} size={24} color={color} />
            </View>
            <View style={{ flex: 1 }}>
              <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
                <Text style={{
                  color: colors.textPrimary, fontSize: 16,
                  fontWeight: "700", flex: 1,
                }}>
                  {notification.subject}
                </Text>
                {isUnread && (
                  <View style={{
                    width: 8, height: 8, borderRadius: 4,
                    backgroundColor: colors.accent,
                  }} />
                )}
              </View>
              <Text style={{ color: colors.textMuted, fontSize: 11, marginTop: 4 }}>
                {formatFullDate(notification.sentAt)}
              </Text>
            </View>
            <TouchableOpacity
              onPress={onClose}
              style={{
                width: 32, height: 32, borderRadius: 10,
                backgroundColor: colors.surface,
                alignItems: "center", justifyContent: "center",
              }}
            >
              <Ionicons name="close" size={18} color={colors.textSecondary} />
            </TouchableOpacity>
          </View>

          {/* Contenido */}
          <ScrollView
            style={{ maxHeight: 380 }}
            contentContainerStyle={{ padding: 20, gap: 12 }}
            showsVerticalScrollIndicator={false}
          >
            <Text style={{
              color: colors.textSecondary, fontSize: 14, lineHeight: 22,
            }}>
              {body}
            </Text>

            {/* Tip según tipo */}
            {tips[notification.type] && (
              <View style={{
                backgroundColor: colors.surface, borderRadius: 12,
                padding: 12, borderWidth: 1,
                borderColor: colors.primary + "20",
              }}>
                <Text style={{ color: colors.textMuted, fontSize: 12 }}>
                  {tips[notification.type]}
                </Text>
              </View>
            )}

            {/* Metadata */}
            <View style={{
              borderTopWidth: 1, borderTopColor: colors.primary + "15",
              paddingTop: 12, flexDirection: "row", justifyContent: "space-between",
            }}>
              <Text style={{ color: colors.textMuted, fontSize: 11 }}>
                ID: #{notification.notificationUserId}
              </Text>
              {notification.readAt && (
                <Text style={{ color: colors.textMuted, fontSize: 11 }}>
                  Leída: {formatFullDate(notification.readAt)}
                </Text>
              )}
            </View>
          </ScrollView>

          {/* Footer */}
          <View style={{
            padding: 16, borderTopWidth: 1, borderTopColor: colors.primary + "20",
          }}>
            <TouchableOpacity
              onPress={onClose}
              activeOpacity={0.8}
              style={{
                backgroundColor: colors.accent, borderRadius: 12,
                paddingVertical: 13, alignItems: "center",
              }}
            >
              <Text style={{ color: "#fff", fontWeight: "700", fontSize: 15 }}>
                Cerrar
              </Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
}

// ─── Card de notificación ─────────────────────────────────────────────────────

function NotificationCard({ notification, onMarkAsRead, colors }) {
  const [showModal, setShowModal] = useState(false);
  const { icon, color } = getTypeConfig(notification.type);
  const isUnread = !notification.readAt;
  const body = stripHtml(notification.body);

  const handlePress = () => {
    if (isUnread) onMarkAsRead(notification.notificationUserId);
    setShowModal(true);
  };

  return (
    <>
      <TouchableOpacity
        onPress={handlePress}
        activeOpacity={0.75}
        style={{
          flexDirection: "row", alignItems: "flex-start", gap: 12,
          backgroundColor: isUnread ? colors.primary + "0D" : colors.surface + "CC",
          borderRadius: 14, borderWidth: 1,
          borderColor: isUnread ? colors.primary + "50" : colors.primary + "20",
          borderLeftWidth: isUnread ? 4 : 1,
          borderLeftColor: isUnread ? colors.primary : colors.primary + "20",
          padding: 14,
        }}
      >
        {/* Ícono */}
        <View style={{
          width: 44, height: 44, borderRadius: 12,
          backgroundColor: isUnread ? colors.primary + "20" : colors.surface,
          alignItems: "center", justifyContent: "center",
        }}>
          <Ionicons name={icon} size={22} color={color} />
        </View>

        {/* Contenido */}
        <View style={{ flex: 1 }}>
          <View style={{ flexDirection: "row", alignItems: "flex-start", gap: 6 }}>
            <Text style={{
              color: isUnread ? colors.textPrimary : colors.textSecondary,
              fontSize: 14, fontWeight: isUnread ? "700" : "600",
              flex: 1, lineHeight: 20,
            }}>
              {notification.subject}
            </Text>
            {isUnread && (
              <View style={{
                width: 9, height: 9, borderRadius: 5,
                backgroundColor: colors.accent, marginTop: 5,
              }} />
            )}
          </View>

          <Text
            numberOfLines={2}
            style={{ color: colors.textMuted, fontSize: 12, lineHeight: 18, marginTop: 3 }}
          >
            {body}
          </Text>

          <View style={{ flexDirection: "row", alignItems: "center", gap: 8, marginTop: 6 }}>
            <Text style={{ color: colors.textMuted, fontSize: 11 }}>
              {formatRelativeTime(notification.sentAt)}
            </Text>
            <Text style={{ color: colors.textMuted, fontSize: 11 }}>•</Text>
            <Text style={{ color: colors.textMuted, fontSize: 11, textTransform: "capitalize" }}>
              {notification.type?.replace(/_/g, " ").toLowerCase()}
            </Text>
          </View>
        </View>
      </TouchableOpacity>

      {showModal && (
        <NotificationDetailModal
          notification={notification}
          onClose={() => setShowModal(false)}
          colors={colors}
        />
      )}
    </>
  );
}

// ─── Pantalla principal ───────────────────────────────────────────────────────

export default function NotificationsScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const { notifications, unreadCount, loading, markAsRead, markAllAsRead, refresh } =
    useNotifications();

  const [filter, setFilter] = useState("all"); // 'all' | 'unread'
  const [refreshing, setRefreshing] = useState(false);

  const onRefresh = async () => {
    setRefreshing(true);
    await refresh();
    setRefreshing(false);
  };

  const filtered = filter === "unread"
    ? notifications.filter((n) => !n.readAt)
    : notifications;

  const styles = StyleSheet.create({
    root: { flex: 1, backgroundColor: colors.background },
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
    headerTitle: {
      color: colors.textPrimary, fontSize: 18, fontWeight: "800", flex: 1,
    },
    badgeWrap: {
      backgroundColor: colors.error, borderRadius: 10,
      minWidth: 20, height: 20,
      alignItems: "center", justifyContent: "center",
      paddingHorizontal: 5,
    },
    badgeText: { color: "#fff", fontSize: 11, fontWeight: "800" },
    filtersRow: {
      flexDirection: "row", gap: 10,
      paddingHorizontal: 20, paddingVertical: 14,
    },
    filterBtn: {
      flex: 1, borderRadius: 10, paddingVertical: 9,
      alignItems: "center", borderWidth: 1,
    },
    filterBtnText: { fontSize: 13, fontWeight: "700" },
    markAllBtn: {
      marginHorizontal: 20, marginBottom: 8,
      backgroundColor: colors.accent, borderRadius: 12,
      paddingVertical: 11, alignItems: "center",
    },
    markAllText: { color: "#fff", fontWeight: "700", fontSize: 13 },
    list: { flex: 1 },
    listContent: {
      paddingHorizontal: 20, paddingTop: 4,
      paddingBottom: insets.bottom + 32, gap: 10,
    },
    emptyWrap: {
      alignItems: "center", justifyContent: "center",
      paddingVertical: 60, paddingHorizontal: 32,
    },
    emptyTitle: {
      color: colors.textPrimary, fontSize: 17, fontWeight: "700",
      textAlign: "center", marginTop: 16, marginBottom: 8,
    },
    emptyText: {
      color: colors.textSecondary, fontSize: 13,
      textAlign: "center", lineHeight: 20,
    },
  });

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
        <Text style={styles.headerTitle}>Notificaciones</Text>
        {unreadCount > 0 && (
          <View style={styles.badgeWrap}>
            <Text style={styles.badgeText}>
              {unreadCount > 99 ? "99+" : unreadCount}
            </Text>
          </View>
        )}
      </View>

      {/* Filtros */}
      <View style={styles.filtersRow}>
        <TouchableOpacity
          onPress={() => setFilter("all")}
          activeOpacity={0.8}
          style={[
            styles.filterBtn,
            {
              backgroundColor: filter === "all" ? colors.primary : colors.primary + "15",
              borderColor: filter === "all" ? colors.primary : colors.primary + "30",
            },
          ]}
        >
          <Text style={[
            styles.filterBtnText,
            { color: filter === "all" ? colors.textPrimary : colors.textSecondary },
          ]}>
            Todas ({notifications.length})
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          onPress={() => setFilter("unread")}
          activeOpacity={0.8}
          style={[
            styles.filterBtn,
            {
              backgroundColor: filter === "unread" ? colors.primary : colors.primary + "15",
              borderColor: filter === "unread" ? colors.primary : colors.primary + "30",
            },
          ]}
        >
          <Text style={[
            styles.filterBtnText,
            { color: filter === "unread" ? colors.textPrimary : colors.textSecondary },
          ]}>
            No leídas ({unreadCount})
          </Text>
        </TouchableOpacity>
      </View>

      {/* Marcar todas */}
      {unreadCount > 0 && (
        <TouchableOpacity
          onPress={markAllAsRead}
          activeOpacity={0.8}
          style={styles.markAllBtn}
        >
          <Text style={styles.markAllText}>Marcar todas como leídas</Text>
        </TouchableOpacity>
      )}

      {/* Lista */}
      {loading ? (
        <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      ) : (
        <ScrollView
          style={styles.list}
          contentContainerStyle={styles.listContent}
          showsVerticalScrollIndicator={false}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={onRefresh}
              tintColor={colors.primary}
            />
          }
        >
          {filtered.length === 0 ? (
            <View style={styles.emptyWrap}>
              <Ionicons name="notifications-off-outline" size={56} color={colors.textMuted} />
              <Text style={styles.emptyTitle}>
                {filter === "unread"
                  ? "Todo al día"
                  : "Sin notificaciones"}
              </Text>
              <Text style={styles.emptyText}>
                {filter === "unread"
                  ? "No tenés notificaciones sin leer."
                  : "Las notificaciones aparecerán aquí cuando las recibas."}
              </Text>
            </View>
          ) : (
            filtered.map((n) => (
              <NotificationCard
                key={n.notificationUserId}
                notification={n}
                onMarkAsRead={markAsRead}
                colors={colors}
              />
            ))
          )}
        </ScrollView>
      )}
    </View>
  );
}
