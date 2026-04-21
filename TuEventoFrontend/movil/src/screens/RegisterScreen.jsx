import { useState } from "react";
import {
  View, Text, TextInput, TouchableOpacity,
  ScrollView,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { authService } from "../services/authService";
import { mapErrorMessage, parseValidationErrors } from "../utils/errorMessages";
import ScreenLayout from "../components/ScreenLayout";
import { colors } from "../theme";

// Mismas reglas que ValidationUtils.java del backend
const GMAIL_PATTERN = /^[a-zA-Z0-9._%+\-]+@gmail\.com$/;
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).{8,}$/;
const FULL_NAME_PATTERN = /^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]{2,}(\s[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]{2,})+$/;

export default function RegisterScreen() {
  const navigation = useNavigation();
  const [form, setForm] = useState({ fullName: "", email: "", password: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState(null);
  const [apiLoading, setApiLoading] = useState(false);

  const update = (field, value) => {
    setForm({ ...form, [field]: value });
    if (errors[field]) setErrors({ ...errors, [field]: null });
    if (apiError) setApiError(null);
  };

  const validate = () => {
    const e = {};
    const name = form.fullName.trim();
    if (!name) e.fullName = "El nombre completo es requerido";
    else if (!FULL_NAME_PATTERN.test(name)) e.fullName = "Ingresa tu nombre y apellido (solo letras, mínimo dos palabras)";
    else if (name.length > 100) e.fullName = "El nombre no puede superar los 100 caracteres";

    const email = form.email.trim();
    if (!email) e.email = "El correo electrónico es requerido";
    else if (!GMAIL_PATTERN.test(email)) e.email = "Solo se aceptan correos @gmail.com";

    const password = form.password;
    if (!password) e.password = "La contraseña es requerida";
    else if (!PASSWORD_PATTERN.test(password)) e.password = "Debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial";
    else if (password.length > 100) e.password = "La contraseña no puede superar los 100 caracteres";

    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleRegister = async () => {
    if (!validate()) return;
    setApiLoading(true);
    setApiError(null);
    try {
      await authService.register(form.fullName.trim(), form.email.trim(), form.password);
      navigation.navigate("Activate", { email: form.email.trim() });
    } catch (e) {
      const fieldErrors = parseValidationErrors(e.message);
      if (fieldErrors) setErrors(fieldErrors);
      else setApiError(mapErrorMessage(e.message));
    } finally {
      setApiLoading(false);
    }
  };

  const inputStyle = (field) => ({
    backgroundColor: colors.surface,
    borderRadius: 12,
    borderWidth: errors[field] ? 1 : 0,
    borderColor: colors.error,
  });

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

        <Text style={{ color: colors.textPrimary, fontSize: 28, fontWeight: "800", textAlign: "center", marginBottom: 40 }}>
          Crea tu cuenta
        </Text>

        {/* Nombre completo */}
        <Text style={{ color: colors.textPrimary, fontWeight: "600", marginBottom: 8 }}>Nombre completo</Text>
        <View style={inputStyle("fullName")}>
          <TextInput
            placeholder="Ej: Juan Pérez"
            placeholderTextColor={colors.textMuted}
            value={form.fullName}
            onChangeText={(t) => update("fullName", t)}
            maxLength={100}
            style={{ color: colors.textPrimary, padding: 16, fontSize: 15 }}
          />
        </View>
        {errors.fullName && <Text style={{ color: colors.error, fontSize: 13, marginTop: 4 }}>{errors.fullName}</Text>}

        {/* Email */}
        <Text style={{ color: colors.textPrimary, fontWeight: "600", marginTop: 20, marginBottom: 8 }}>Correo electrónico</Text>
        <View style={inputStyle("email")}>
          <TextInput
            placeholder="tucorreo@gmail.com"
            placeholderTextColor={colors.textMuted}
            value={form.email}
            onChangeText={(t) => update("email", t)}
            maxLength={255}
            style={{ color: colors.textPrimary, padding: 16, fontSize: 15 }}
            keyboardType="email-address"
            autoCapitalize="none"
            autoCorrect={false}
          />
        </View>
        {errors.email && <Text style={{ color: colors.error, fontSize: 13, marginTop: 4 }}>{errors.email}</Text>}

        {/* Contraseña */}
        <Text style={{ color: colors.textPrimary, fontWeight: "600", marginTop: 20, marginBottom: 8 }}>Contraseña</Text>
        <View style={[inputStyle("password"), { flexDirection: "row", alignItems: "center" }]}>
          <TextInput
            placeholder="Mín. 8 chars, mayúscula, número y símbolo"
            placeholderTextColor={colors.textMuted}
            value={form.password}
            onChangeText={(t) => update("password", t)}
            secureTextEntry={!showPassword}
            maxLength={100}
            style={{ color: colors.textPrimary, padding: 16, fontSize: 15, flex: 1 }}
          />
          <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={{ paddingRight: 16 }}>
            <Ionicons name={showPassword ? "eye" : "eye-off"} size={22} color={colors.textMuted} />
          </TouchableOpacity>
        </View>
        {errors.password && <Text style={{ color: colors.error, fontSize: 13, marginTop: 4 }}>{errors.password}</Text>}

        {/* Error API */}
        {apiError && (
          <View style={{ backgroundColor: colors.errorBg, borderRadius: 10, padding: 12, marginTop: 16, borderWidth: 1, borderColor: colors.error }}>
            <Text style={{ color: colors.error, fontSize: 13, textAlign: "center" }}>{apiError}</Text>
          </View>
        )}

        {/* Botón */}
        <TouchableOpacity
          onPress={handleRegister}
          disabled={apiLoading}
          activeOpacity={0.85}
          style={{ borderRadius: 14, overflow: "hidden", marginTop: 32, shadowColor: colors.primary, shadowOffset: { width: 0, height: 6 }, shadowOpacity: 0.5, shadowRadius: 12, elevation: 8, opacity: apiLoading ? 0.7 : 1 }}
        >
          <LinearGradient colors={colors.gradientPrimary} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={{ paddingVertical: 16, alignItems: "center" }}>
            <Text style={{ color: colors.textPrimary, fontSize: 16, fontWeight: "700" }}>
              {apiLoading ? "Creando cuenta..." : "Siguiente"}
            </Text>
          </LinearGradient>
        </TouchableOpacity>

        {/* OAuth */}
        <Text style={{ color: colors.textSecondary, textAlign: "center", marginTop: 24, marginBottom: 16 }}>Regístrate con:</Text>
        <View style={{ gap: 12 }}>
          <TouchableOpacity activeOpacity={0.85} style={{ flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 10, backgroundColor: "#FFFFFF", borderRadius: 14, paddingVertical: 14 }}>
            <Ionicons name="logo-google" size={20} color="#EA4335" />
            <Text style={{ color: "#111827", fontSize: 15, fontWeight: "600" }}>Continuar con Google</Text>
          </TouchableOpacity>
          <TouchableOpacity activeOpacity={0.85} style={{ flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 10, backgroundColor: "#1877F2", borderRadius: 14, paddingVertical: 14 }}>
            <Ionicons name="logo-facebook" size={20} color="#FFFFFF" />
            <Text style={{ color: "#FFFFFF", fontSize: 15, fontWeight: "600" }}>Continuar con Facebook</Text>
          </TouchableOpacity>
        </View>

        {/* Términos */}
        <View style={{ flexDirection: "row", justifyContent: "center", marginTop: 24, marginBottom: 40, flexWrap: "wrap" }}>
          <Text style={{ color: colors.textMuted, fontSize: 13 }}>Crea tu cuenta bajo nuestros </Text>
          <TouchableOpacity onPress={() => navigation.navigate("Terms")}>
            <Text style={{ color: colors.primary, fontSize: 13, fontWeight: "600" }}>términos y condiciones del sistema</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </ScreenLayout>
  );
}
