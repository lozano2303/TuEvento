import { useState } from "react";
import { View, Text, TextInput, TouchableOpacity, ScrollView } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { mapErrorMessage } from "../utils/errorMessages";
import BackButton from "../components/BackButton";
import ScreenLayout from "../components/ScreenLayout";
import { useTheme } from "../context/ThemeContext";

const API_URL = process.env.EXPO_PUBLIC_API_URL;
const GMAIL_PATTERN = /^[a-zA-Z0-9._%+\-]+@gmail\.com$/;

export default function ForgotPasswordScreen() {
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const { palette } = useTheme();
  const [email, setEmail] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const validate = () => {
    if (!email.trim()) { setError("El correo es requerido"); return false; }
    if (!GMAIL_PATTERN.test(email.trim())) { setError("Solo se aceptan correos @gmail.com"); return false; }
    return true;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`${API_URL}/auth/recover-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim() }),
      });
      const data = await response.json();
      if (!data.success) throw new Error(data.message);
      navigation.navigate("ResetPassword", { email: email.trim() });
    } catch (e) {
      setError(mapErrorMessage(e.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScreenLayout>
      <ScrollView
        style={{ paddingTop: insets.top > 0 ? insets.top + 16 : 0 }}
        contentContainerStyle={{ flexGrow: 1, paddingHorizontal: 28, paddingBottom: insets.bottom + 24 }}
        keyboardShouldPersistTaps="handled"
      >
        <View style={{ flexDirection: "row", alignItems: "center", marginBottom: 32 }}>
          <BackButton style={{ marginBottom: 0 }} />
          <View style={{ flex: 1, alignItems: "center" }}>
            <Text style={{ color: palette.textPrimary, fontSize: 26, fontWeight: "800" }}>
              Recuperar acceso
            </Text>
          </View>
          <View style={{ width: 40 }} />
        </View>

        {/* Header */}
        <View style={{ alignItems: "center", marginBottom: 32 }}>
          <View style={{ width: 72, height: 72, borderRadius: 20, backgroundColor: palette.surfaceAlt, alignItems: "center", justifyContent: "center", marginBottom: 16 }}>
            <Ionicons name="lock-closed-outline" size={32} color={palette.primary} />
          </View>
          <Text style={{ color: palette.textSecondary, fontSize: 14, textAlign: "center", lineHeight: 22 }}>
            Ingresa tu correo y te enviaremos un código para restablecer tu contraseña.
          </Text>
        </View>

        {/* Email */}
        <Text style={{ color: palette.textPrimary, fontWeight: "600", marginBottom: 8 }}>Correo electrónico</Text>
        <View style={{ backgroundColor: palette.surface, borderRadius: 12, borderWidth: error ? 1 : 0, borderColor: palette.error }}>
          <TextInput
            placeholder="tucorreo@gmail.com"
            placeholderTextColor={palette.textMuted}
            value={email}
            onChangeText={(t) => { setEmail(t); setError(null); }}
            style={{ color: palette.textPrimary, padding: 16, fontSize: 15 }}
            keyboardType="email-address"
            autoCapitalize="none"
          />
        </View>
        {error && <Text style={{ color: palette.error, fontSize: 13, marginTop: 4 }}>{error}</Text>}

        {/* Botón */}
        <TouchableOpacity
          onPress={handleSubmit}
          disabled={loading}
          activeOpacity={0.85}
          style={{ borderRadius: 14, overflow: "hidden", marginTop: 32, shadowColor: palette.primary, shadowOffset: { width: 0, height: 6 }, shadowOpacity: 0.5, shadowRadius: 12, elevation: 8, opacity: loading ? 0.7 : 1 }}
        >
          <LinearGradient colors={palette.gradientPrimary} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={{ paddingVertical: 16, alignItems: "center" }}>
            <Text style={{ color: palette.textPrimary, fontSize: 16, fontWeight: "700" }}>
              {loading ? "Enviando..." : "Enviar código"}
            </Text>
          </LinearGradient>
        </TouchableOpacity>
      </ScrollView>
    </ScreenLayout>
  );
}
