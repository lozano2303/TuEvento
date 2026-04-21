import { useState } from "react";
import { View, Text, TextInput, TouchableOpacity, ScrollView } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { mapErrorMessage } from "../utils/errorMessages";
import ScreenLayout from "../components/ScreenLayout";
import { colors } from "../theme";

const API_URL = process.env.EXPO_PUBLIC_API_URL;
const GMAIL_PATTERN = /^[a-zA-Z0-9._%+\-]+@gmail\.com$/;

export default function ForgotPasswordScreen() {
  const navigation = useNavigation();
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
        contentContainerStyle={{ flexGrow: 1, paddingHorizontal: 28, paddingTop: 60 }}
        keyboardShouldPersistTaps="handled"
      >
        {/* Botón volver */}
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          style={{ width: 40, height: 40, borderRadius: 20, backgroundColor: colors.surface, alignItems: "center", justifyContent: "center", marginBottom: 24, borderWidth: 1, borderColor: colors.surfaceAlt }}
        >
          <Ionicons name="arrow-back" size={20} color={colors.textPrimary} />
        </TouchableOpacity>

        {/* Header */}
        <View style={{ alignItems: "center", marginBottom: 32 }}>
          <View style={{ width: 72, height: 72, borderRadius: 20, backgroundColor: colors.surfaceAlt, alignItems: "center", justifyContent: "center", marginBottom: 16 }}>
            <Ionicons name="lock-closed-outline" size={32} color={colors.primary} />
          </View>
          <Text style={{ color: colors.textPrimary, fontSize: 26, fontWeight: "800", marginBottom: 8 }}>Recuperar acceso</Text>
          <Text style={{ color: colors.textSecondary, fontSize: 14, textAlign: "center", lineHeight: 22 }}>
            Ingresa tu correo y te enviaremos un código para restablecer tu contraseña.
          </Text>
        </View>

        {/* Email */}
        <Text style={{ color: colors.textPrimary, fontWeight: "600", marginBottom: 8 }}>Correo electrónico</Text>
        <View style={{ backgroundColor: colors.surface, borderRadius: 12, borderWidth: error ? 1 : 0, borderColor: colors.error }}>
          <TextInput
            placeholder="tucorreo@gmail.com"
            placeholderTextColor={colors.textMuted}
            value={email}
            onChangeText={(t) => { setEmail(t); setError(null); }}
            style={{ color: colors.textPrimary, padding: 16, fontSize: 15 }}
            keyboardType="email-address"
            autoCapitalize="none"
          />
        </View>
        {error && <Text style={{ color: colors.error, fontSize: 13, marginTop: 4 }}>{error}</Text>}

        {/* Botón */}
        <TouchableOpacity
          onPress={handleSubmit}
          disabled={loading}
          activeOpacity={0.85}
          style={{ borderRadius: 14, overflow: "hidden", marginTop: 32, shadowColor: colors.primary, shadowOffset: { width: 0, height: 6 }, shadowOpacity: 0.5, shadowRadius: 12, elevation: 8, opacity: loading ? 0.7 : 1 }}
        >
          <LinearGradient colors={colors.gradientPrimary} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={{ paddingVertical: 16, alignItems: "center" }}>
            <Text style={{ color: colors.textPrimary, fontSize: 16, fontWeight: "700" }}>
              {loading ? "Enviando..." : "Enviar código"}
            </Text>
          </LinearGradient>
        </TouchableOpacity>
      </ScrollView>
    </ScreenLayout>
  );
}
