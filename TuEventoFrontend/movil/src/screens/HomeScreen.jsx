import { View, Text, ScrollView, TouchableOpacity, StatusBar, Dimensions } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useAuth } from "../context/AuthContext";

const { width } = Dimensions.get("window");

export default function HomeScreen() {
  const navigation = useNavigation();

  const { user, logout } = useAuth();

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
        return { label: "⚡ Admin", color: "#EF4444", bg: "#EF444433" };
      case "ORGANIZER":
        return { label: "🎯 Organizador", color: "#3B82F6", bg: "#3B82F633" };
      default:
        return { label: "👤 Usuario", color: "#A78BFA", bg: "#7C3AED33" };
    }
  };
  const roleBadge = getRoleBadge(user?.role);

  const quickActions = [
    { icon: "search-outline", label: "Buscar eventos", color: "#7C3AED" },
    { icon: "ticket-outline", label: "Mis tickets", color: "#059669" },
    { icon: "heart-outline", label: "Favoritos", color: "#DC2626" },
    { icon: "notifications-outline", label: "Notificaciones", color: "#D97706" },
  ];

  return (
    <View style={{ flex: 1, backgroundColor: "#1E0A3C" }}>
      <StatusBar barStyle="light-content" backgroundColor="#1E0A3C" />
      <ScrollView contentContainerStyle={{ paddingBottom: 100 }}>
        {/* Hero */}
        <LinearGradient
          colors={["#2D1B4E", "#1E0A3C"]}
          style={{ paddingTop: 60, paddingHorizontal: 24, paddingBottom: 32 }}
        >
          <Text style={{ color: "#A78BFA", fontSize: 14, marginBottom: 4 }}>Bienvenido de vuelta 👋</Text>
          <View style={{ flexDirection: "row", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
            <Text style={{ color: "#FFFFFF", fontSize: 26, fontWeight: "800" }}>
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
          <Text style={{ color: "#9CA3AF", fontSize: 14, marginTop: 6 }}>
            Descubre y vive experiencias únicas
          </Text>

          <TouchableOpacity
            onPress={async () => {
              await logout();
              navigation.replace("Login");
            }}
            style={{
              flexDirection: "row", alignItems: "center", gap: 6,
              alignSelf: "flex-start", marginTop: 16,
              backgroundColor: "#EF444422", borderRadius: 20,
              paddingHorizontal: 14, paddingVertical: 7,
              borderWidth: 1, borderColor: "#EF4444",
            }}
          >
            <Ionicons name="log-out-outline" size={16} color="#EF4444" />
            <Text style={{ color: "#EF4444", fontSize: 13, fontWeight: "700" }}>
              Cerrar sesión
            </Text>
          </TouchableOpacity>

          {/* Buscador decorativo */}
          <TouchableOpacity
            activeOpacity={0.85}
            style={{
              flexDirection: "row", alignItems: "center",
              backgroundColor: "#3D2B5E", borderRadius: 14,
              padding: 14, marginTop: 24, gap: 10,
            }}
          >
            <Ionicons name="search-outline" size={20} color="#6B7280" />
            <Text style={{ color: "#6B7280", fontSize: 15 }}>Buscar eventos...</Text>
          </TouchableOpacity>
        </LinearGradient>

        {/* Accesos rápidos */}
        <View style={{ paddingHorizontal: 24, marginTop: 8 }}>
          <Text style={{ color: "#FFFFFF", fontSize: 18, fontWeight: "700", marginBottom: 16 }}>
            Accesos rápidos
          </Text>
          <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 12 }}>
            {quickActions.map((action, index) => (
              <TouchableOpacity
                key={index}
                activeOpacity={0.85}
                style={{
                  width: (width - 60) / 2,
                  backgroundColor: "#2D1B4E", borderRadius: 16,
                  padding: 20, alignItems: "center",
                  borderWidth: 1, borderColor: "#3D2B5E",
                }}
              >
                <View style={{
                  width: 48, height: 48, borderRadius: 14,
                  backgroundColor: action.color + "22",
                  alignItems: "center", justifyContent: "center", marginBottom: 10,
                }}>
                  <Ionicons name={action.icon} size={24} color={action.color} />
                </View>
                <Text style={{ color: "#FFFFFF", fontSize: 13, fontWeight: "600", textAlign: "center" }}>
                  {action.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* Próximos eventos placeholder */}
        <View style={{ paddingHorizontal: 24, marginTop: 28 }}>
          <Text style={{ color: "#FFFFFF", fontSize: 18, fontWeight: "700", marginBottom: 16 }}>
            Próximos eventos
          </Text>
          <View style={{
            backgroundColor: "#2D1B4E", borderRadius: 16,
            padding: 24, alignItems: "center",
            borderWidth: 1, borderColor: "#3D2B5E",
          }}>
            <Ionicons name="calendar-outline" size={40} color="#7C3AED" />
            <Text style={{ color: "#FFFFFF", fontSize: 15, fontWeight: "600", marginTop: 12 }}>
              Próximamente
            </Text>
            <Text style={{ color: "#9CA3AF", fontSize: 13, marginTop: 6, textAlign: "center" }}>
              Los eventos aparecerán aquí cuando estén disponibles
            </Text>
          </View>
        </View>
      </ScrollView>
    </View>
  );
}
