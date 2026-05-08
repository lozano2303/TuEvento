import { useRef, useEffect, useState, useCallback } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StatusBar,
  Animated,
  StyleSheet,
  Modal,
  ScrollView,
  FlatList,
  TextInput,
  KeyboardAvoidingView,
  Platform,
  SafeAreaView,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useRoute } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useTheme } from "../context/ThemeContext";
import { customizeTheme, resetCustomization } from "../services/themeService";
import BackButton from "../components/BackButton";

// ─── Paleta de swatches curados ───────────────────────────────────────────────
const SWATCHES = [
  // Púrpuras — identidad Tu Evento
  "#1E0A3C", "#2D1B4E", "#3D2B5E", "#6D28D9", "#7C3AED", "#8B5CF6", "#A78BFA", "#C4B5FD",
  // Rosas y rojos
  "#E91E63", "#FF4081", "#EF4444", "#DC2626", "#FF5252", "#F43F5E",
  // Verdes
  "#059669", "#10B981", "#22C55E", "#16A34A", "#69F0AE",
  // Azules
  "#005FCC", "#1D4ED8", "#3B82F6", "#60A5FA", "#1877F2",
  // Amarillos y naranjas
  "#F59E0B", "#FFEB3B", "#FDE047", "#E65100", "#D97706",
  // Neutros
  "#FFFFFF", "#F5F5F5", "#9CA3AF", "#6B7280", "#374151", "#1F2937", "#111827", "#000000",
];

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

// ─── Regex de validación hex ──────────────────────────────────────────────────
const HEX_REGEX = /^#([A-Fa-f0-9]{3,4}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$/;

// ─── Mini mockup contextual según grupo ──────────────────────────────────────
function ContextPreview({ property, previewColor, colors }) {
  const isBg      = ["background", "surface", "surfaceAlt"].includes(property);
  const isPrimary = ["primary", "primaryDark", "accent"].includes(property);
  const isText    = ["textPrimary", "textSecondary", "textMuted"].includes(property);
  const isState   = ["error", "errorBg", "success", "successBg"].includes(property);

  if (isBg) {
    return (
      <View style={[ctxStyles.bgCard, { backgroundColor: previewColor, borderColor: colors.surfaceAlt }]}>
        <View style={[ctxStyles.bgLine, { backgroundColor: colors.textSecondary + "60" }]} />
        <View style={[ctxStyles.bgLineShort, { backgroundColor: colors.textSecondary + "40" }]} />
      </View>
    );
  }
  if (isPrimary) {
    return (
      <View style={[ctxStyles.primaryBtn, { backgroundColor: previewColor }]}>
        <Text style={[ctxStyles.primaryBtnText, { color: colors.textPrimary }]}>Aa</Text>
      </View>
    );
  }
  if (isText) {
    return (
      <View style={[ctxStyles.textBox, { backgroundColor: colors.surface }]}>
        <Text style={[ctxStyles.textSample, { color: previewColor }]}>Texto</Text>
      </View>
    );
  }
  if (isState) {
    return (
      <View style={[ctxStyles.stateBadge, { backgroundColor: previewColor + "33", borderColor: previewColor + "88" }]}>
        <Text style={[ctxStyles.stateBadgeText, { color: previewColor }]}>Estado</Text>
      </View>
    );
  }
  return null;
}

const ctxStyles = StyleSheet.create({
  bgCard:        { width: 56, height: 56, borderRadius: 10, borderWidth: 1, padding: 8, justifyContent: "center", gap: 5 },
  bgLine:        { height: 6, borderRadius: 3 },
  bgLineShort:   { height: 6, borderRadius: 3, width: "60%" },
  primaryBtn:    { width: 56, height: 32, borderRadius: 8, alignItems: "center", justifyContent: "center" },
  primaryBtnText:{ fontSize: 14, fontWeight: "800" },
  textBox:       { width: 56, height: 32, borderRadius: 8, alignItems: "center", justifyContent: "center" },
  textSample:    { fontSize: 13, fontWeight: "700" },
  stateBadge:    { borderRadius: 8, borderWidth: 1, paddingHorizontal: 8, paddingVertical: 4 },
  stateBadgeText:{ fontSize: 11, fontWeight: "700" },
});

// ─── Fila de propiedad ────────────────────────────────────────────────────────
function PropertyRow({ property, value, colors, styles, onPress }) {
  return (
    <TouchableOpacity onPress={onPress} activeOpacity={0.75} style={styles.propertyRow}>
      <View style={[styles.colorSwatch, { backgroundColor: value, borderColor: colors.surfaceAlt }]} />
      <View style={styles.propertyInfo}>
        <Text style={[styles.propertyLabel, { color: colors.textPrimary }]}>
          {PROPERTY_LABELS[property] ?? property}
        </Text>
        <Text style={[styles.propertyValue, { color: colors.textMuted }]}>{value}</Text>
      </View>
      <Ionicons name="chevron-forward" size={16} color={colors.textMuted} />
    </TouchableOpacity>
  );
}

// ─── Pantalla principal ───────────────────────────────────────────────────────
export default function ThemeCustomizeScreen() {
  const route  = useRoute();
  const insets = useSafeAreaInsets();
  const { colors, palette, applyPalette } = useTheme();

  const { themeName } = route.params ?? {};

  // ── Estado del modal ──
  const [selectedProperty, setSelectedProperty] = useState(null);
  const [modalVisible, setModalVisible]         = useState(false);
  const [previewColor, setPreviewColor]         = useState("#000000");
  const [hexInput, setHexInput]                 = useState("");
  const [hexError, setHexError]                 = useState(false);
  const [applyError, setApplyError]             = useState(null);
  const [loading, setLoading]                   = useState(false);

  // ── Animación de entrada ──
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

  // ── Abrir modal ──
  const openPicker = useCallback((property) => {
    const currentColor = palette?.[property] ?? colors[property] ?? "#000000";
    setSelectedProperty(property);
    setPreviewColor(currentColor);
    setHexInput(currentColor);
    setHexError(false);
    setApplyError(null);
    setModalVisible(true);
  }, [palette, colors]);

  // ── Validar y actualizar desde input hex ──
  const handleHexChange = (text) => {
    setHexInput(text);
    if (HEX_REGEX.test(text)) {
      setPreviewColor(text);
      setHexError(false);
    } else {
      setHexError(true);
    }
  };

  // ── Seleccionar swatch ──
  const handleSwatchPress = (hex) => {
    setPreviewColor(hex);
    setHexInput(hex);
    setHexError(false);
  };

  // ── Aplicar personalización ──
  const handleApply = async () => {
    if (hexError || !selectedProperty) return;
    setLoading(true);
    setApplyError(null);
    try {
      const token = await AsyncStorage.getItem("accessToken");
      const data  = await customizeTheme(selectedProperty, previewColor, token);
      // data es la paleta resuelta completa del backend
      if (data?.palette) {
        applyPalette(data.palette);
      } else if (data && typeof data === "object") {
        // algunos backends devuelven la paleta directamente
        applyPalette(data);
      }
      setModalVisible(false);
    } catch (e) {
      setApplyError("No se pudo aplicar el color. Intenta de nuevo.");
    } finally {
      setLoading(false);
    }
  };

  // ── Resetear propiedad ──
  const handleReset = async () => {
    if (!selectedProperty) return;
    setLoading(true);
    setApplyError(null);
    try {
      const token = await AsyncStorage.getItem("accessToken");
      const data  = await resetCustomization(selectedProperty, token);
      if (data?.palette) {
        applyPalette(data.palette);
      } else if (data && typeof data === "object") {
        applyPalette(data);
      }
      setModalVisible(false);
    } catch (e) {
      setApplyError("No se pudo resetear el color. Intenta de nuevo.");
    } finally {
      setLoading(false);
    }
  };

  // ── Render swatch ──
  const renderSwatch = ({ item }) => {
    const isSelected = item === previewColor;
    return (
      <TouchableOpacity
        onPress={() => handleSwatchPress(item)}
        activeOpacity={0.8}
        style={[
          styles.swatchItem,
          { backgroundColor: item },
          isSelected && styles.swatchSelected,
        ]}
      />
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      {/* Orbes de luz ambiental */}
      <View pointerEvents="none" style={[styles.orb1, { backgroundColor: colors.primary + "1A" }]} />
      <View pointerEvents="none" style={[styles.orb2, { backgroundColor: colors.accent + "12" }]} />

      {/* ── Header ── */}
      <View style={[styles.header, { paddingTop: insets.top + 8, borderBottomColor: colors.primary + "20" }]}>
        <BackButton style={{ marginBottom: 0 }} />
        <View style={styles.headerCenter}>
          <Text style={[styles.headerTitle, { color: colors.textPrimary }]}>
            Personalizar tema
          </Text>
          {themeName ? (
            <Text style={[styles.headerSub, { color: colors.accent }]}>{themeName}</Text>
          ) : null}
        </View>
        <View style={{ width: 40 }} />
      </View>

      {/* ── Lista de propiedades ── */}
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={[styles.scrollContent, { paddingBottom: insets.bottom + 32 }]}
      >
        <Animated.View
          style={{
            opacity: fadeAnim,
            transform: [{ translateY: fadeAnim.interpolate({ inputRange: [0, 1], outputRange: [20, 0] }) }],
          }}
        >
          <Text style={[styles.description, { color: colors.textMuted }]}>
            Toca cualquier propiedad para cambiar su color. Los cambios se aplican al instante y se sincronizan con tu cuenta.
          </Text>

          {PROPERTY_GROUPS.map((group) => (
            <View key={group.label} style={styles.group}>
              <View style={styles.groupHeader}>
                <Ionicons name={group.icon} size={14} color={colors.accent} style={{ marginRight: 6 }} />
                <Text style={[styles.groupLabel, { color: colors.textSecondary }]}>
                  {group.label.toUpperCase()}
                </Text>
              </View>

              <View style={[styles.groupCard, { backgroundColor: colors.surface + "CC", borderColor: colors.primary + "30" }]}>
                {group.properties.map((prop, idx) => {
                  const value  = palette?.[prop] ?? colors[prop] ?? "#000000";
                  const isLast = idx === group.properties.length - 1;
                  return (
                    <View key={prop}>
                      <PropertyRow
                        property={prop}
                        value={value}
                        colors={colors}
                        styles={styles}
                        onPress={() => openPicker(prop)}
                      />
                      {!isLast && (
                        <View style={[styles.separator, { backgroundColor: colors.primary + "18" }]} />
                      )}
                    </View>
                  );
                })}
              </View>
            </View>
          ))}

          <View style={[styles.infoBox, { backgroundColor: colors.surface + "80", borderColor: colors.primary + "25" }]}>
            <Ionicons name="information-circle-outline" size={18} color={colors.accent} style={{ marginTop: 1 }} />
            <Text style={[styles.infoText, { color: colors.textSecondary }]}>
              Puedes restablecer cada color al valor original del tema en cualquier momento.
            </Text>
          </View>
        </Animated.View>
      </ScrollView>

      {/* ── Modal color picker ── */}
      <Modal
        visible={modalVisible}
        animationType="slide"
        transparent
        statusBarTranslucent
        onRequestClose={() => setModalVisible(false)}
      >
        <KeyboardAvoidingView
          behavior={Platform.OS === "ios" ? "padding" : "height"}
          style={styles.modalOverlay}
        >
          <TouchableOpacity
            style={styles.modalBackdrop}
            activeOpacity={1}
            onPress={() => setModalVisible(false)}
          />

          <SafeAreaView style={[styles.modalSheet, { backgroundColor: colors.surface }]}>
            {/* ── Header del modal ── */}
            <View style={[styles.modalHeader, { borderBottomColor: colors.primary + "20" }]}>
              <Text style={[styles.modalTitle, { color: colors.textPrimary }]}>
                {selectedProperty ? (PROPERTY_LABELS[selectedProperty] ?? selectedProperty) : ""}
              </Text>
              <TouchableOpacity
                onPress={() => setModalVisible(false)}
                hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
              >
                <Ionicons name="close" size={22} color={colors.textSecondary} />
              </TouchableOpacity>
            </View>

            <ScrollView
              showsVerticalScrollIndicator={false}
              keyboardShouldPersistTaps="handled"
              contentContainerStyle={styles.modalScroll}
            >
              {/* ── Preview en tiempo real ── */}
              <View style={styles.previewRow}>
                {/* Rectángulo de color */}
                <View style={styles.previewBlock}>
                  <View style={[styles.previewRect, { backgroundColor: previewColor }]} />
                  <Text style={[styles.previewHex, { color: colors.textMuted }]}>{previewColor}</Text>
                </View>

                {/* Mini mockup contextual */}
                <View style={styles.previewContext}>
                  <Text style={[styles.previewContextLabel, { color: colors.textMuted }]}>Vista previa</Text>
                  {selectedProperty && (
                    <ContextPreview
                      property={selectedProperty}
                      previewColor={previewColor}
                      colors={colors}
                    />
                  )}
                </View>
              </View>

              {/* ── Grilla de swatches ── */}
              <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>COLORES</Text>
              <FlatList
                data={SWATCHES}
                renderItem={renderSwatch}
                keyExtractor={(item) => item}
                numColumns={8}
                scrollEnabled={false}
                columnWrapperStyle={styles.swatchRow}
                style={styles.swatchGrid}
              />

              {/* ── Input hex manual ── */}
              <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>HEX MANUAL</Text>
              <View style={[
                styles.hexInputWrapper,
                {
                  backgroundColor: colors.surfaceAlt,
                  borderColor: hexError ? colors.error : colors.primary + "40",
                },
              ]}>
                <View style={[styles.hexInputSwatch, { backgroundColor: HEX_REGEX.test(hexInput) ? hexInput : colors.surfaceAlt }]} />
                <TextInput
                  value={hexInput}
                  onChangeText={handleHexChange}
                  placeholder="#RRGGBB"
                  placeholderTextColor={colors.textMuted}
                  autoCapitalize="characters"
                  autoCorrect={false}
                  maxLength={9}
                  style={[styles.hexInput, { color: colors.textPrimary }]}
                />
              </View>
              {hexError && (
                <Text style={[styles.hexErrorText, { color: colors.error }]}>
                  Formato inválido — usa #RGB o #RRGGBB
                </Text>
              )}

              {/* ── Error de red ── */}
              {applyError && (
                <View style={[styles.applyErrorBox, { backgroundColor: colors.errorBg, borderColor: colors.error + "44" }]}>
                  <Ionicons name="alert-circle-outline" size={15} color={colors.error} />
                  <Text style={[styles.applyErrorText, { color: colors.error }]}>{applyError}</Text>
                </View>
              )}

              {/* ── Botones de acción ── */}
              <View style={styles.actionRow}>
                <TouchableOpacity
                  onPress={handleReset}
                  disabled={loading || hexError}
                  activeOpacity={0.75}
                  style={[
                    styles.btnReset,
                    {
                      borderColor: colors.surfaceAlt,
                      opacity: loading || hexError ? 0.45 : 1,
                    },
                  ]}
                >
                  <Ionicons name="refresh-outline" size={16} color={colors.textSecondary} style={{ marginRight: 6 }} />
                  <Text style={[styles.btnResetText, { color: colors.textSecondary }]}>Resetear</Text>
                </TouchableOpacity>

                <TouchableOpacity
                  onPress={handleApply}
                  disabled={loading || hexError}
                  activeOpacity={0.75}
                  style={[
                    styles.btnApply,
                    {
                      backgroundColor: colors.primary,
                      opacity: loading || hexError ? 0.45 : 1,
                      shadowColor: colors.primary,
                    },
                  ]}
                >
                  <Ionicons name="checkmark" size={16} color={colors.textPrimary} style={{ marginRight: 6 }} />
                  <Text style={[styles.btnApplyText, { color: colors.textPrimary }]}>
                    {loading ? "Aplicando…" : "Aplicar"}
                  </Text>
                </TouchableOpacity>
              </View>
            </ScrollView>
          </SafeAreaView>
        </KeyboardAvoidingView>
      </Modal>
    </View>
  );
}

// ─── Estilos ──────────────────────────────────────────────────────────────────
function makeStyles(colors) {
  return StyleSheet.create({
    container: { flex: 1 },
    orb1: { position: "absolute", top: -50, right: -50, width: 220, height: 220, borderRadius: 110 },
    orb2: { position: "absolute", bottom: 100, left: -60, width: 200, height: 200, borderRadius: 100 },

    // Header
    header:       { paddingBottom: 12, paddingHorizontal: 24, flexDirection: "row", alignItems: "center", borderBottomWidth: 1 },
    headerCenter: { flex: 1, alignItems: "center" },
    headerTitle:  { fontSize: 18, fontWeight: "800" },
    headerSub:    { fontSize: 12, fontWeight: "600", marginTop: 2, opacity: 0.85 },

    // Scroll
    scrollContent: { paddingHorizontal: 24, paddingTop: 24 },
    description:   { fontSize: 13, lineHeight: 20, marginBottom: 24 },

    // Grupos
    group:       { marginBottom: 20 },
    groupHeader: { flexDirection: "row", alignItems: "center", marginBottom: 8 },
    groupLabel:  { fontSize: 11, fontWeight: "700", letterSpacing: 1.2 },
    groupCard:   { borderRadius: 16, borderWidth: 1, overflow: "hidden" },

    // Fila de propiedad
    propertyRow:   { flexDirection: "row", alignItems: "center", paddingHorizontal: 16, paddingVertical: 14 },
    colorSwatch:   { width: 36, height: 36, borderRadius: 10, borderWidth: 1.5, marginRight: 14 },
    propertyInfo:  { flex: 1 },
    propertyLabel: { fontSize: 14, fontWeight: "600", marginBottom: 2 },
    propertyValue: { fontSize: 12, fontFamily: "monospace" },
    separator:     { height: 1, marginHorizontal: 16 },

    // Info box
    infoBox:  { flexDirection: "row", alignItems: "flex-start", gap: 10, borderRadius: 12, borderWidth: 1, padding: 14, marginTop: 4 },
    infoText: { fontSize: 12, lineHeight: 18, flex: 1 },

    // Modal
    modalOverlay:  { flex: 1, justifyContent: "flex-end" },
    modalBackdrop: { ...StyleSheet.absoluteFillObject, backgroundColor: "#00000077" },
    modalSheet:    { borderTopLeftRadius: 24, borderTopRightRadius: 24, maxHeight: "88%", overflow: "hidden" },
    modalHeader:   { flexDirection: "row", alignItems: "center", justifyContent: "space-between", paddingHorizontal: 20, paddingVertical: 16, borderBottomWidth: 1 },
    modalTitle:    { fontSize: 16, fontWeight: "800" },
    modalScroll:   { paddingHorizontal: 20, paddingBottom: 32 },

    // Preview
    previewRow:          { flexDirection: "row", gap: 16, marginTop: 20, marginBottom: 24, alignItems: "flex-start" },
    previewBlock:        { flex: 1, gap: 8 },
    previewRect:         { height: 80, borderRadius: 14 },
    previewHex:          { fontSize: 12, textAlign: "center", fontFamily: "monospace" },
    previewContext:      { alignItems: "center", gap: 8 },
    previewContextLabel: { fontSize: 10, fontWeight: "700", letterSpacing: 0.8, textTransform: "uppercase" },

    // Swatches
    sectionLabel: { fontSize: 10, fontWeight: "700", letterSpacing: 1.2, marginBottom: 10 },
    swatchGrid:   { marginBottom: 20 },
    swatchRow:    { justifyContent: "space-between", marginBottom: 8 },
    swatchItem:   { width: 32, height: 32, borderRadius: 16, borderWidth: 1, borderColor: "rgba(255,255,255,0.15)" },
    swatchSelected: { borderWidth: 2.5, borderColor: "#FFFFFF", transform: [{ scale: 1.2 }] },

    // Hex input
    hexInputWrapper: { flexDirection: "row", alignItems: "center", borderRadius: 12, borderWidth: 1.5, paddingHorizontal: 12, paddingVertical: 10, marginBottom: 6, gap: 10 },
    hexInputSwatch:  { width: 24, height: 24, borderRadius: 6, borderWidth: 1, borderColor: "rgba(255,255,255,0.2)" },
    hexInput:        { flex: 1, fontSize: 15, fontFamily: "monospace", letterSpacing: 1 },
    hexErrorText:    { fontSize: 12, marginBottom: 12 },

    // Error de red
    applyErrorBox:  { flexDirection: "row", alignItems: "center", gap: 8, borderRadius: 10, borderWidth: 1, padding: 10, marginBottom: 12 },
    applyErrorText: { fontSize: 12, flex: 1 },

    // Botones
    actionRow:    { flexDirection: "row", gap: 12, marginTop: 8 },
    btnReset:     { flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center", borderRadius: 14, borderWidth: 1.5, paddingVertical: 14 },
    btnResetText: { fontSize: 14, fontWeight: "700" },
    btnApply:     { flex: 2, flexDirection: "row", alignItems: "center", justifyContent: "center", borderRadius: 14, paddingVertical: 14, shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.4, shadowRadius: 10, elevation: 8 },
    btnApplyText: { fontSize: 14, fontWeight: "700" },
  });
}
