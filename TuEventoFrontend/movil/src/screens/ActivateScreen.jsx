import { useState } from "react";
import { View, Text, TextInput, TouchableOpacity } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useRoute } from "@react-navigation/native";
import { authService } from "../services/authService";
import { mapErrorMessage } from "../utils/errorMessages";
import BackButton from "../components/BackButton";
import ScreenLayout from "../components/ScreenLayout";
import { colors } from "../theme";

export default function ActivateScreen() {
  const navigation = useNavigation();
  const route = useRoute();
  const email = route.params?.email || "";
  const [code, setCode] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleActivate = async () => {
    if (!code.trim()) { setError("El código es requerido"); return; }
    if (code.trim().length !== 8) { setError("El código debe tener exactamente 8 caracteres"); return; }
    setLoading(true);
    setError(null);
    try {
      await authService.activateAccount(email, code.trim());
      navigation.navigate("Login");
    } catch (e) {
      setError(mapErrorMessage(e.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScreenLayout>
      <View style={{ flex: 1, paddingHorizontal: 28, paddingTop: 80 }}>
        <View style={{ flexDirection: "row", alignItems: "center", marginBottom: 20 }}>
          <BackButton style={{ marginBottom: 0 }} />
          <View style={{ flex: 1, alignItems: "center" }}>
            <Text style={{ color: colors.textPrimary, fontSize: 26, fontWeight: "800" }}>
              Activa tu cuenta
            </Text>
          </View>
          <View style={{ width: 40 }} />
        </View>

        <Ionicons name="mail-outline" size={48} color={colors.primary} style={{ marginBottom: 20 }} />

        <Text style={{ color: colors.textSecondary, fontSize: 14, marginBottom: 8 }}>Enviamos un código a:</Text>
        <Text style={{ color: colors.accent, fontSize: 15, fontWeight: "600", marginBottom: 32 }}>{email}</Text>

        <Text style={{ color: colors.textPrimary, fontWeight: "600", marginBottom: 8 }}>Código de activación</Text>
        <View style={{ backgroundColor: colors.surface, borderRadius: 12, borderWidth: error ? 1 : 0, borderColor: colors.error }}>
          <TextInput
            placeholder="Ingresa el código"
            placeholderTextColor={colors.textMuted}
            value={code}
            onChangeText={setCode}
            style={{ color: colors.textPrimary, padding: 16, fontSize: 15, letterSpacing: 4 }}
            keyboardType="default"
          />
        </View>

        {error && (
          <View style={{ backgroundColor: colors.errorBg, borderRadius: 10, padding: 12, marginTop: 8, borderWidth: 1, borderColor: colors.error }}>
            <Text style={{ color: colors.error, fontSize: 13, textAlign: "center" }}>{error}</Text>
          </View>
        )}

        <TouchableOpacity
          onPress={handleActivate}
          activeOpacity={0.85}
          style={{ borderRadius: 14, overflow: "hidden", marginTop: 32, shadowColor: colors.primary, shadowOffset: { width: 0, height: 6 }, shadowOpacity: 0.5, shadowRadius: 12, elevation: 8 }}
        >
          <LinearGradient colors={colors.gradientPrimary} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={{ paddingVertical: 16, alignItems: "center" }}>
            <Text style={{ color: colors.textPrimary, fontSize: 16, fontWeight: "700" }}>
              {loading ? "Verificando..." : "Activar cuenta"}
            </Text>
          </LinearGradient>
        </TouchableOpacity>
      </View>
    </ScreenLayout>
  );
}
