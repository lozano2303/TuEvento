import { useState, useEffect } from "react";
import { View, Text, TouchableOpacity, ScrollView, ActivityIndicator, SafeAreaView } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useTheme } from "../context/ThemeContext";
import { useAuth } from "../context/AuthContext";
import { getThemes, activateTheme } from "../services/themeService";
import ScreenLayout from "../components/ScreenLayout";

const THEME_PREVIEWS = {
  DARK:       { background: "#1E0A3C", primary: "#7C3AED", accent: "#A78BFA" },
  LIGHT:      { background: "#FFFFFF", primary: "#7C3AED", accent: "#8B5CF6" },
  VIBRANT:    { background: "#0D0D0D", primary: "#FF4081", accent: "#FFEB3B" },
  ACCESSIBLE: { background: "#FFFFFF", primary: "#005FCC", accent: "#E65100" }
};

export default function ProfileScreen() {
  const insets = useSafeAreaInsets();
  const { palette, refreshPalette } = useTheme();
  const { user } = useAuth();
  const [themes, setThemes] = useState([]);
  const [activeThemeId, setActiveThemeId] = useState(null);
  const [loadingThemes, setLoadingThemes] = useState(true);
  const [activating, setActivating] = useState(null);

  useEffect(() => {
    loadThemes();
  }, []);

  const loadThemes = async () => {
    try {
      setLoadingThemes(true);
      const themesData = await getThemes();
      setThemes(themesData);
      
      const activeTheme = themesData.find(t => 
        t.name === palette.themeName || 
        (palette.background === THEME_PREVIEWS[t.name]?.background)
      );
      if (activeTheme) {
        setActiveThemeId(activeTheme.id);
      }
    } catch (err) {
      console.error("Error al cargar temas:", err);
    } finally {
      setLoadingThemes(false);
    }
  };

  const handleThemeSelect = async (themeId) => {
    if (activating || themeId === activeThemeId) return;
    
    setActivating(themeId);
    try {
      const token = await AsyncStorage.getItem("accessToken");
      if (!token) return;

      await activateTheme(themeId, token);
      await refreshPalette();
      setActiveThemeId(themeId);
    } catch (err) {
      console.error("Error al activar tema:", err);
    } finally {
      setActivating(null);
    }
  };

  const getInitials = (name) => {
    if (!name) return "U";
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  };

  const getRoleBadge = (role) => {
    switch (role) {
      case "ADMIN":
        return { label: "ADMIN", color: palette.error, bg: palette.error + "22" };
      case "ORGANIZER":
        return { label: "ORGANIZADOR", color: "#3B82F6", bg: "#3B82F633" };
      default:
        return { label: "USUARIO", color: palette.accent, bg: palette.accent + "22" };
    }
  };

  const roleBadge = getRoleBadge(user?.role);
  const StyleSheet = {
    container: { flex: 1 },
    scrollContent: { 
      paddingHorizontal: 24, 
      paddingTop: insets.top + 24,
      paddingBottom: insets.bottom + 24 
    },
    header: { 
      alignItems: "center", 
      marginBottom: 32 
    },
    avatar: {
      width: 96,
      height: 96,
      borderRadius: 48,
      backgroundColor: palette.primary,
      alignItems: "center",
      justifyContent: "center",
      marginBottom: 16,
      borderWidth: 3,
      borderColor: palette.surfaceAlt
    },
    avatarText: {
      color: "#FFFFFF",
      fontSize: 36,
      fontWeight: "800"
    },
    fullName: {
      color: palette.textPrimary,
      fontSize: 24,
      fontWeight: "800",
      marginBottom: 6
    },
    alias: {
      color: palette.textSecondary,
      fontSize: 15,
      marginBottom: 12
    },
    roleBadge: {
      backgroundColor: roleBadge.bg,
      borderRadius: 20,
      paddingHorizontal: 16,
      paddingVertical: 8,
      borderWidth: 1,
      borderColor: roleBadge.color
    },
    roleText: {
      color: roleBadge.color,
      fontSize: 12,
      fontWeight: "800",
      letterSpacing: 0.5
    },
    section: { marginBottom: 32 },
    sectionTitle: {
      color: palette.textPrimary,
      fontSize: 20,
      fontWeight: "700",
      marginBottom: 8
    },
    sectionSubtitle: {
      color: palette.textSecondary,
      fontSize: 14,
      marginBottom: 20
    },
    themeCard: (isActive) => ({
      backgroundColor: palette.surface,
      borderRadius: 16,
      padding: 20,
      marginBottom: 16,
      borderWidth: 2,
      borderColor: isActive ? palette.primary : palette.surfaceAlt
    }),
    themeHeader: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: 16
    },
    themeName: {
      color: palette.textPrimary,
      fontSize: 17,
      fontWeight: "700"
    },
    activeBadge: {
      backgroundColor: palette.primary + "22",
      borderRadius: 20,
      paddingHorizontal: 12,
      paddingVertical: 6,
      borderWidth: 1,
      borderColor: palette.primary
    },
    activeBadgeText: {
      color: palette.primary,
      fontSize: 11,
      fontWeight: "800",
      letterSpacing: 0.5
    },
    previewContainer: {
      flexDirection: "row",
      gap: 12
    },
    previewCircle: (color) => ({
      width: 56,
      height: 56,
      borderRadius: 12,
      backgroundColor: color,
      borderWidth: 1,
      borderColor: palette.surfaceAlt
    })
  };

  return (
    <ScreenLayout>
      <SafeAreaView style={StyleSheet.container}>
        <ScrollView
          style={{ flex: 1 }}
          contentContainerStyle={StyleSheet.scrollContent}
        >
          {/* Información del usuario */}
          <View style={StyleSheet.header}>
            <View style={StyleSheet.avatar}>
              <Text style={StyleSheet.avatarText}>
                {getInitials(user?.fullName)}
              </Text>
            </View>
            <Text style={StyleSheet.fullName}>{user?.fullName || "Usuario"}</Text>
            <Text style={StyleSheet.alias}>@{user?.alias || "usuario"}</Text>
            <View style={StyleSheet.roleBadge}>
              <Text style={StyleSheet.roleText}>{roleBadge.label}</Text>
            </View>
          </View>

          {/* Apariencia */}
          <View style={StyleSheet.section}>
            <Text style={StyleSheet.sectionTitle}>Apariencia</Text>
            <Text style={StyleSheet.sectionSubtitle}>
              Personaliza el tema de la aplicación
            </Text>

            {loadingThemes ? (
              <View style={{ paddingVertical: 40, alignItems: "center" }}>
                <ActivityIndicator size="large" color={palette.primary} />
              </View>
            ) : (
              themes.map((theme) => {
                const preview = THEME_PREVIEWS[theme.name] || THEME_PREVIEWS.DARK;
                const isActive = theme.id === activeThemeId;
                const isActivating = activating === theme.id;

                return (
                  <TouchableOpacity
                    key={theme.id}
                    onPress={() => handleThemeSelect(theme.id)}
                    disabled={isActivating || loadingThemes}
                    activeOpacity={0.85}
                    style={StyleSheet.themeCard(isActive)}
                  >
                    <View style={StyleSheet.themeHeader}>
                      <Text style={StyleSheet.themeName}>{theme.name}</Text>
                      {isActivating ? (
                        <ActivityIndicator size="small" color={palette.primary} />
                      ) : isActive ? (
                        <View style={StyleSheet.activeBadge}>
                          <Text style={StyleSheet.activeBadgeText}>ACTIVO</Text>
                        </View>
                      ) : null}
                    </View>

                    <View style={StyleSheet.previewContainer}>
                      <View style={StyleSheet.previewCircle(preview.background)} />
                      <View style={StyleSheet.previewCircle(preview.primary)} />
                      <View style={StyleSheet.previewCircle(preview.accent)} />
                    </View>

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
              })
            )}
          </View>
        </ScrollView>
      </SafeAreaView>
    </ScreenLayout>
  );
}
