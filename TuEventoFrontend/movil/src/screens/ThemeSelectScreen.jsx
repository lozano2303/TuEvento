import { useState, useEffect } from "react";
import { View, Text, TouchableOpacity, ScrollView, ActivityIndicator, SafeAreaView } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useTheme } from "../context/ThemeContext";
import { getThemes, activateTheme } from "../services/themeService";
import BackButton from "../components/BackButton";
import ScreenLayout from "../components/ScreenLayout";

const THEME_PREVIEWS = {
  DARK:       { background: "#1E0A3C", primary: "#7C3AED", accent: "#A78BFA" },
  LIGHT:      { background: "#FFFFFF", primary: "#7C3AED", accent: "#8B5CF6" },
  VIBRANT:    { background: "#0D0D0D", primary: "#FF4081", accent: "#FFEB3B" },
  ACCESSIBLE: { background: "#FFFFFF", primary: "#005FCC", accent: "#E65100" }
};

export default function ThemeSelectScreen() {
  const insets = useSafeAreaInsets();
  const { palette, refreshPalette } = useTheme();
  const [themes, setThemes] = useState([]);
  const [activeThemeId, setActiveThemeId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activating, setActivating] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadThemes();
  }, []);

  const loadThemes = async () => {
    try {
      setLoading(true);
      const themesData = await getThemes();
      setThemes(themesData);
      
      // Identificar tema activo por el nombre del palette actual
      const activeTheme = themesData.find(t => 
        t.name === palette.themeName || 
        (palette.background === THEME_PREVIEWS[t.name]?.background)
      );
      if (activeTheme) {
        setActiveThemeId(activeTheme.id);
      }
    } catch (err) {
      setError("Error al cargar los temas");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleThemeSelect = async (themeId) => {
    if (activating || themeId === activeThemeId) return;
    
    setActivating(themeId);
    try {
      const token = await AsyncStorage.getItem("accessToken");
      if (!token) {
        console.warn('[ThemeSelectScreen] No token available');
        setError("No se encontró el token de autenticación");
        return;
      }

      await activateTheme(themeId, token);
      await refreshPalette();
      setActiveThemeId(themeId);
    } catch (err) {
      setError("Error al activar el tema");
      console.error(err);
    } finally {
      setActivating(null);
    }
  };

  return (
    <ScreenLayout>
      <SafeAreaView style={{ flex: 1 }}>
        <ScrollView
          style={{ flex: 1 }}
          contentContainerStyle={{ 
            paddingHorizontal: 28, 
            paddingTop: insets.top + 16,
            paddingBottom: insets.bottom + 24 
          }}
        >
          {/* Header */}
          <View style={{ flexDirection: "row", alignItems: "center", marginBottom: 32 }}>
            <BackButton style={{ marginBottom: 0 }} />
            <View style={{ flex: 1, alignItems: "center" }}>
              <Text style={{ color: palette.textPrimary, fontSize: 26, fontWeight: "800" }}>
                Apariencia
              </Text>
            </View>
            <View style={{ width: 40 }} />
          </View>

          {/* Descripción */}
          <View style={{ 
            backgroundColor: palette.surface, 
            borderRadius: 16, 
            padding: 16, 
            marginBottom: 24,
            borderWidth: 1,
            borderColor: palette.surfaceAlt
          }}>
            <View style={{ flexDirection: "row", alignItems: "center", marginBottom: 8 }}>
              <Ionicons name="color-palette-outline" size={20} color={palette.primary} />
              <Text style={{ 
                color: palette.textPrimary, 
                fontSize: 15, 
                fontWeight: "700",
                marginLeft: 8
              }}>
                Personaliza tu experiencia
              </Text>
            </View>
            <Text style={{ color: palette.textSecondary, fontSize: 13, lineHeight: 20 }}>
              Elige el tema que mejor se adapte a tus preferencias. Los cambios se aplicarán inmediatamente.
            </Text>
          </View>

          {/* Error */}
          {error && (
            <View style={{ 
              backgroundColor: palette.errorBg, 
              borderRadius: 12, 
              padding: 12, 
              marginBottom: 16,
              borderWidth: 1,
              borderColor: palette.error
            }}>
              <Text style={{ color: palette.error, fontSize: 13, textAlign: "center" }}>
                {error}
              </Text>
            </View>
          )}

          {/* Loading */}
          {loading ? (
            <View style={{ paddingVertical: 40, alignItems: "center" }}>
              <ActivityIndicator size="large" color={palette.primary} />
              <Text style={{ color: palette.textMuted, fontSize: 14, marginTop: 12 }}>
                Cargando temas...
              </Text>
            </View>
          ) : (
            /* Temas */
            <View style={{ gap: 16 }}>
              {themes.map((theme) => {
                const preview = THEME_PREVIEWS[theme.name] || THEME_PREVIEWS.DARK;
                const isActive = theme.id === activeThemeId;
                const isActivating = activating === theme.id;

                return (
                  <TouchableOpacity
                    key={theme.id}
                    onPress={() => handleThemeSelect(theme.id)}
                    disabled={isActivating || loading}
                    activeOpacity={0.85}
                    style={{
                      backgroundColor: palette.surface,
                      borderRadius: 16,
                      padding: 20,
                      borderWidth: 2,
                      borderColor: isActive ? palette.primary : palette.surfaceAlt,
                      opacity: isActivating ? 0.7 : 1
                    }}
                  >
                    {/* Header de la tarjeta */}
                    <View style={{ 
                      flexDirection: "row", 
                      alignItems: "center", 
                      justifyContent: "space-between",
                      marginBottom: 16
                    }}>
                      <Text style={{ 
                        color: palette.textPrimary, 
                        fontSize: 17, 
                        fontWeight: "700" 
                      }}>
                        {theme.name}
                      </Text>
                      {isActivating ? (
                        <ActivityIndicator size="small" color={palette.primary} />
                      ) : isActive ? (
                        <View style={{
                          backgroundColor: palette.primary + "22",
                          borderRadius: 20,
                          paddingHorizontal: 12,
                          paddingVertical: 6,
                          borderWidth: 1,
                          borderColor: palette.primary
                        }}>
                          <Text style={{ 
                            color: palette.primary, 
                            fontSize: 11, 
                            fontWeight: "800",
                            letterSpacing: 0.5
                          }}>
                            ACTIVO
                          </Text>
                        </View>
                      ) : null}
                    </View>

                    {/* Preview de colores */}
                    <View style={{ flexDirection: "row", gap: 12 }}>
                      <View style={{
                        width: 56,
                        height: 56,
                        borderRadius: 12,
                        backgroundColor: preview.background,
                        borderWidth: 1,
                        borderColor: palette.surfaceAlt
                      }} />
                      <View style={{
                        width: 56,
                        height: 56,
                        borderRadius: 12,
                        backgroundColor: preview.primary,
                        borderWidth: 1,
                        borderColor: palette.surfaceAlt
                      }} />
                      <View style={{
                        width: 56,
                        height: 56,
                        borderRadius: 12,
                        backgroundColor: preview.accent,
                        borderWidth: 1,
                        borderColor: palette.surfaceAlt
                      }} />
                    </View>

                    {/* Indicador de selección */}
                    {isActive && (
                      <View style={{ 
                        flexDirection: "row", 
                        alignItems: "center", 
                        marginTop: 12,
                        gap: 6
                      }}>
                        <Ionicons name="checkmark-circle" size={16} color={palette.primary} />
                        <Text style={{ 
                          color: palette.primary, 
                          fontSize: 12, 
                          fontWeight: "600" 
                        }}>
                          Tema actual
                        </Text>
                      </View>
                    )}
                  </TouchableOpacity>
                );
              })}
            </View>
          )}
        </ScrollView>
      </SafeAreaView>
    </ScreenLayout>
  );
}
