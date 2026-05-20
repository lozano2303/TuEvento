import { useRef, useEffect } from "react";
import {
  View, Text, ScrollView, TouchableOpacity,
  StatusBar, Animated, StyleSheet,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import BackButton from "../components/BackButton";

// ─── Descripciones cortas por tema ───────────────────────────────────────────
const THEME_DESCRIPTIONS = {
  PRINCIPAL: "Identidad oficial de Tu Evento",
  DARK:      "Oscuro neutro con grises profundos",
  LIGHT:     "Claro y limpio para el día",
  PASTEL:    "Suave y relajado",
  VIBRANT:   "Colores saturados y energéticos",
  NATURE:    "Verde orgánico y tierras",
  OCEAN:     "Azules profundos y turquesas",
  SUNSET:    "Cálido y acogedor",
};

// ─── Colores de preview hardcodeados por tema ─────────────────────────────────
const THEME_PREVIEWS = {
  PRINCIPAL: { background: "#1E0A3C", primary: "#7C3AED", accent: "#A78BFA" },
  DARK:      { background: "#0D0D0D", primary: "#E0E0E0", accent: "#757575" },
  LIGHT:     { background: "#FFFFFF", primary: "#424242", accent: "#757575" },
  PASTEL:    { background: "#FFF9FB", primary: "#F48FB1", accent: "#CE93D8" },
  VIBRANT:   { background: "#0A0A0A", primary: "#FF1744", accent: "#FFEA00" },
  NATURE:    { background: "#F1F8E9", primary: "#2E7D32", accent: "#8D6E63" },
  OCEAN:     { background: "#E3F2FD", primary: "#0277BD", accent: "#00BCD4" },
  SUNSET:    { background: "#FFF3E0", primary: "#E64A19", accent: "#FF8A65" },
};

export default function SettingsScreen() {
  const navigation = useNavigation();
  const insets     = useSafeAreaInsets();
  const { colors, activeThemeId, applyTheme, themes } = useTheme();

  // Animación de entrada
  const listAnim = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    Animated.spring(listAnim, {
      toValue: 1,
      useNativeDriver: true,
      tension: 60,
      friction: 8,
    }).start();
  }, []);

  const styles = makeStyles(colors);

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      {/* Orbes de luz ambiental */}
      <View
        pointerEvents="none"
        style={{
          position: "absolute", top: -50, right: -50,
          width: 220, height: 220, borderRadius: 110,
          backgroundColor: colors.primary + "1A",
        }}
      />
      <View
        pointerEvents="none"
        style={{
          position: "absolute", bottom: 100, left: -60,
          width: 200, height: 200, borderRadius: 100,
          backgroundColor: colors.accent + "12",
        }}
      />

      {/* ── Header ── */}
      <View
        style={{
          paddingTop: insets.top + 8,
          paddingBottom: 12,
          paddingHorizontal: 24,
          flexDirection: "row",
          alignItems: "center",
          borderBottomWidth: 1,
          borderBottomColor: colors.primary + "20",
        }}
      >
        <BackButton style={{ marginBottom: 0 }} />
        <View style={{ flex: 1, alignItems: "center" }}>
          <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "800" }}>
            Configuración
          </Text>
        </View>
        {/* Spacer para centrar el título */}
        <View style={{ width: 40 }} />
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: insets.bottom + 32 }}
      >
        {/* ── Sección Tema de color ── */}
        <Animated.View
          style={{
            paddingHorizontal: 24,
            marginTop: 28,
            opacity: listAnim,
            transform: [
              {
                translateY: listAnim.interpolate({
                  inputRange: [0, 1],
                  outputRange: [20, 0],
                }),
              },
            ],
          }}
        >
          {/* Encabezado de sección */}
          <Text
            style={{
              color: colors.textSecondary,
              fontSize: 11,
              fontWeight: "700",
              letterSpacing: 1.2,
              textTransform: "uppercase",
              marginBottom: 4,
            }}
          >
            Apariencia
          </Text>
          <Text
            style={{
              color: colors.textPrimary,
              fontSize: 20,
              fontWeight: "800",
              marginBottom: 6,
            }}
          >
            Tema de color
          </Text>
          <Text
            style={{
              color: colors.textMuted,
              fontSize: 13,
              lineHeight: 20,
              marginBottom: 20,
            }}
          >
            Elige el tema que mejor se adapte a tus preferencias. El cambio se aplica de inmediato.
          </Text>

          {/* ── Cards de temas ── */}
          {themes.map((theme) => {
            const isActive    = theme.id === activeThemeId;
            const themeKey    = (theme.id ?? theme.name)?.toUpperCase();
            const preview     = THEME_PREVIEWS[themeKey] ?? THEME_PREVIEWS.PRINCIPAL;
            const description = THEME_DESCRIPTIONS[themeKey] ?? "";

            return (
              <TouchableOpacity
                key={theme.id}
                onPress={() => applyTheme(theme.id)}
                activeOpacity={0.75}
                style={[styles.themeCard, isActive && styles.themeCardActive]}
              >
                {/* Preview visual */}
                <View style={[styles.themePreviewBanner, { backgroundColor: preview.background }]}>
                  <View style={[styles.previewCircle, { backgroundColor: preview.primary }]} />
                  <View style={[styles.previewCircle, { backgroundColor: preview.accent }]} />
                </View>

                {/* Info */}
                <View style={styles.themeInfo}>
                  <Text style={styles.themeName}>{theme.name}</Text>
                  <Text style={styles.themeDescription}>
                    {description}
                  </Text>
                </View>

                {/* Badges */}
                <View style={styles.themeBadges}>
                  {isActive && (
                    <View style={styles.activeBadge}>
                      <Text style={styles.activeBadgeText}>ACTIVO</Text>
                    </View>
                  )}
                  {isActive && themeKey === "PRINCIPAL" && (
                    <View style={styles.officialBadge}>
                      <Text style={styles.officialBadgeText}>OFICIAL</Text>
                    </View>
                  )}
                  {isActive && themeKey !== "PRINCIPAL" && (
                    <TouchableOpacity
                      activeOpacity={0.75}
                      style={styles.customizeButton}
                      onPress={() =>
                        navigation.navigate("ThemeCustomize", {
                          themeName: themeKey,
                          activeThemeId: theme.id,
                        })
                      }
                    >
                      <Text style={styles.customizeButtonText}>PERSONALIZAR</Text>
                    </TouchableOpacity>
                  )}
                </View>
              </TouchableOpacity>
            );
          })}

          {/* Nota informativa */}
          <View
            style={{
              flexDirection: "row",
              alignItems: "flex-start",
              gap: 10,
              backgroundColor: colors.surface + "80",
              borderRadius: 12,
              borderWidth: 1,
              borderColor: colors.primary + "25",
              padding: 14,
              marginTop: 4,
            }}
          >
            <Ionicons
              name="information-circle-outline"
              size={18}
              color={colors.accent}
              style={{ marginTop: 1 }}
            />
            <Text
              style={{
                color: colors.textSecondary,
                fontSize: 12,
                lineHeight: 18,
                flex: 1,
              }}
            >
              El tema se guarda automáticamente y se restaura la próxima vez que abras la app.
            </Text>
          </View>
        </Animated.View>
      </ScrollView>
    </View>
  );
}

// ─── Estilos ──────────────────────────────────────────────────────────────────
function makeStyles(colors) {
  return StyleSheet.create({
    // Tarjeta base
    themeCard: {
      backgroundColor: colors.surface + "CC",
      borderRadius: 16,
      borderWidth: 1.5,
      borderColor: colors.primary + "30",
      marginBottom: 12,
      overflow: "hidden",
    },
    themeCardActive: {
      backgroundColor: colors.primary + "20",
      borderColor: colors.primary,
    },

    // Preview visual
    themePreviewBanner: {
      height: 48,
      borderRadius: 0,
      flexDirection: "row",
      alignItems: "center",
      paddingHorizontal: 12,
      gap: 8,
    },
    previewCircle: {
      width: 24,
      height: 24,
      borderRadius: 12,
      borderWidth: 2,
      borderColor: "rgba(255,255,255,0.3)",
    },

    // Info
    themeInfo: {
      paddingHorizontal: 16,
      paddingTop: 12,
      marginBottom: 10,
    },
    themeName: {
      fontSize: 15,
      fontWeight: "600",
      color: colors.textPrimary,
      marginBottom: 2,
    },
    themeDescription: {
      fontSize: 12,
      color: colors.textSecondary,
    },

    // Badges
    themeBadges: {
      flexDirection: "row",
      gap: 8,
      flexWrap: "wrap",
      paddingHorizontal: 16,
      paddingBottom: 14,
    },
    activeBadge: {
      backgroundColor: colors.primary,
      borderRadius: 6,
      paddingHorizontal: 8,
      paddingVertical: 3,
    },
    activeBadgeText: {
      color: "#FFFFFF",
      fontSize: 10,
      fontWeight: "700",
      letterSpacing: 0.5,
    },
    officialBadge: {
      backgroundColor: colors.accent,
      borderRadius: 6,
      paddingHorizontal: 8,
      paddingVertical: 3,
    },
    officialBadgeText: {
      color: colors.background,
      fontSize: 10,
      fontWeight: "700",
    },
    customizeButton: {
      backgroundColor: colors.accent + "22",
      borderRadius: 6,
      paddingHorizontal: 8,
      paddingVertical: 3,
      borderWidth: 1,
      borderColor: colors.accent + "55",
    },
    customizeButtonText: {
      color: colors.accent,
      fontSize: 10,
      fontWeight: "800",
      letterSpacing: 0.5,
    },
  });
}
