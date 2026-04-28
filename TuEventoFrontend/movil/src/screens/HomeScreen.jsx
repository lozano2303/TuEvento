import { View, Text, ScrollView, TouchableOpacity, StatusBar, Dimensions } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useAuth } from "../context/AuthContext";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";

const { width } = Dimensions.get("window");

export default function HomeScreen() {
  const navigation = useNavigation();
  const { user, setShowLogoutModal } = useAuth();
  const insets = useSafeAreaInsets();
  const { palette } = useTheme();

  const getDisplayName = (alias) => {
    if (!alias) return "Usuario";
    const parts = alias.trim().split(/\s+/);
    if (parts.length === 1) return parts[0];
    if (parts[0].length <= 3) return `${parts[0]} ${parts[1]}`;
    return parts[0];
  };
  const userName = getDisplayName(user?.fullName);

  const getRoleBadge = (role) => {
    switch (role) {
      case "ADMIN":
        return { label: "⚡ Admin", color: palette.error, bg: palette.error + "33" };
      case "ORGANIZER":
        return { label: "🎯 Organizador", color: "#3B82F6", bg: "#3B82F633" };
      default:
        return { label: "👤 Usuario", color: palette.accent, bg: palette.primary + "33" };
    }
  };
  const roleBadge = getRoleBadge(user?.role);
  const shouldShowLogoutText = `${userName} ${roleBadge.label}`.length <= 24;

  const quickActions = [
    { icon: "search-outline", label: "Buscar eventos", color: palette.primary },
    { icon: "ticket-outline", label: "Mis tickets", color: palette.success },
    { icon: "heart-outline", label: "Favoritos", color: "#DC2626" },
    { icon: "notifications-outline", label: "Notificaciones", color: "#D97706" },
  ];

  return (
    <View style={{ flex: 1, backgroundColor: palette.background }}>
      <StatusBar barStyle="light-content" backgroundColor={palette.background} />
      <ScrollView
        style={{ paddingTop: insets.top > 0 ? insets.top + 16 : 0 }}
        contentContainerStyle={{ paddingBottom: insets.bottom + 24 }}
      >
        {/* Hero */}
        <LinearGradient
          colors={[palette.surface, palette.background]}
          style={{ paddingTop: insets.top + 16, paddingHorizontal: 24, paddingBottom: 32 }}
        >
          <Text style={{ color: palette.accent, fontSize: 14, marginBottom: 4 }}>Bienvenido de vuelta 👋</Text>
          <View style={{ flexDirection: "row", alignItems: "flex-start", justifyContent: "space-between", gap: 12 }}>
            <View style={{ flex: 1 }}>
              <View style={{ flexDirection: "row", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
                <Text style={{ color: palette.textPrimary, fontSize: 26, fontWeight: "800" }}>
                  {userName}
                </Text>
                <View style={{
                  backgroundColor: roleBadge.bg,
                  borderRadius: 20, paddingHorizontal: 12, paddingVertical: 5,
                  borderWidth: 1, borderColor: roleBadge.color,
                }}>
                  <Text style={{ color: roleBadge.color, fontSize: 12, fontWeight: "800", letterSpacing: 0.5 }}>
                    {roleBadge.label}
                  </Text>
                </View>
              </View>
            </View>
            <TouchableOpacity
              onPress={() => setShowLogoutModal(true)}
              activeOpacity={0.7}
              style={{
                minWidth: 44,
                minHeight: 44,
                paddingHorizontal: shouldShowLogoutText ? 12 : 10,
                paddingVertical: 10,
                borderRadius: 22,
                backgroundColor: palette.surface,
                borderWidth: 1,
                borderColor: palette.surfaceAlt,
                flexDirection: "row",
                alignItems: "center",
                justifyContent: "center",
                gap: shouldShowLogoutText ? 6 : 0,
              }}
            >
              <Ionicons name="log-out-outline" size={20} color={palette.textSecondary} />
              {shouldShowLogoutText ? (
                <Text style={{ color: palette.textSecondary, fontSize: 13, fontWeight: "600" }}>
                  Salir
                </Text>
              ) : null}
            </TouchableOpacity>
          </View>
          <Text style={{ color: palette.textSecondary, fontSize: 14, marginTop: 6 }}>
            Descubre y vive experiencias únicas
          </Text>

          {/* Buscador decorativo */}
          <TouchableOpacity
            activeOpacity={0.85}
            style={{
              flexDirection: "row", alignItems: "center",
              backgroundColor: palette.surfaceAlt, borderRadius: 14,
              padding: 14, marginTop: 24, gap: 10,
            }}
          >
            <Ionicons name="search-outline" size={20} color={palette.textMuted} />
            <Text style={{ color: palette.textMuted, fontSize: 15 }}>Buscar eventos...</Text>
          </TouchableOpacity>
        </LinearGradient>

        {/* Accesos rápidos */}
        <View style={{ paddingHorizontal: 24, marginTop: 8 }}>
          <Text style={{ color: palette.textPrimary, fontSize: 18, fontWeight: "700", marginBottom: 16 }}>
            Accesos rápidos
          </Text>
          <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 12 }}>
            {quickActions.map((action, index) => (
              <TouchableOpacity
                key={index}
                activeOpacity={0.85}
                style={{
                  width: (width - 60) / 2,
                  backgroundColor: palette.surface, borderRadius: 16,
                  padding: 20, alignItems: "center",
                  borderWidth: 1, borderColor: palette.surfaceAlt,
                }}
              >
                <View style={{
                  width: 48, height: 48, borderRadius: 14,
                  backgroundColor: action.color + "22",
                  alignItems: "center", justifyContent: "center", marginBottom: 10,
                }}>
                  <Ionicons name={action.icon} size={24} color={action.color} />
                </View>
                <Text style={{ color: palette.textPrimary, fontSize: 13, fontWeight: "600", textAlign: "center" }}>
                  {action.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* Próximos eventos placeholder */}
        <View style={{ paddingHorizontal: 24, marginTop: 28 }}>
          <Text style={{ color: palette.textPrimary, fontSize: 18, fontWeight: "700", marginBottom: 16 }}>
            Próximos eventos
          </Text>
          <View style={{
            backgroundColor: palette.surface, borderRadius: 16,
            padding: 24, alignItems: "center",
            borderWidth: 1, borderColor: palette.surfaceAlt,
          }}>
            <Ionicons name="calendar-outline" size={40} color={palette.primary} />
            <Text style={{ color: palette.textPrimary, fontSize: 15, fontWeight: "600", marginTop: 12 }}>
              Próximamente
            </Text>
            <Text style={{ color: palette.textSecondary, fontSize: 13, marginTop: 6, textAlign: "center" }}>
              Los eventos aparecerán aquí cuando estén disponibles
            </Text>
          </View>
        </View>

        {/* Configuración */}
        <View style={{ paddingHorizontal: 24, marginTop: 28 }}>
          <Text style={{ color: palette.textPrimary, fontSize: 18, fontWeight: "700", marginBottom: 16 }}>
            Configuración
          </Text>
          <TouchableOpacity
            onPress={() => navigation.navigate("ThemeSelect")}
            activeOpacity={0.85}
            style={{
              backgroundColor: palette.surface,
              borderRadius: 16,
              padding: 16,
              flexDirection: "row",
              alignItems: "center",
              justifyContent: "space-between",
              borderWidth: 1,
              borderColor: palette.surfaceAlt,
            }}
          >
            <View style={{ flexDirection: "row", alignItems: "center", gap: 12 }}>
              <View style={{
                width: 44,
                height: 44,
                borderRadius: 12,
                backgroundColor: palette.primary + "22",
                alignItems: "center",
                justifyContent: "center",
              }}>
                <Ionicons name="color-palette-outline" size={22} color={palette.primary} />
              </View>
              <View>
                <Text style={{ color: palette.textPrimary, fontSize: 15, fontWeight: "600" }}>
                  Apariencia
                </Text>
                <Text style={{ color: palette.textSecondary, fontSize: 12, marginTop: 2 }}>
                  Personaliza el tema de la app
                </Text>
              </View>
            </View>
            <Ionicons name="chevron-forward" size={20} color={palette.textMuted} />
          </TouchableOpacity>
        </View>
      </ScrollView>
    </View>
  );
}
