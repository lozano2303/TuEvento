import { useState, useRef, useEffect } from "react";
import {
  View, Text, ScrollView, TouchableOpacity,
  StatusBar, Dimensions, Animated,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";

const { width } = Dimensions.get("window");

// ─── Colores fijos por categoría (no del tema) ───────────────────────────────
const CATEGORY_COLORS = {
  Todos:    "#7C3AED",
  Música:   "#EC4899",
  Teatro:   "#F59E0B",
  Deportes: "#10B981",
  Arte:     "#3B82F6",
  Tech:     "#06B6D4",
};

const CATEGORIES = ["Todos", "Música", "Teatro", "Deportes", "Arte", "Tech"];

// ─── Datos de ejemplo (se reemplazarán con fetch real) ───────────────────────
const SAMPLE_EVENTS = [
  {
    id: 1,
    nombre: "Festival de Jazz en el Parque",
    fecha: "15 Jun 2025",
    ciudad: "Bogotá",
    categoria: "Música",
    emoji: "🎷",
    color: CATEGORY_COLORS.Música,
  },
  {
    id: 2,
    nombre: "Hackathon TechBogotá 2025",
    fecha: "22 Jun 2025",
    ciudad: "Medellín",
    categoria: "Tech",
    emoji: "💻",
    color: CATEGORY_COLORS.Tech,
  },
  {
    id: 3,
    nombre: "Exposición Arte Contemporáneo",
    fecha: "1 Jul 2025",
    ciudad: "Cali",
    categoria: "Arte",
    emoji: "🎨",
    color: CATEGORY_COLORS.Arte,
  },
];

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

// ─── Componente principal ─────────────────────────────────────────────────────
export default function HomeScreen() {
  const { colors } = useTheme();
  const { user } = useAuth();
  const insets = useSafeAreaInsets();
  const navigation = useNavigation();
  const [activeCategory, setActiveCategory] = useState("Todos");

  // Animaciones de entrada con stagger
  const heroAnim   = useRef(new Animated.Value(0)).current;
  const bentoAnim  = useRef(new Animated.Value(0)).current;
  const eventsAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.stagger(120, [
      Animated.spring(heroAnim,   { toValue: 1, useNativeDriver: true, tension: 60, friction: 8 }),
      Animated.spring(bentoAnim,  { toValue: 1, useNativeDriver: true, tension: 60, friction: 8 }),
      Animated.spring(eventsAnim, { toValue: 1, useNativeDriver: true, tension: 60, friction: 8 }),
    ]).start();
  }, []);

  const animStyle = (anim) => ({
    opacity: anim,
    transform: [{ translateY: anim.interpolate({ inputRange: [0, 1], outputRange: [24, 0] }) }],
  });

  const displayName = getDisplayName(user?.fullName);
  const initial     = getInitial(user?.fullName);

  const filtered = activeCategory === "Todos"
    ? SAMPLE_EVENTS
    : SAMPLE_EVENTS.filter((e) => e.categoria === activeCategory);

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      {/* Orbes de luz ambiental */}
      <View pointerEvents="none" style={{ position: "absolute", top: -60, left: -60,
        width: 260, height: 260, borderRadius: 130,
        backgroundColor: colors.primary + "1E" }} />
      <View pointerEvents="none" style={{ position: "absolute", top: 180, right: -80,
        width: 220, height: 220, borderRadius: 110,
        backgroundColor: colors.accent + "14" }} />
      <View pointerEvents="none" style={{ position: "absolute", bottom: 120, left: -40,
        width: 180, height: 180, borderRadius: 90,
        backgroundColor: colors.primaryDark + "18" }} />

      {/* ── Top bar ── */}
      <View style={{
        paddingTop: insets.top + 8,
        paddingBottom: 12,
        paddingHorizontal: 24,
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
        borderBottomWidth: 1,
        borderBottomColor: colors.primary + "20",
      }}>
        {/* Logo */}
        <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
          <View style={{
            width: 30, height: 30, borderRadius: 8,
            backgroundColor: colors.primary,
            alignItems: "center", justifyContent: "center",
          }}>
            <Ionicons name="calendar" size={16} color={colors.textPrimary} />
          </View>
          <Text style={{ color: colors.textPrimary, fontSize: 17, fontWeight: "800", letterSpacing: 0.2 }}>
            Tu<Text style={{ color: colors.accent }}>Evento</Text>
          </Text>
        </View>

        {/* Avatar */}
        <TouchableOpacity
          onPress={() => navigation.navigate("Perfil")}
          activeOpacity={0.75}
          style={{
            width: 38, height: 38, borderRadius: 19,
            backgroundColor: colors.primary + "40",
            borderWidth: 2, borderColor: colors.accent + "80",
            alignItems: "center", justifyContent: "center",
          }}
        >
          <Text style={{ color: colors.textPrimary, fontSize: 16, fontWeight: "800" }}>
            {initial}
          </Text>
        </TouchableOpacity>
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: insets.bottom + 32 }}
      >
        {/* ── Hero ── */}
        <Animated.View style={[{ paddingHorizontal: 24, paddingTop: 28, paddingBottom: 8 }, animStyle(heroAnim)]}>
          <Text style={{ color: colors.textSecondary, fontSize: 14, marginBottom: 8 }}>
            Hola, {displayName} 👋
          </Text>
          <Text style={{ color: colors.textPrimary, fontSize: 26, fontWeight: "800", lineHeight: 34 }}>
            Organiza y vive{"\n"}
            <Text style={{ color: colors.primary }}>eventos</Text>
            <Text style={{ color: colors.textPrimary }}> únicos.</Text>
          </Text>
        </Animated.View>

        {/* ── Bento accesos rápidos ── */}
        <Animated.View style={[{ paddingHorizontal: 24, marginTop: 24 }, animStyle(bentoAnim)]}>
          <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700", marginBottom: 12 }}>
            Accesos rápidos
          </Text>
          <View style={{ flexDirection: "row", gap: 12 }}>
            {/* Card Buscar */}
            <TouchableOpacity
              activeOpacity={0.75}
              style={{
                flex: 1, height: 90,
                backgroundColor: colors.primary + "28",
                borderRadius: 14, borderWidth: 1,
                borderColor: colors.primary + "50",
                padding: 14, justifyContent: "space-between",
              }}
            >
              <View style={{
                width: 36, height: 36, borderRadius: 10,
                backgroundColor: colors.primary + "40",
                alignItems: "center", justifyContent: "center",
              }}>
                <Text style={{ fontSize: 18 }}>🔍</Text>
              </View>
              <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
                <Text style={{ color: colors.textPrimary, fontSize: 13, fontWeight: "700" }}>
                  Buscar{"\n"}eventos
                </Text>
                <View style={{
                  width: 26, height: 26, borderRadius: 13,
                  backgroundColor: colors.primary,
                  alignItems: "center", justifyContent: "center",
                }}>
                  <Ionicons name="arrow-forward" size={13} color={colors.textPrimary} />
                </View>
              </View>
            </TouchableOpacity>

            {/* Card Mis tickets */}
            <TouchableOpacity
              activeOpacity={0.75}
              style={{
                flex: 1, height: 90,
                backgroundColor: colors.success + "28",
                borderRadius: 14, borderWidth: 1,
                borderColor: colors.success + "50",
                padding: 14, justifyContent: "space-between",
              }}
            >
              <View style={{
                width: 36, height: 36, borderRadius: 10,
                backgroundColor: colors.success + "40",
                alignItems: "center", justifyContent: "center",
              }}>
                <Text style={{ fontSize: 18 }}>🎟</Text>
              </View>
              <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
                <Text style={{ color: colors.textPrimary, fontSize: 13, fontWeight: "700" }}>
                  Mis{"\n"}tickets
                </Text>
                <View style={{
                  width: 26, height: 26, borderRadius: 13,
                  backgroundColor: colors.success,
                  alignItems: "center", justifyContent: "center",
                }}>
                  <Ionicons name="arrow-forward" size={13} color={colors.textPrimary} />
                </View>
              </View>
            </TouchableOpacity>
          </View>
        </Animated.View>

        {/* ── Próximos eventos ── */}
        <Animated.View style={[{ marginTop: 28 }, animStyle(eventsAnim)]}>
          <Text style={{
            color: colors.textPrimary, fontSize: 15, fontWeight: "700",
            paddingHorizontal: 24, marginBottom: 14,
          }}>
            Próximos eventos
          </Text>

          {/* Chips de filtro */}
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={{ paddingHorizontal: 24, gap: 8 }}
          >
            {CATEGORIES.map((cat) => {
              const isActive = cat === activeCategory;
              const catColor = CATEGORY_COLORS[cat];
              return (
                <TouchableOpacity
                  key={cat}
                  onPress={() => setActiveCategory(cat)}
                  activeOpacity={0.75}
                  style={{
                    paddingHorizontal: 16, paddingVertical: 7,
                    borderRadius: 20,
                    backgroundColor: isActive ? catColor : colors.surface,
                    borderWidth: 1,
                    borderColor: isActive ? catColor : colors.surfaceAlt,
                  }}
                >
                  <Text style={{
                    color: isActive ? colors.textPrimary : colors.textSecondary,
                    fontSize: 13,
                    fontWeight: isActive ? "700" : "500",
                  }}>
                    {cat}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </ScrollView>

          {/* Lista de eventos */}
          <View style={{ paddingHorizontal: 24, marginTop: 14 }}>
            {filtered.length > 0 ? (
              filtered.map((evento) => (
                <TouchableOpacity
                  key={evento.id}
                  activeOpacity={0.75}
                  style={{
                    flexDirection: "row",
                    backgroundColor: colors.surface + "CC",
                    borderRadius: 14, borderWidth: 1,
                    borderColor: colors.surfaceAlt,
                    marginBottom: 10, overflow: "hidden",
                  }}
                >
                  {/* Franja lateral */}
                  <View style={{
                    width: 48,
                    backgroundColor: evento.color + "30",
                    borderRightWidth: 1,
                    borderRightColor: evento.color + "40",
                    alignItems: "center", justifyContent: "center",
                  }}>
                    <Text style={{ fontSize: 22 }}>{evento.emoji}</Text>
                  </View>

                  {/* Contenido */}
                  <View style={{ flex: 1, padding: 12 }}>
                    <View style={{ flexDirection: "row", alignItems: "flex-start", justifyContent: "space-between", gap: 8 }}>
                      <Text style={{
                        color: colors.textPrimary, fontSize: 14,
                        fontWeight: "700", flex: 1,
                      }} numberOfLines={1}>
                        {evento.nombre}
                      </Text>
                      <View style={{
                        backgroundColor: evento.color + "33",
                        borderRadius: 10, paddingHorizontal: 8, paddingVertical: 2,
                        borderWidth: 1, borderColor: evento.color + "66",
                      }}>
                        <Text style={{ color: evento.color, fontSize: 10, fontWeight: "700" }}>
                          {evento.categoria}
                        </Text>
                      </View>
                    </View>
                    <Text style={{ color: colors.textSecondary, fontSize: 12, marginTop: 4 }}>
                      {evento.fecha} · {evento.ciudad}
                    </Text>
                  </View>
                </TouchableOpacity>
              ))
            ) : (
              /* Estado vacío */
              <View style={{
                borderRadius: 14, borderWidth: 1.5,
                borderColor: colors.surfaceAlt,
                borderStyle: "dashed",
                padding: 36, alignItems: "center",
              }}>
                <Text style={{ fontSize: 38, marginBottom: 12 }}>🗓</Text>
                <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700", marginBottom: 6 }}>
                  Sin eventos por aquí
                </Text>
                <Text style={{ color: colors.textMuted, fontSize: 13, textAlign: "center", lineHeight: 20 }}>
                  Pronto habrá contenido disponible
                </Text>
              </View>
            )}
          </View>
        </Animated.View>
      </ScrollView>
    </View>
  );
}
