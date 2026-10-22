import { useState, useEffect, useRef } from "react";
import { View, Text, TextInput, TouchableOpacity, ActivityIndicator } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useRoute } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { authService } from "../services/authService";
import { mapErrorMessage } from "../utils/errorMessages";
import BackButton from "../components/BackButton";
import ScreenLayout from "../components/ScreenLayout";
import { darkColors } from "../theme/colors";

const RESEND_COOLDOWN = 60; // segundos entre reenvíos

export default function ActivateScreen() {
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const route = useRoute();
  const palette = darkColors;
  const email = route.params?.email || "";
  const [code, setCode] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  // Reenvío de código
  const [resendLoading, setResendLoading] = useState(false);
  const [resendMsg, setResendMsg] = useState(null);
  const [cooldown, setCooldown] = useState(0);
  const cooldownRef = useRef(null);

  useEffect(() => () => clearInterval(cooldownRef.current), []);

  const startCooldown = () => {
    setCooldown(RESEND_COOLDOWN);
    clearInterval(cooldownRef.current);
    cooldownRef.current = setInterval(() => {
      setCooldown((c) => {
        if (c <= 1) { clearInterval(cooldownRef.current); return 0; }
        return c - 1;
      });
    }, 1000);
  };

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

  const handleResend = async () => {
    if (!email || resendLoading || cooldown > 0) return;
    setResendLoading(true);
    setResendMsg(null);
    setError(null);
    try {
      await authService.resendActivation(email);
      setResendMsg("Te enviamos un nuevo código a tu correo.");
      startCooldown();
    } catch (e) {
      setError(mapErrorMessage(e.message));
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <ScreenLayout>
      <View style={{ flex: 1, paddingHorizontal: 28, paddingTop: insets.top + 16 }}>
        <View style={{ flexDirection: "row", alignItems: "center", marginBottom: 20 }}>
          <BackButton style={{ marginBottom: 0 }} />
          <View style={{ flex: 1, alignItems: "center" }}>
            <Text style={{ color: palette.textPrimary, fontSize: 26, fontWeight: "800" }}>
              Activa tu cuenta
            </Text>
          </View>
          <View style={{ width: 40 }} />
        </View>

        <Ionicons name="mail-outline" size={48} color={palette.primary} style={{ marginBottom: 20 }} />

        <Text style={{ color: palette.textSecondary, fontSize: 14, marginBottom: 8 }}>Enviamos un código a:</Text>
        <Text style={{ color: palette.accent, fontSize: 15, fontWeight: "600", marginBottom: 32 }}>{email}</Text>

        <Text style={{ color: palette.textPrimary, fontWeight: "600", marginBottom: 8 }}>Código de activación</Text>
        <View style={{ backgroundColor: palette.surface, borderRadius: 12, borderWidth: error ? 1 : 0, borderColor: palette.error }}>
          <TextInput
            placeholder="Ingresa el código"
            placeholderTextColor={palette.textMuted}
            value={code}
            onChangeText={setCode}
            style={{ color: palette.textPrimary, padding: 16, fontSize: 15, letterSpacing: 4 }}
            keyboardType="default"
          />
        </View>

        {error && (
          <View style={{ backgroundColor: palette.errorBg, borderRadius: 10, padding: 12, marginTop: 8, borderWidth: 1, borderColor: palette.error }}>
            <Text style={{ color: palette.error, fontSize: 13, textAlign: "center" }}>{error}</Text>
          </View>
        )}

        <TouchableOpacity
          onPress={handleActivate}
          activeOpacity={0.85}
          style={{ borderRadius: 14, overflow: "hidden", marginTop: 32, shadowColor: palette.primary, shadowOffset: { width: 0, height: 6 }, shadowOpacity: 0.5, shadowRadius: 12, elevation: 8 }}
        >
          <LinearGradient colors={palette.gradientPrimary} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={{ paddingVertical: 16, alignItems: "center" }}>
            <Text style={{ color: palette.textPrimary, fontSize: 16, fontWeight: "700" }}>
              {loading ? "Verificando..." : "Activar cuenta"}
            </Text>
          </LinearGradient>
        </TouchableOpacity>

        {/* Mensaje de reenvío exitoso */}
        {resendMsg && (
          <View style={{ flexDirection: "row", alignItems: "center", gap: 8, marginTop: 20 }}>
            <Ionicons name="checkmark-circle" size={16} color={palette.success} />
            <Text style={{ color: palette.success, fontSize: 13, flex: 1 }}>{resendMsg}</Text>
          </View>
        )}

        {/* Reenviar código */}
        <View style={{ flexDirection: "row", justifyContent: "center", alignItems: "center", marginTop: resendMsg ? 16 : 28, gap: 6 }}>
          <Text style={{ color: palette.textSecondary, fontSize: 13 }}>¿No recibiste el código?</Text>
          <TouchableOpacity
            onPress={handleResend}
            disabled={resendLoading || cooldown > 0}
            activeOpacity={0.75}
          >
            {resendLoading ? (
              <View style={{ flexDirection: "row", alignItems: "center", gap: 5 }}>
                <ActivityIndicator size="small" color={palette.accent} />
                <Text style={{ color: palette.accent, fontSize: 13, fontWeight: "700" }}>Enviando…</Text>
              </View>
            ) : (
              <Text style={{ color: cooldown > 0 ? palette.textMuted : palette.accent, fontSize: 13, fontWeight: "700" }}>
                {cooldown > 0 ? `Reenviar en ${cooldown}s` : "Reenviar código"}
              </Text>
            )}
          </TouchableOpacity>
        </View>
      </View>
    </ScreenLayout>
  );
}
