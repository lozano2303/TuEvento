import { useState, useCallback } from "react";
import {
  View, Text, TextInput, TouchableOpacity,
  ScrollView, ActivityIndicator, StatusBar,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useFocusEffect } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useAuth } from "../context/AuthContext";
import { authService } from "../services/authService";
import { oauthService } from "../services/oauthService";
import { mapErrorMessage } from "../utils/errorMessages";
import BackButton from "../components/BackButton";
import ScreenLayout from "../components/ScreenLayout";
import { colors } from "../theme";

export default function LoginScreen() {
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const { login, setSession, loading, error } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [oauthLoading, setOauthLoading] = useState(null);

  useFocusEffect(
    useCallback(() => {
      setEmail("");
      setPassword("");
      setErrors({});
    }, [])
  );

  const validate = () => {
    const e = {};
    if (!email)    e.email    = "El correo es requerido";
    if (!password) e.password = "La contraseña es requerida";
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleLogin = async () => {
    if (!validate()) return;
    try {
      const result = await login(email, password);
      if (result.success) navigation.navigate("Main");
    } finally {}
  };

  const handleOAuthLogin = async (provider) => {
    setOauthLoading(provider);
    try {
      const code = provider === "google"
        ? await oauthService.loginWithGoogle()
        : await oauthService.loginWithFacebook();
      const result = await authService.oauthLogin(provider, code);
      await setSession(result);
      navigation.navigate("Main");
    } catch (e) {
      setErrors({ oauth: mapErrorMessage(e.message) });
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
        position: "absolute", top: -80, left: -60,
        width: 280, height: 280, borderRadius: 140,
        backgroundColor: colors.primary + "1A",
      }} />
      <View pointerEvents="none" style={{
        position: "absolute", bottom: 60, right: -60,
        width: 220, height: 220, borderRadius: 110,
        backgroundColor: colors.accent + "12",
      }} />

      <ScrollView
        style={{ paddingTop: insets.top > 0 ? insets.top + 8 : 0 }}
        contentContainerStyle={{ flexGrow: 1, paddingHorizontal: 24, paddingBottom: insets.bottom + 24 }}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        {/* Header */}
        <View style={{ flexDirection: "row", alignItems: "center", marginBottom: 32 }}>
          <BackButton
            onPress={() => navigation.navigate("Landing")}
            style={{ marginBottom: 0 }}
          />
          <View style={{ flex: 1, alignItems: "center" }}>
            <Text style={{ color: colors.textPrimary, fontSize: 26, fontWeight: "800" }}>
              Iniciar sesión
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
          {/* Email */}
          <Text style={{ color: colors.textSecondary, fontSize: 13, fontWeight: "600", marginBottom: 8 }}>
            Correo electrónico
          </Text>
          <View style={inputStyle("email")}>
            <TextInput
              placeholder="tucorreo@gmail.com"
              placeholderTextColor={colors.textMuted}
              value={email}
              onChangeText={(t) => { setEmail(t); setErrors({ ...errors, email: null }); }}
              style={{ color: colors.textPrimary, padding: 14, fontSize: 15, flex: 1 }}
              keyboardType="email-address"
              autoCapitalize="none"
            />
          </View>
          {errors.email && (
            <Text style={{ color: colors.error, fontSize: 12, marginTop: 4 }}>{errors.email}</Text>
          )}

          {/* Password */}
          <Text style={{ color: colors.textSecondary, fontSize: 13, fontWeight: "600", marginTop: 18, marginBottom: 8 }}>
            Contraseña
          </Text>
          <View style={inputStyle("password")}>
            <TextInput
              placeholder="Tu contraseña"
              placeholderTextColor={colors.textMuted}
              value={password}
              onChangeText={(t) => { setPassword(t); setErrors({ ...errors, password: null }); }}
              secureTextEntry={!showPassword}
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
          {error && !errors.email && !errors.password && (
            <View style={{
              backgroundColor: colors.errorBg, borderRadius: 10,
              padding: 12, marginTop: 14,
              borderWidth: 1, borderColor: colors.error,
            }}>
              <Text style={{ color: colors.error, fontSize: 13, textAlign: "center" }}>{error}</Text>
            </View>
          )}

          {/* Botón CTA */}
          <TouchableOpacity
            onPress={handleLogin}
            disabled={loading}
            activeOpacity={0.75}
            style={{
              borderRadius: 14, overflow: "hidden", marginTop: 24,
              opacity: loading ? 0.7 : 1,
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
                {loading ? "Iniciando sesión..." : "Iniciar sesión"}
              </Text>
            </LinearGradient>
          </TouchableOpacity>

          {/* Links */}
          <TouchableOpacity
            onPress={() => navigation.navigate("ForgotPassword")}
            style={{ alignItems: "center", marginTop: 16 }}
          >
            <Text style={{ color: colors.primary, fontSize: 13, fontWeight: "600" }}>
              ¿Olvidaste tu contraseña?
            </Text>
          </TouchableOpacity>
        </View>

        {/* Divider */}
        <View style={{ flexDirection: "row", alignItems: "center", marginVertical: 24 }}>
          <View style={{ flex: 1, height: 1, backgroundColor: colors.surfaceAlt }} />
          <Text style={{ color: colors.textMuted, marginHorizontal: 12, fontSize: 12 }}>
            o continúa con
          </Text>
          <View style={{ flex: 1, height: 1, backgroundColor: colors.surfaceAlt }} />
        </View>

        {/* OAuth */}
        <View style={{ gap: 12 }}>
          <TouchableOpacity
            activeOpacity={0.75}
            disabled={oauthLoading !== null}
            onPress={() => handleOAuthLogin("google")}
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
            onPress={() => handleOAuthLogin("facebook")}
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

        {/* Registro + Términos */}
        <View style={{ alignItems: "center", marginTop: 28, gap: 12, marginBottom: 16 }}>
          <View style={{ flexDirection: "row" }}>
            <Text style={{ color: colors.textSecondary, fontSize: 14 }}>¿No tienes cuenta? </Text>
            <TouchableOpacity onPress={() => navigation.navigate("Register")}>
              <Text style={{ color: colors.primary, fontSize: 14, fontWeight: "600" }}>
                Regístrate
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
