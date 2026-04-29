import { useRef, useEffect } from "react";
import {
  View, Text, ScrollView, TouchableOpacity,
  StatusBar, Animated,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import BackButton from "../components/BackButton";

export default function SettingsScreen() {
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
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

          {/* Cards de temas */}
          {themes.map((theme) => {
            const isActive = theme.id === activeThemeId;

            return (
              <TouchableOpacity
                key={theme.id}
                onPress={() => applyTheme(theme.id)}
                activeOpacity={0.75}
                style={{
                  flexDirection: "row",
                  alignItems: "center",
                  backgroundColor: isActive
                    ? colors.primary + "20"
                    : colors.surface + "CC",
                  borderRadius: 16,
                  borderWidth: 1.5,
                  borderColor: isActive
                    ? colors.primary
                    : colors.primary + "30",
                  padding: 16,
                  marginBottom: 12,
                }}
              >
                {/* Emoji del tema */}
                <View
                  style={{
                    width: 44,
                    height: 44,
                    borderRadius: 12,
                    backgroundColor: isActive
                      ? colors.primary + "30"
                      : colors.surface,
                    borderWidth: 1,
                    borderColor: isActive
                      ? colors.primary + "60"
                      : colors.surfaceAlt,
                    alignItems: "center",
                    justifyContent: "center",
                    marginRight: 14,
                  }}
                >
                  <Text style={{ fontSize: 22 }}>{theme.emoji}</Text>
                </View>

                {/* Info del tema */}
                <View style={{ flex: 1 }}>
                  <View
                    style={{
                      flexDirection: "row",
                      alignItems: "center",
                      gap: 8,
                      marginBottom: 4,
                    }}
                  >
                    <Text
                      style={{
                        color: colors.textPrimary,
                        fontSize: 15,
                        fontWeight: "700",
                      }}
                    >
                      {theme.name}
                    </Text>
                    {isActive && (
                      <View
                        style={{
                          backgroundColor: colors.primary + "30",
                          borderRadius: 10,
                          paddingHorizontal: 8,
                          paddingVertical: 2,
                          borderWidth: 1,
                          borderColor: colors.primary + "60",
                        }}
                      >
                        <Text
                          style={{
                            color: colors.primary,
                            fontSize: 10,
                            fontWeight: "800",
                            letterSpacing: 0.5,
                          }}
                        >
                          ACTIVO
                        </Text>
                      </View>
                    )}
                  </View>
                  <Text
                    style={{
                      color: colors.textMuted,
                      fontSize: 12,
                      lineHeight: 17,
                    }}
                  >
                    {theme.description}
                  </Text>

                  {/* Preview de colores */}
                  <View
                    style={{
                      flexDirection: "row",
                      gap: 6,
                      marginTop: 10,
                    }}
                  >
                    {Object.values(theme.preview).map((hex, i) => (
                      <View
                        key={i}
                        style={{
                          width: 20,
                          height: 20,
                          borderRadius: 10,
                          backgroundColor: hex,
                          borderWidth: 1,
                          borderColor: colors.surfaceAlt,
                        }}
                      />
                    ))}
                  </View>
                </View>

                {/* Indicador de selección */}
                <View
                  style={{
                    width: 24,
                    height: 24,
                    borderRadius: 12,
                    borderWidth: 2,
                    borderColor: isActive ? colors.primary : colors.surfaceAlt,
                    backgroundColor: isActive ? colors.primary : "transparent",
                    alignItems: "center",
                    justifyContent: "center",
                    marginLeft: 12,
                  }}
                >
                  {isActive && (
                    <Ionicons
                      name="checkmark"
                      size={14}
                      color={colors.textPrimary}
                    />
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
