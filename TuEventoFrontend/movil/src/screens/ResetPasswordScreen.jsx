import { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StatusBar,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useRoute } from "@react-navigation/native";
import { mapErrorMessage } from "../utils/errorMessages";

const API_URL = process.env.EXPO_PUBLIC_API_URL;

export default function ResetPasswordScreen() {
  const navigation = useNavigation();
  const route = useRoute();
  const email = route.params?.email || "";
  const [form, setForm] = useState({ code: "", newPassword: "", confirmPassword: "" });
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  // Misma regla que ValidationUtils.validateStrongPassword del backend
  const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).{8,}$/;

  const update = (field, value) => {
    setForm({ ...form, [field]: value });
    setErrors({ ...errors, [field]: null });
    if (apiError) setApiError(null);
  };

  const validate = () => {
    const e = {};
    if (!form.code.trim()) {
      e.code = "El código de recuperación es requerido";
    } else if (form.code.trim().length !== 8) {
      e.code = "El código debe tener exactamente 8 caracteres";
    }
    if (!form.newPassword) {
      e.newPassword = "La contraseña es requerida";
    } else if (!PASSWORD_PATTERN.test(form.newPassword)) {
      e.newPassword = "Debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial";
    } else if (form.newPassword.length > 100) {
      e.newPassword = "La contraseña no puede superar los 100 caracteres";
    }
    if (!form.confirmPassword) {
      e.confirmPassword = "Confirma tu contraseña";
    } else if (form.newPassword !== form.confirmPassword) {
      e.confirmPassword = "Las contraseñas no coinciden";
    }
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
        body: JSON.stringify({
          email,
          code: form.code.trim(),
          newPassword: form.newPassword,
          confirmPassword: form.confirmPassword,
        }),
      });
      const data = await response.json();
      if (!data.success) throw new Error(data.message);
      setSuccess(true);
    } catch (e) {
      const msg = e.message;
      // Errores que aplican al campo código
      if (
        msg === "Invalid recovery code" ||
        msg === "Recovery code has already been used" ||
        msg === "Recovery code has expired"
      ) {
        setErrors({ code: mapErrorMessage(msg) });
      // Error que aplica al campo contraseña
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
      <View style={{ flex: 1, backgroundColor: "#1E0A3C", justifyContent: "center", alignItems: "center", paddingHorizontal: 32 }}>
        <View style={{
          width: 80, height: 80, borderRadius: 40,
          backgroundColor: "#05966922", alignItems: "center",
          justifyContent: "center", marginBottom: 24,
        }}>
          <Ionicons name="checkmark-circle" size={48} color="#059669" />
        </View>
        <Text style={{ color: "#FFFFFF", fontSize: 24, fontWeight: "800", marginBottom: 12 }}>
          ¡Contraseña restablecida!
        </Text>
        <Text style={{ color: "#9CA3AF", fontSize: 14, textAlign: "center", lineHeight: 22, marginBottom: 32 }}>
          Tu contraseña fue actualizada exitosamente. Ya puedes iniciar sesión.
        </Text>
        <TouchableOpacity
          onPress={() => navigation.navigate("Login")}
          activeOpacity={0.85}
          style={{ borderRadius: 14, overflow: "hidden", width: "100%" }}
        >
          <LinearGradient
            colors={["#7C3AED", "#6D28D9"]}
            start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }}
            style={{ paddingVertical: 16, alignItems: "center" }}
          >
            <Text style={{ color: "#FFFFFF", fontSize: 16, fontWeight: "700" }}>Ir al inicio de sesión</Text>
          </LinearGradient>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={{ flex: 1, backgroundColor: "#1E0A3C" }}>
      <StatusBar barStyle="light-content" backgroundColor="#1E0A3C" />
      <LinearGradient
        colors={["#1E0A3C", "#2D1B4E", "#1E0A3C"]}
        style={{ position: "absolute", top: 0, left: 0, right: 0, bottom: 0 }}
      />
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === "ios" ? "padding" : "height"}
      >
        <ScrollView
          contentContainerStyle={{ flexGrow: 1, paddingHorizontal: 28, paddingTop: 60 }}
          keyboardShouldPersistTaps="handled"
        >
          {/* Botón volver */}
          <TouchableOpacity
            onPress={() => navigation.goBack()}
            style={{
              width: 40, height: 40, borderRadius: 20,
              backgroundColor: "#2D1B4E", alignItems: "center",
              justifyContent: "center", marginBottom: 24,
              borderWidth: 1, borderColor: "#3D2B5E",
            }}
          >
            <Ionicons name="arrow-back" size={20} color="#FFFFFF" />
          </TouchableOpacity>

          {/* Header */}
          <View style={{ alignItems: "center", marginBottom: 32 }}>
            <View style={{
              width: 72, height: 72, borderRadius: 20,
              backgroundColor: "#3D2B5E", alignItems: "center",
              justifyContent: "center", marginBottom: 16,
            }}>
              <Ionicons name="key-outline" size={32} color="#7C3AED" />
            </View>
            <Text style={{ color: "#FFFFFF", fontSize: 26, fontWeight: "800", marginBottom: 8 }}>
              Nueva contraseña
            </Text>
            <Text style={{ color: "#9CA3AF", fontSize: 14, textAlign: "center", lineHeight: 22 }}>
              Ingresa el código que enviamos a{"\n"}
              <Text style={{ color: "#A78BFA", fontWeight: "600" }}>{email}</Text>
            </Text>
          </View>

          {/* Código */}
          <Text style={{ color: "#FFFFFF", fontWeight: "600", marginBottom: 8 }}>
            Código de verificación
          </Text>
          <View style={{
            backgroundColor: "#2D1B4E", borderRadius: 12,
            borderWidth: errors.code ? 1 : 0, borderColor: "#EF4444",
          }}>
            <TextInput
              placeholder="Código de 8 caracteres"
              placeholderTextColor="#6B7280"
              value={form.code}
              onChangeText={(t) => update("code", t)}
              style={{ color: "#FFFFFF", padding: 16, fontSize: 15 }}
              autoCapitalize="none"
            />
          </View>
          {errors.code && (
            <Text style={{ color: "#EF4444", fontSize: 13, marginTop: 4 }}>{errors.code}</Text>
          )}

          {/* Nueva contraseña */}
          <Text style={{ color: "#FFFFFF", fontWeight: "600", marginTop: 20, marginBottom: 8 }}>
            Nueva contraseña
          </Text>
          <View style={{
            backgroundColor: "#2D1B4E", borderRadius: 12,
            flexDirection: "row", alignItems: "center",
            borderWidth: errors.newPassword ? 1 : 0, borderColor: "#EF4444",
          }}>
            <TextInput
              placeholder="Mín. 8 chars, mayúscula, número y símbolo"
              placeholderTextColor="#6B7280"
              value={form.newPassword}
              onChangeText={(t) => update("newPassword", t)}
              secureTextEntry={!showNew}
              style={{ color: "#FFFFFF", padding: 16, fontSize: 15, flex: 1 }}
            />
            <TouchableOpacity onPress={() => setShowNew(!showNew)} style={{ paddingRight: 16 }}>
              <Ionicons name={showNew ? "eye" : "eye-off"} size={22} color="#6B7280" />
            </TouchableOpacity>
          </View>
          {errors.newPassword && (
            <Text style={{ color: "#EF4444", fontSize: 13, marginTop: 4 }}>{errors.newPassword}</Text>
          )}

          {/* Confirmar contraseña */}
          <Text style={{ color: "#FFFFFF", fontWeight: "600", marginTop: 20, marginBottom: 8 }}>
            Confirmar contraseña
          </Text>
          <View style={{
            backgroundColor: "#2D1B4E", borderRadius: 12,
            flexDirection: "row", alignItems: "center",
            borderWidth: errors.confirmPassword ? 1 : 0, borderColor: "#EF4444",
          }}>
            <TextInput
              placeholder="Repite tu nueva contraseña"
              placeholderTextColor="#6B7280"
              value={form.confirmPassword}
              onChangeText={(t) => update("confirmPassword", t)}
              secureTextEntry={!showConfirm}
              style={{ color: "#FFFFFF", padding: 16, fontSize: 15, flex: 1 }}
            />
            <TouchableOpacity onPress={() => setShowConfirm(!showConfirm)} style={{ paddingRight: 16 }}>
              <Ionicons name={showConfirm ? "eye" : "eye-off"} size={22} color="#6B7280" />
            </TouchableOpacity>
          </View>
          {errors.confirmPassword && (
            <Text style={{ color: "#EF4444", fontSize: 13, marginTop: 4 }}>{errors.confirmPassword}</Text>
          )}

          {/* Botón */}
          {apiError && (
            <View style={{
              backgroundColor: "#EF444422", borderRadius: 10,
              padding: 12, marginTop: 16, borderWidth: 1, borderColor: "#EF4444",
            }}>
              <Text style={{ color: "#EF4444", fontSize: 13, textAlign: "center" }}>{apiError}</Text>
            </View>
          )}
          <TouchableOpacity
            onPress={handleSubmit}
            disabled={loading}
            activeOpacity={0.85}
            style={{
              borderRadius: 14, overflow: "hidden", marginTop: 32, marginBottom: 40,
              shadowColor: "#7C3AED", shadowOffset: { width: 0, height: 6 },
              shadowOpacity: 0.5, shadowRadius: 12, elevation: 8,
              opacity: loading ? 0.7 : 1,
            }}
          >
            <LinearGradient
              colors={["#7C3AED", "#6D28D9"]}
              start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }}
              style={{ paddingVertical: 16, alignItems: "center" }}
            >
              <Text style={{ color: "#FFFFFF", fontSize: 16, fontWeight: "700" }}>
                {loading ? "Restableciendo..." : "Restablecer contraseña"}
              </Text>
            </LinearGradient>
          </TouchableOpacity>
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}
