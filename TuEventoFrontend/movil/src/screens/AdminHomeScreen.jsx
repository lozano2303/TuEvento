import { useCallback } from "react";
import { View, Text, ScrollView, TouchableOpacity, StatusBar } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useFocusEffect } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import { useAuth } from "../context/AuthContext";
import { useNotifications } from "../context/NotificationContext";
import { adminText } from "../utils/adminText";

// Secciones del panel (espejo del sidebar web)
const ADMIN_ITEMS = [
  { icon: "grid-outline",           key: "dashboard",    route: "AdminDashboard" },
  { icon: "card-outline",           key: "financial",    route: "AdminFinancial" },
  { icon: "calendar-outline",       key: "events",       route: "AdminEvents" },
  { icon: "bar-chart-outline",      key: "reports",      route: "AdminReports" },
  { icon: "refresh-circle-outline", key: "refunds",      route: "AdminRefunds" },
  { icon: "people-outline",         key: "requests",     route: "OrganizerRequests" },
  { icon: "person-circle-outline",  key: "users",        route: "AdminUsers" },
  { icon: "pricetags-outline",      key: "categories",   route: "AdminCategories" },
  { icon: "layers-outline",         key: "sectionTypes", route: "AdminSectionTypes" },
  { icon: "language-outline",       key: "translations", route: "AdminTranslations" },
];

/**
 * Inicio del administrador. Igual que en la web, el admin entra directo al panel y no usa
 * las pantallas de usuario (eventos, perfil, cartera); solo puede cerrar sesión.
 * AppNavigator lo devuelve aquí si intenta abrir una pantalla de usuario.
 */
export default function AdminHomeScreen() {
  const { colors } = useTheme();
  const { user, setShowLogoutModal } = useAuth();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const { adminPending, refreshAdminPending } = useNotifications();

  // Respaldo del WebSocket: al volver al panel se relee el estado real de los pendientes
  useFocusEffect(useCallback(() => { refreshAdminPending(); }, [refreshAdminPending]));

  const PENDING_BY_ROUTE = {
    AdminEvents:       adminPending.eventsInReview,
    OrganizerRequests: adminPending.organizerRequests,
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
        <View style={{
          width: 42, height: 42, borderRadius: 12,
          backgroundColor: colors.error + "22", borderWidth: 1, borderColor: colors.error + "40",
          alignItems: "center", justifyContent: "center",
        }}>
          <Ionicons name="shield-outline" size={22} color={colors.error} />
        </View>
        <View style={{ flex: 1 }}>
          <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "800" }}>
            {adminText("mobileProfile.adminPanel.label")}
          </Text>
          <Text style={{ color: colors.textMuted, fontSize: 12 }} numberOfLines={1}>
            {user?.fullName || user?.alias || adminText("mobileProfile.adminPanel.desc")}
          </Text>
        </View>
      </View>

      <ScrollView contentContainerStyle={{ padding: 20, paddingBottom: insets.bottom + 32 }}>
        <View style={{
          backgroundColor: colors.surface + "88",
          borderRadius: 14, borderWidth: 1, borderColor: colors.primary + "25",
          overflow: "hidden", marginBottom: 24,
        }}>
          {ADMIN_ITEMS.map((item, idx) => {
            const pending = PENDING_BY_ROUTE[item.route] ?? 0;
            return (
              <TouchableOpacity
                key={item.route}
                activeOpacity={0.75}
                onPress={() => navigation.navigate(item.route)}
                style={{
                  flexDirection: "row", alignItems: "center",
                  paddingHorizontal: 16, paddingVertical: 14,
                  borderBottomWidth: idx === ADMIN_ITEMS.length - 1 ? 0 : 1,
                  borderBottomColor: colors.primary + "18",
                }}
              >
                <View style={{
                  width: 38, height: 38, borderRadius: 10,
                  backgroundColor: colors.primary + "22",
                  alignItems: "center", justifyContent: "center", marginRight: 13,
                }}>
                  <Ionicons name={item.icon} size={19} color={colors.accent} />
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "600" }}>
                    {adminText(`mobileProfile.admin.${item.key}.label`)}
                  </Text>
                  <Text style={{ color: colors.textMuted, fontSize: 12, marginTop: 1 }}>
                    {adminText(`mobileProfile.admin.${item.key}.sub`)}
                  </Text>
                </View>
                {pending > 0 && (
                  <View style={{
                    minWidth: 20, height: 20, borderRadius: 10, paddingHorizontal: 6, marginRight: 8,
                    backgroundColor: colors.error, alignItems: "center", justifyContent: "center",
                  }}>
                    <Text style={{ color: "#FFFFFF", fontSize: 11, fontWeight: "700" }}>
                      {pending > 99 ? "99+" : pending}
                    </Text>
                  </View>
                )}
                <Ionicons name="chevron-forward" size={16} color={colors.textMuted} />
              </TouchableOpacity>
            );
          })}
        </View>

        {/* Cerrar sesión: única acción de cuenta del admin, igual que en la web */}
        <TouchableOpacity
          activeOpacity={0.8}
          onPress={() => setShowLogoutModal(true)}
          style={{
            flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 8,
            paddingVertical: 14, borderRadius: 14,
            backgroundColor: colors.error + "18", borderWidth: 1, borderColor: colors.error + "40",
          }}
        >
          <Ionicons name="log-out-outline" size={18} color={colors.error} />
          <Text style={{ color: colors.error, fontSize: 15, fontWeight: "700" }}>Cerrar sesión</Text>
        </TouchableOpacity>
      </ScrollView>
    </View>
  );
}
