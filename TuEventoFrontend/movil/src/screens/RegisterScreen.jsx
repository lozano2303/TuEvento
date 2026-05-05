import { useState } from "react";
import {
  View, Text, TextInput, TouchableOpacity,
  ScrollView, ActivityIndicator, StatusBar,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { authService } from "../services/authService";
import { oauthService } from "../services/oauthService";
import { mapErrorMessage, parseValidationErrors } from "../utils/errorMessages";
import BackButton from "../components/BackButton";
import ScreenLayout from "../components/ScreenLayout";
import { colors } from "../theme";

const GMAIL_PATTERN    = /^[a-zA-Z0-9._%+\-]+@gmail\.com$/;
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).{8,}$/;
const FULL_NAME_PATTERN = /^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]{2,}(\s[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]{2,})+$/;

export default function RegisterScreen() {
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const [form, setForm] = useState({ fullName: "", email: "", password: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState(null);
  const [apiLoading, setApiLoading] = useState(false);
  const [oauthLoading, setOauthLoading] = useState(null);

  const update = (field, value) => {
    setForm({ ...form, [field]: value });
    if (errors[field]) setErrors({ ...errors, [field]: null });
    if (apiError) setApiError(null);
  };

  const validate = () => {
    const e = {};
    const name = form.fullName.trim();
    if (!name) e.fullName = "El nombre completo es requerido";
    else if (!FULL_NAME_PATTERN.test(name)) e.fullName = "Ingresa nombre y apellido (solo letras, mínimo dos palabras)";
    else if (name.length > 100) e.fullName = "Máximo 100 caracteres";

    const email = form.email.trim();
    if (!email) e.email = "El correo es requerido";
    else if (!GMAIL_PATTERN.test(email)) e.email = "Solo se aceptan correos @gmail.com";

    const pw = form.password;
    if (!pw) e.password = "La contraseña es requerida";
    else if (!PASSWORD_PATTERN.test(pw)) e.password = "Mín. 8 chars, mayúscula, minúscula, número y símbolo";
    else if (pw.length > 100) e.password = "Máximo 100 caracteres";

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

  const handleOAuthRegister = async (provider) => {
    setOauthLoading(provider);
    try {
      const code = provider === "google"
        ? await oauthService.loginWithGoogle()
        : await oauthService.loginWithFacebook();
      await authService.oauthLogin(provider, code);
      navigation.navigate("Main");
    } catch (e) {
      setApiError(mapErrorMessage(e.message));
    } finally {
      setOauthLoading(null);
    }
  };

  // Estilo de input glass
  const inputStyle = (field) => ({
    backgroundColor: colors.background + "60",
    borderRadius: 14,
    borderWidth: 1,
    borderColor: errors[field] ? colors.error : colors.primary + "35",
    flexDirection: "row",
    alignItems: "center",
  });

  return (
    <ScreenLayout>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      {/* Orbes de luz ambiental */}
      <View pointerEvents="none" style={{
        position: "absolute", top: -60, right: -60,
        width: 260, height: 260, borderRadius: 130,
        backgroundColor: colors.accent + "14",
      }} />
      <View pointerEvents="none" style={{
        position: "absolute", bottom: 80, left: -50,
        width: 200, height: 200, borderRadius: 100,
        backgroundColor: colors.primary + "18",
      }} />

      <ScrollView
        style={{ paddingTop: insets.top > 0 ? insets.top + 8 : 0 }}
        contentContainerStyle={{ flexGrow: 1, paddingHorizontal: 24, paddingBottom: insets.bottom + 24 }}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        {/* Header */}
        <View style={{ flexDirection: "row", alignItems: "center", marginBottom: 32 }}>
          <BackButton style={{ marginBottom: 0 }} />
          <View style={{ flex: 1, alignItems: "center" }}>
            <Text style={{ color: colors.textPrimary, fontSize: 26, fontWeight: "800" }}>
              Crea tu cuenta
            </Text>
          </View>
          <View style={{ width: 40 }} />
        </View>

        {/* Card glass del formulario */}
        <View style={{
          backgroundColor: colors.surface + "80",
          borderRadius: 24, borderWidth: 1,
          borderColor: colors.primary + "30",
          padding: 24,
        }}>
          {/* Nombre completo */}
          <Text style={{ color: colors.textSecondary, fontSize: 13, fontWeight: "600", marginBottom: 8 }}>
            Nombre completo
          </Text>
          <View style={inputStyle("fullName")}>
            <TextInput
              placeholder="Ej: Juan Pérez"
              placeholderTextColor={colors.textMuted}
              value={form.fullName}
              onChangeText={(t) => update("fullName", t)}
              maxLength={100}
              style={{ color: colors.textPrimary, padding: 14, fontSize: 15, flex: 1 }}
            />
          </View>
          {errors.fullName && (
            <Text style={{ color: colors.error, fontSize: 12, marginTop: 4 }}>{errors.fullName}</Text>
          )}

          {/* Email */}
          <Text style={{ color: colors.textSecondary, fontSize: 13, fontWeight: "600", marginTop: 18, marginBottom: 8 }}>
            Correo electrónico
          </Text>
          <View style={inputStyle("email")}>
            <TextInput
              placeholder="tucorreo@gmail.com"
              placeholderTextColor={colors.textMuted}
              value={form.email}
              onChangeText={(t) => update("email", t)}
              maxLength={255}
              style={{ color: colors.textPrimary, padding: 14, fontSize: 15, flex: 1 }}
              keyboardType="email-address"
              autoCapitalize="none"
              autoCorrect={false}
            />
          </View>
          {errors.email && (
            <Text style={{ color: colors.error, fontSize: 12, marginTop: 4 }}>{errors.email}</Text>
          )}

          {/* Contraseña */}
          <Text style={{ color: colors.textSecondary, fontSize: 13, fontWeight: "600", marginTop: 18, marginBottom: 8 }}>
            Contraseña
          </Text>
          <View style={inputStyle("password")}>
            <TextInput
              placeholder="Mín. 8 chars, mayúscula, número y símbolo"
              placeholderTextColor={colors.textMuted}
              value={form.password}
              onChangeText={(t) => update("password", t)}
              secureTextEntry={!showPassword}
              maxLength={100}
              style={{ color: colors.textPrimary, padding: 14, fontSize: 15, flex: 1 }}
            />
            <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={{ paddingRight: 14 }}>
              <Ionicons name={showPassword ? "eye" : "eye-off"} size={20} color={colors.textMuted} />
            </TouchableOpacity>
          </View>
          {errors.password && (
            <Text style={{ color: colors.error, fontSize: 12, marginTop: 4 }}>{errors.password}</Text>
          )}

          {/* Error API */}
          {apiError && (
            <View style={{
              backgroundColor: colors.errorBg, borderRadius: 10,
              padding: 12, marginTop: 14,
              borderWidth: 1, borderColor: colors.error,
            }}>
              <Text style={{ color: colors.error, fontSize: 13, textAlign: "center" }}>{apiError}</Text>
            </View>
          )}

          {/* Botón CTA */}
          <TouchableOpacity
            onPress={handleRegister}
            disabled={apiLoading}
            activeOpacity={0.75}
            style={{
              borderRadius: 14, overflow: "hidden", marginTop: 24,
              opacity: apiLoading ? 0.7 : 1,
              shadowColor: colors.primary,
              shadowOffset: { width: 0, height: 6 },
              shadowOpacity: 0.5, shadowRadius: 12, elevation: 8,
            }}
          >
            <LinearGradient
              colors={colors.gradientPrimary}
              start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }}
              style={{ paddingVertical: 16, alignItems: "center" }}
            >
              <Text style={{ color: colors.textPrimary, fontSize: 16, fontWeight: "700" }}>
                {apiLoading ? "Creando cuenta..." : "Crear cuenta"}
              </Text>
            </LinearGradient>
          </TouchableOpacity>
        </View>

        {/* Divider */}
        <View style={{ flexDirection: "row", alignItems: "center", marginVertical: 24 }}>
          <View style={{ flex: 1, height: 1, backgroundColor: colors.surfaceAlt }} />
          <Text style={{ color: colors.textMuted, marginHorizontal: 12, fontSize: 12 }}>
            o regístrate con
          </Text>
          <View style={{ flex: 1, height: 1, backgroundColor: colors.surfaceAlt }} />
        </View>

        {/* OAuth */}
        <View style={{ gap: 12 }}>
          <TouchableOpacity
            activeOpacity={0.75}
            disabled={oauthLoading !== null}
            onPress={() => handleOAuthRegister("google")}
            style={{
              flexDirection: "row", alignItems: "center", justifyContent: "center",
              gap: 10, backgroundColor: "#FFFFFF", borderRadius: 14,
              paddingVertical: 14, opacity: oauthLoading === "google" ? 0.7 : 1,
            }}
          >
            {oauthLoading === "google"
              ? <ActivityIndicator size="small" color="#EA4335" />
              : <Ionicons name="logo-google" size={20} color="#EA4335" />}
            <Text style={{ color: "#111827", fontSize: 15, fontWeight: "600" }}>
              {oauthLoading === "google" ? "Conectando..." : "Continuar con Google"}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            activeOpacity={0.75}
            disabled={oauthLoading !== null}
            onPress={() => handleOAuthRegister("facebook")}
            style={{
              flexDirection: "row", alignItems: "center", justifyContent: "center",
              gap: 10, backgroundColor: "#1877F2", borderRadius: 14,
              paddingVertical: 14, opacity: oauthLoading === "facebook" ? 0.7 : 1,
            }}
          >
            {oauthLoading === "facebook"
              ? <ActivityIndicator size="small" color="#FFFFFF" />
              : <Ionicons name="logo-facebook" size={20} color="#FFFFFF" />}
            <Text style={{ color: "#FFFFFF", fontSize: 15, fontWeight: "600" }}>
              {oauthLoading === "facebook" ? "Conectando..." : "Continuar con Facebook"}
            </Text>
          </TouchableOpacity>
        </View>

        {/* Login + Términos */}
        <View style={{ alignItems: "center", marginTop: 28, gap: 12, marginBottom: 16 }}>
          <View style={{ flexDirection: "row" }}>
            <Text style={{ color: colors.textSecondary, fontSize: 14 }}>¿Ya tienes cuenta? </Text>
            <TouchableOpacity onPress={() => navigation.navigate("Login")}>
              <Text style={{ color: colors.primary, fontSize: 14, fontWeight: "600" }}>
                Inicia sesión
              </Text>
            </TouchableOpacity>
          </View>
          <TouchableOpacity onPress={() => navigation.navigate("Terms")}>
            <Text style={{ color: colors.textMuted, fontSize: 12 }}>
              Términos y condiciones
            </Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </ScreenLayout>
  );
}
