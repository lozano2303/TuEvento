import { useRef, useEffect } from "react";
import {
  View, Text, ScrollView, TouchableOpacity,
  StatusBar, Alert, Animated,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";
import { navigationRef } from "../navigation/AppNavigator";

// ─── Helpers ─────────────────────────────────────────────────────────────────
function getDisplayName(fullName) {
  if (!fullName) return "Usuario";
  const parts = fullName.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "Usuario";
  if (parts.length === 1) return parts[0];
  const first = parts[0];
  const second = parts[1];
  if (first.length <= 3) return second.length > first.length ? second : first;
  return first;
}

function getInitial(fullName) {
  return getDisplayName(fullName).charAt(0).toUpperCase();
}

function getRoleBadge(role) {
  if (role === "ADMIN")     return { label: "Administrador", color: colors.error };
  if (role === "ORGANIZER") return { label: "Organizador",   color: "#3B82F6" };
  return { label: "Usuario", color: colors.accent };
}

// ─── Opciones del menú de perfil ─────────────────────────────────────────────
const MENU_OPTIONS = [
  { icon: "person-outline",        label: "Editar perfil",    sub: "Actualiza tu información",  route: null },
  { icon: "notifications-outline", label: "Notificaciones",   sub: "Gestiona tus alertas",       route: null },
  { icon: "settings-outline",      label: "Configuración",    sub: "Preferencias de la app",     route: "Settings" },
];

export default function ProfileScreen() {
  const { user, logout } = useAuth();
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();
  const navigation = useNavigation();

  // Animaciones de entrada
  const cardAnim = useRef(new Animated.Value(0)).current;
  const menuAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.stagger(100, [
      Animated.spring(cardAnim, { toValue: 1, useNativeDriver: true, tension: 60, friction: 8 }),
      Animated.spring(menuAnim, { toValue: 1, useNativeDriver: true, tension: 60, friction: 8 }),
    ]).start();
  }, []);

  const animStyle = (anim) => ({
    opacity: anim,
    transform: [{ translateY: anim.interpolate({ inputRange: [0, 1], outputRange: [20, 0] }) }],
  });

  const displayName = getDisplayName(user?.fullName);
  const initial     = getInitial(user?.fullName);
  const badge       = getRoleBadge(user?.role);
  const email       = user?.alias || "";

  const handleLogout = () => {
    Alert.alert(
      "Cerrar sesión",
      "¿Estás seguro de que quieres cerrar sesión?",
      [
        { text: "Cancelar", style: "cancel" },
        {
          text: "Cerrar sesión",
          style: "destructive",
          onPress: async () => {
            await AsyncStorage.multiRemove(["accessToken", "refreshToken"]);
            await logout();
            if (navigationRef.isReady()) {
              navigationRef.reset({ index: 0, routes: [{ name: "Login" }] });
            }
          },
        },
      ]
    );
  };

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      {/* Orbes de luz ambiental */}
      <View pointerEvents="none" style={{
        position: "absolute", top: -40, right: -60,
        width: 240, height: 240, borderRadius: 120,
        backgroundColor: colors.primary + "1A",
      }} />
      <View pointerEvents="none" style={{
        position: "absolute", bottom: 80, left: -50,
        width: 200, height: 200, borderRadius: 100,
        backgroundColor: colors.accent + "12",
      }} />

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: insets.bottom + 32 }}
      >
        {/* ── Hero con gradiente ── */}
        <LinearGradient
          colors={colors.gradientPrimary}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={{
            paddingTop: insets.top + 24,
            paddingBottom: 64,
            paddingHorizontal: 24,
            alignItems: "center",
          }}
        >
          <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "800", opacity: 0.9 }}>
            Mi Perfil
          </Text>
        </LinearGradient>

        {/* ── Avatar solapando el hero ── */}
        <Animated.View style={[{ alignItems: "center", marginTop: -52 }, animStyle(cardAnim)]}>
          {/* Avatar */}
          <View style={{
            width: 96, height: 96, borderRadius: 48,
            borderWidth: 3, borderColor: colors.textPrimary,
            overflow: "hidden",
            shadowColor: colors.primary,
            shadowOffset: { width: 0, height: 8 },
            shadowOpacity: 0.5, shadowRadius: 16, elevation: 12,
          }}>
            <LinearGradient
              colors={colors.gradientPrimary}
              style={{ flex: 1, alignItems: "center", justifyContent: "center" }}
            >
              <Text style={{ color: colors.textPrimary, fontSize: 38, fontWeight: "800" }}>
                {initial}
              </Text>
            </LinearGradient>
          </View>

          {/* Nombre */}
          <Text style={{
            color: colors.textPrimary, fontSize: 22, fontWeight: "800",
            marginTop: 14, textAlign: "center",
          }}>
            {user?.fullName || displayName}
          </Text>

          {/* Email / alias */}
          <Text style={{
            color: colors.textSecondary, fontSize: 14,
            marginTop: 4, textAlign: "center",
          }}>
            {email}
          </Text>

          {/* Badge de rol */}
          <View style={{
            marginTop: 10,
            backgroundColor: badge.color + "22",
            borderRadius: 20, paddingHorizontal: 14, paddingVertical: 5,
            borderWidth: 1, borderColor: badge.color + "55",
          }}>
            <Text style={{ color: badge.color, fontSize: 12, fontWeight: "700", letterSpacing: 0.4 }}>
              {badge.label}
            </Text>
          </View>
        </Animated.View>

        {/* ── Opciones de menú ── */}
        <Animated.View style={[{ paddingHorizontal: 24, marginTop: 32 }, animStyle(menuAnim)]}>
          <Text style={{
            color: colors.textSecondary, fontSize: 11, fontWeight: "700",
            letterSpacing: 1.2, textTransform: "uppercase", marginBottom: 12,
          }}>
            Cuenta
          </Text>

          {MENU_OPTIONS.map((opt, index) => (
            <TouchableOpacity
              key={index}
              activeOpacity={0.75}
              onPress={() => opt.route && navigation.navigate(opt.route)}
              style={{
                flexDirection: "row", alignItems: "center",
                backgroundColor: colors.surface + "CC",
                borderRadius: 14, borderWidth: 1,
                borderColor: colors.primary + "30",
                padding: 16, marginBottom: 10,
              }}
            >
              {/* Ícono */}
              <View style={{
                width: 40, height: 40, borderRadius: 12,
                backgroundColor: colors.primary + "28",
                borderWidth: 1, borderColor: colors.primary + "40",
                alignItems: "center", justifyContent: "center",
                marginRight: 14,
              }}>
                <Ionicons name={opt.icon} size={20} color={colors.accent} />
              </View>

              {/* Texto */}
              <View style={{ flex: 1 }}>
                <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "600" }}>
                  {opt.label}
                </Text>
                <Text style={{ color: colors.textMuted, fontSize: 12, marginTop: 1 }}>
                  {opt.sub}
                </Text>
              </View>

              {/* Chevron */}
              <Ionicons name="chevron-forward" size={18} color={colors.textMuted} />
            </TouchableOpacity>
          ))}

          {/* ── Botón cerrar sesión ── */}
          <TouchableOpacity
            onPress={handleLogout}
            activeOpacity={0.75}
            style={{
              flexDirection: "row", alignItems: "center", justifyContent: "center",
              backgroundColor: colors.error,
              borderRadius: 14, padding: 16,
              marginTop: 8, gap: 8,
              shadowColor: colors.error,
              shadowOffset: { width: 0, height: 4 },
              shadowOpacity: 0.4, shadowRadius: 10, elevation: 8,
            }}
          >
            <Ionicons name="log-out-outline" size={20} color={colors.textPrimary} />
            <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700" }}>
              Cerrar sesión
            </Text>
          </TouchableOpacity>
        </Animated.View>
      </ScrollView>
    </View>
  );
}
