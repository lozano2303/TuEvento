import { useRef, useEffect } from "react";
import {
  SafeAreaView,
  ScrollView,
  View,
  Text,
  TouchableOpacity,
  StatusBar,
  Animated,
  StyleSheet,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useRoute } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import BackButton from "../components/BackButton";

// ─── Grupos de propiedades personalizables ────────────────────────────────────
const PROPERTY_GROUPS = [
  {
    label: "Fondos",
    icon: "layers-outline",
    properties: ["background", "surface", "surfaceAlt"],
  },
  {
    label: "Primarios",
    icon: "color-palette-outline",
    properties: ["primary", "primaryDark", "accent"],
  },
  {
    label: "Textos",
    icon: "text-outline",
    properties: ["textPrimary", "textSecondary", "textMuted"],
  },
  {
    label: "Estados",
    icon: "alert-circle-outline",
    properties: ["error", "errorBg", "success", "successBg"],
  },
];

// ─── Labels en español para cada propiedad ────────────────────────────────────
const PROPERTY_LABELS = {
  background:    "Fondo principal",
  surface:       "Superficie",
  surfaceAlt:    "Superficie alternativa",
  primary:       "Color primario",
  primaryDark:   "Primario oscuro",
  accent:        "Acento",
  textPrimary:   "Texto principal",
  textSecondary: "Texto secundario",
  textMuted:     "Texto sutil",
  error:         "Error",
  errorBg:       "Fondo de error",
  success:       "Éxito",
  successBg:     "Fondo de éxito",
};

// ─── Componente fila de propiedad ─────────────────────────────────────────────
function PropertyRow({ property, value, colors, styles, onPress }) {
  return (
    <TouchableOpacity
      onPress={onPress}
      activeOpacity={0.75}
      style={styles.propertyRow}
    >
      {/* Círculo de color */}
      <View
        style={[
          styles.colorSwatch,
          {
            backgroundColor: value,
            borderColor: colors.surfaceAlt,
          },
        ]}
      />

      {/* Label + valor hex */}
      <View style={styles.propertyInfo}>
        <Text style={[styles.propertyLabel, { color: colors.textPrimary }]}>
          {PROPERTY_LABELS[property] ?? property}
        </Text>
        <Text style={[styles.propertyValue, { color: colors.textMuted }]}>
          {value}
        </Text>
      </View>

      {/* TODO: color picker — Parte 2 */}
      <Ionicons name="chevron-forward" size={16} color={colors.textMuted} />
    </TouchableOpacity>
  );
}

// ─── Pantalla principal ───────────────────────────────────────────────────────
export default function ThemeCustomizeScreen() {
  const route  = useRoute();
  const insets = useSafeAreaInsets();
  const { colors, palette } = useTheme();

  const { themeName, activeThemeId } = route.params ?? {};

  // Animación de entrada
  const fadeAnim = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    Animated.spring(fadeAnim, {
      toValue: 1,
      useNativeDriver: true,
      tension: 60,
      friction: 8,
    }).start();
  }, []);

  const styles = makeStyles(colors);

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      {/* Orbes de luz ambiental */}
      <View pointerEvents="none" style={[styles.orb1, { backgroundColor: colors.primary + "1A" }]} />
      <View pointerEvents="none" style={[styles.orb2, { backgroundColor: colors.accent + "12" }]} />

      {/* ── Header ── */}
      <View
        style={[
          styles.header,
          {
            paddingTop: insets.top + 8,
            borderBottomColor: colors.primary + "20",
          },
        ]}
      >
        <BackButton style={{ marginBottom: 0 }} />
        <View style={styles.headerCenter}>
          <Text style={[styles.headerTitle, { color: colors.textPrimary }]}>
            Personalizar tema
          </Text>
          {themeName ? (
            <Text style={[styles.headerSub, { color: colors.accent }]}>
              {themeName}
            </Text>
          ) : null}
        </View>
        {/* Spacer para centrar el título */}
        <View style={{ width: 40 }} />
      </View>

      {/* ── Contenido ── */}
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={[
          styles.scrollContent,
          { paddingBottom: insets.bottom + 32 },
        ]}
      >
        <Animated.View
          style={{
            opacity: fadeAnim,
            transform: [
              {
                translateY: fadeAnim.interpolate({
                  inputRange: [0, 1],
                  outputRange: [20, 0],
                }),
              },
            ],
          }}
        >
          {/* Descripción */}
          <Text style={[styles.description, { color: colors.textMuted }]}>
            Toca cualquier propiedad para cambiar su color. Los cambios se aplican al instante y se sincronizan con tu cuenta.
          </Text>

          {/* Grupos de propiedades */}
          {PROPERTY_GROUPS.map((group) => (
            <View key={group.label} style={styles.group}>
              {/* Encabezado del grupo */}
              <View style={styles.groupHeader}>
                <Ionicons
                  name={group.icon}
                  size={14}
                  color={colors.accent}
                  style={{ marginRight: 6 }}
                />
                <Text style={[styles.groupLabel, { color: colors.textSecondary }]}>
                  {group.label.toUpperCase()}
                </Text>
              </View>

              {/* Card del grupo */}
              <View
                style={[
                  styles.groupCard,
                  {
                    backgroundColor: colors.surface + "CC",
                    borderColor: colors.primary + "30",
                  },
                ]}
              >
                {group.properties.map((prop, idx) => {
                  const value = palette?.[prop] ?? colors[prop] ?? "#000000";
                  const isLast = idx === group.properties.length - 1;
                  return (
                    <View key={prop}>
                      <PropertyRow
                        property={prop}
                        value={value}
                        colors={colors}
                        styles={styles}
                        onPress={() => {
                          // TODO Parte 2: abrir color picker para `prop`
                        }}
                      />
                      {!isLast && (
                        <View
                          style={[
                            styles.separator,
                            { backgroundColor: colors.primary + "18" },
                          ]}
                        />
                      )}
                    </View>
                  );
                })}
              </View>
            </View>
          ))}

          {/* Nota informativa */}
          <View
            style={[
              styles.infoBox,
              {
                backgroundColor: colors.surface + "80",
                borderColor: colors.primary + "25",
              },
            ]}
          >
            <Ionicons
              name="information-circle-outline"
              size={18}
              color={colors.accent}
              style={{ marginTop: 1 }}
            />
            <Text style={[styles.infoText, { color: colors.textSecondary }]}>
              Puedes restablecer cada color al valor original del tema en cualquier momento.
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
    container: {
      flex: 1,
    },
    orb1: {
      position: "absolute",
      top: -50,
      right: -50,
      width: 220,
      height: 220,
      borderRadius: 110,
    },
    orb2: {
      position: "absolute",
      bottom: 100,
      left: -60,
      width: 200,
      height: 200,
      borderRadius: 100,
    },
    // Header
    header: {
      paddingBottom: 12,
      paddingHorizontal: 24,
      flexDirection: "row",
      alignItems: "center",
      borderBottomWidth: 1,
    },
    headerCenter: {
      flex: 1,
      alignItems: "center",
    },
    headerTitle: {
      fontSize: 18,
      fontWeight: "800",
    },
    headerSub: {
      fontSize: 12,
      fontWeight: "600",
      marginTop: 2,
      opacity: 0.85,
    },
    // Scroll
    scrollContent: {
      paddingHorizontal: 24,
      paddingTop: 24,
    },
    description: {
      fontSize: 13,
      lineHeight: 20,
      marginBottom: 24,
    },
    // Grupos
    group: {
      marginBottom: 20,
    },
    groupHeader: {
      flexDirection: "row",
      alignItems: "center",
      marginBottom: 8,
    },
    groupLabel: {
      fontSize: 11,
      fontWeight: "700",
      letterSpacing: 1.2,
    },
    groupCard: {
      borderRadius: 16,
      borderWidth: 1,
      overflow: "hidden",
    },
    // Fila de propiedad
    propertyRow: {
      flexDirection: "row",
      alignItems: "center",
      paddingHorizontal: 16,
      paddingVertical: 14,
    },
    colorSwatch: {
      width: 36,
      height: 36,
      borderRadius: 10,
      borderWidth: 1.5,
      marginRight: 14,
    },
    propertyInfo: {
      flex: 1,
    },
    propertyLabel: {
      fontSize: 14,
      fontWeight: "600",
      marginBottom: 2,
    },
    propertyValue: {
      fontSize: 12,
      fontFamily: "monospace",
    },
    separator: {
      height: 1,
      marginHorizontal: 16,
    },
    // Info box
    infoBox: {
      flexDirection: "row",
      alignItems: "flex-start",
      gap: 10,
      borderRadius: 12,
      borderWidth: 1,
      padding: 14,
      marginTop: 4,
    },
    infoText: {
      fontSize: 12,
      lineHeight: 18,
      flex: 1,
    },
  });
}
