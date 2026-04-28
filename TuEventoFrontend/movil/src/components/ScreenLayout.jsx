import { View, StatusBar, KeyboardAvoidingView, Platform } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { useTheme } from "../context/ThemeContext";

/**
 * Wrapper base para todas las pantallas.
 * Maneja el fondo degradado, StatusBar y KeyboardAvoidingView
 * sin que el gradiente participe en el layout del teclado.
 */
export default function ScreenLayout({ children }) {
  const { palette } = useTheme();

  return (
    <View style={{ flex: 1, backgroundColor: palette.background }}>
      <StatusBar barStyle="light-content" backgroundColor={palette.background} />
      <LinearGradient
        colors={palette.gradientBg}
        style={{ position: "absolute", top: 0, left: 0, right: 0, bottom: 0 }}
      />
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === "ios" ? "padding" : "height"}
      >
        {children}
      </KeyboardAvoidingView>
    </View>
  );
}
