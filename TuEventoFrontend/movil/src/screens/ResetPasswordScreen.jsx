import { useState } from "react";
import { View, Text, TextInput, TouchableOpacity, ScrollView } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useRoute } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { mapErrorMessage } from "../utils/errorMessages";
import BackButton from "../components/BackButton";
import ScreenLayout from "../components/ScreenLayout";
import { useTheme } from "../context/ThemeContext";

const API_URL = process.env.EXPO_PUBLIC_API_URL;
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).{8,}$/;

export default function ResetPasswordScreen() {
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const route = useRoute();
  const { palette } = useTheme();
  const email = route.params?.email || "";
  const [form, setForm] = useState({ code: "", newPassword: "", confirmPassword: "" });
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const update = (field, value) => {
    setForm({ ...form, [field]: value });
    setErrors({ ...errors, [field]: null });
    if (apiError) setApiError(null);
  };

  const validate = () => {
    const e = {};
    if (!form.code.trim()) e.code = "El código de recuperación es requerido";
    else if (form.code.trim().length !== 8) e.code = "El código debe tener exactamente 8 caracteres";

    if (!form.newPassword) e.newPassword = "La contraseña es requerida";
    else if (!PASSWORD_PATTERN.test(form.newPassword)) e.newPassword = "Debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial";
    else if (form.newPassword.length > 100) e.newPassword = "La contraseña no puede superar los 100 caracteres";

    if (!form.confirmPassword) e.confirmPassword = "Confirma tu contraseña";
    else if (form.newPassword !== form.confirmPassword) e.confirmPassword = "Las contraseñas no coinciden";

    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    setLoading(true);
    setApiError(null);
    try {
      const response = await fetch(`${API_URL}/auth/reset-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, code: form.code.trim(), newPassword: form.newPassword, confirmPassword: form.confirmPassword }),
      });
      const data = await response.json();
      if (!data.success) throw new Error(data.message);
      setSuccess(true);
    } catch (e) {
      const msg = e.message;
      if (["Invalid recovery code", "Recovery code has already been used", "Recovery code has expired"].includes(msg)) {
        setErrors({ code: mapErrorMessage(msg) });
      } else if (msg === "Passwords do not match") {
        setErrors({ confirmPassword: mapErrorMessage(msg) });
      } else if (msg.includes("Password must be")) {
        setErrors({ newPassword: mapErrorMessage(msg) });
      } else {
        setApiError(mapErrorMessage(msg));
      }
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <View style={{ flex: 1, backgroundColor: palette.background, justifyContent: "center", alignItems: "center", paddingHorizontal: 32 }}>
        <View style={{ width: 80, height: 80, borderRadius: 40, backgroundColor: palette.successBg, alignItems: "center", justifyContent: "center", marginBottom: 24 }}>
          <Ionicons name="checkmark-circle" size={48} color={palette.success} />
        </View>
        <Text style={{ color: palette.textPrimary, fontSize: 24, fontWeight: "800", marginBottom: 12 }}>¡Contraseña restablecida!</Text>
        <Text style={{ color: palette.textSecondary, fontSize: 14, textAlign: "center", lineHeight: 22, marginBottom: 32 }}>
          Tu contraseña fue actualizada exitosamente. Ya puedes iniciar sesión.
        </Text>
        <TouchableOpacity onPress={() => navigation.navigate("Login")} activeOpacity={0.85} style={{ borderRadius: 14, overflow: "hidden", width: "100%" }}>
          <LinearGradient colors={palette.gradientPrimary} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={{ paddingVertical: 16, alignItems: "center" }}>
            <Text style={{ color: palette.textPrimary, fontSize: 16, fontWeight: "700" }}>Ir al inicio de sesión</Text>
          </LinearGradient>
        </TouchableOpacity>
      </View>
    );
  }

  const inputStyle = (field) => ({
    backgroundColor: palette.surface,
    borderRadius: 12,
    borderWidth: errors[field] ? 1 : 0,
    borderColor: palette.error,
  });

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
              Nueva contraseña
            </Text>
          </View>
          <View style={{ width: 40 }} />
        </View>

        {/* Header */}
        <View style={{ alignItems: "center", marginBottom: 32 }}>
          <View style={{ width: 72, height: 72, borderRadius: 20, backgroundColor: palette.surfaceAlt, alignItems: "center", justifyContent: "center", marginBottom: 16 }}>
            <Ionicons name="key-outline" size={32} color={palette.primary} />
          </View>
          <Text style={{ color: palette.textSecondary, fontSize: 14, textAlign: "center", lineHeight: 22 }}>
            Ingresa el código que enviamos a{"\n"}
            <Text style={{ color: palette.accent, fontWeight: "600" }}>{email}</Text>
          </Text>
        </View>

        {/* Código */}
        <Text style={{ color: palette.textPrimary, fontWeight: "600", marginBottom: 8 }}>Código de verificación</Text>
        <View style={inputStyle("code")}>
          <TextInput
            placeholder="Código de 8 caracteres"
            placeholderTextColor={palette.textMuted}
            value={form.code}
            onChangeText={(t) => update("code", t)}
            style={{ color: palette.textPrimary, padding: 16, fontSize: 15 }}
            autoCapitalize="none"
          />
        </View>
        {errors.code && <Text style={{ color: palette.error, fontSize: 13, marginTop: 4 }}>{errors.code}</Text>}

        {/* Nueva contraseña */}
        <Text style={{ color: palette.textPrimary, fontWeight: "600", marginTop: 20, marginBottom: 8 }}>Nueva contraseña</Text>
        <View style={[inputStyle("newPassword"), { flexDirection: "row", alignItems: "center" }]}>
          <TextInput
            placeholder="Mín. 8 chars, mayúscula, número y símbolo"
            placeholderTextColor={palette.textMuted}
            value={form.newPassword}
            onChangeText={(t) => update("newPassword", t)}
            secureTextEntry={!showNew}
            style={{ color: palette.textPrimary, padding: 16, fontSize: 15, flex: 1 }}
          />
          <TouchableOpacity onPress={() => setShowNew(!showNew)} style={{ paddingRight: 16 }}>
            <Ionicons name={showNew ? "eye" : "eye-off"} size={22} color={palette.textMuted} />
          </TouchableOpacity>
        </View>
        {errors.newPassword && <Text style={{ color: palette.error, fontSize: 13, marginTop: 4 }}>{errors.newPassword}</Text>}

        {/* Confirmar contraseña */}
        <Text style={{ color: palette.textPrimary, fontWeight: "600", marginTop: 20, marginBottom: 8 }}>Confirmar contraseña</Text>
        <View style={[inputStyle("confirmPassword"), { flexDirection: "row", alignItems: "center" }]}>
          <TextInput
            placeholder="Repite tu nueva contraseña"
            placeholderTextColor={palette.textMuted}
            value={form.confirmPassword}
            onChangeText={(t) => update("confirmPassword", t)}
            secureTextEntry={!showConfirm}
            style={{ color: palette.textPrimary, padding: 16, fontSize: 15, flex: 1 }}
          />
          <TouchableOpacity onPress={() => setShowConfirm(!showConfirm)} style={{ paddingRight: 16 }}>
            <Ionicons name={showConfirm ? "eye" : "eye-off"} size={22} color={palette.textMuted} />
          </TouchableOpacity>
        </View>
        {errors.confirmPassword && <Text style={{ color: palette.error, fontSize: 13, marginTop: 4 }}>{errors.confirmPassword}</Text>}

        {/* Error API */}
        {apiError && (
          <View style={{ backgroundColor: palette.errorBg, borderRadius: 10, padding: 12, marginTop: 16, borderWidth: 1, borderColor: palette.error }}>
            <Text style={{ color: palette.error, fontSize: 13, textAlign: "center" }}>{apiError}</Text>
          </View>
        )}

        {/* Botón */}
        <TouchableOpacity
          onPress={handleSubmit}
          disabled={loading}
          activeOpacity={0.85}
          style={{ borderRadius: 14, overflow: "hidden", marginTop: 32, marginBottom: 40, shadowColor: palette.primary, shadowOffset: { width: 0, height: 6 }, shadowOpacity: 0.5, shadowRadius: 12, elevation: 8, opacity: loading ? 0.7 : 1 }}
        >
          <LinearGradient colors={palette.gradientPrimary} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={{ paddingVertical: 16, alignItems: "center" }}>
            <Text style={{ color: palette.textPrimary, fontSize: 16, fontWeight: "700" }}>
              {loading ? "Restableciendo..." : "Restablecer contraseña"}
            </Text>
          </LinearGradient>
        </TouchableOpacity>
      </ScrollView>
    </ScreenLayout>
  );
}
