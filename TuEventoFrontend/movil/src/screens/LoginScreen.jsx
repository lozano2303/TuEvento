import { useState, useCallback } from "react";
import {
  View, Text, TextInput, TouchableOpacity,
  ScrollView, Platform, ActivityIndicator,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useFocusEffect } from "@react-navigation/native";
import { useAuth } from "../context/AuthContext";
import { authService } from "../services/authService";
import { oauthService } from "../services/oauthService";
import { mapErrorMessage } from "../utils/errorMessages";
import ScreenLayout from "../components/ScreenLayout";
import { colors } from "../theme";

export default function LoginScreen() {
  const navigation = useNavigation();
  const { login, setSession, loading, error } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [oauthLoading, setOauthLoading] = useState(null); // "google" | "facebook" | null

  useFocusEffect(
    useCallback(() => {
      setEmail("");
      setPassword("");
      setErrors({});
    }, [])
  );

  const validate = () => {
    const e = {};
    if (!email) e.email = "Nombre o Correo Incorrecto";
    if (!password) e.password = "Contraseña Incorrecta";
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
      // Guardar tokens en el contexto/storage
      await setSession(result);
      navigation.navigate("Main");
    } catch (e) {
      setErrors({ oauth: mapErrorMessage(e.message) });
    } finally {
      setOauthLoading(null);
    }
  };

  return (
    <ScreenLayout>
      <ScrollView
        contentContainerStyle={{ flexGrow: 1, paddingHorizontal: 28, paddingTop: 60 }}
        keyboardShouldPersistTaps="handled"
      >
        <Text style={{ color: colors.textPrimary, fontSize: 28, fontWeight: "800", textAlign: "center", marginBottom: 40 }}>
          Iniciar Sesión
        </Text>

        {/* Email */}
        <Text style={{ color: colors.textPrimary, fontWeight: "600", marginBottom: 8 }}>
          Ingresa tu Correo Electronico
        </Text>
        <View style={{ backgroundColor: colors.surface, borderRadius: 12, borderWidth: errors.email ? 1 : 0, borderColor: colors.error }}>
          <TextInput
            placeholder="Correo o nombre"
            placeholderTextColor={colors.textMuted}
            value={email}
            onChangeText={(t) => { setEmail(t); setErrors({ ...errors, email: null }); }}
            style={{ color: colors.textPrimary, padding: 16, fontSize: 15 }}
            keyboardType="email-address"
            autoCapitalize="none"
          />
        </View>
        {errors.email && <Text style={{ color: colors.error, fontSize: 13, marginTop: 4 }}>{errors.email}</Text>}

        {/* Password */}
        <Text style={{ color: colors.textPrimary, fontWeight: "600", marginTop: 20, marginBottom: 8 }}>
          Ingresa tu contraseña
        </Text>
        <View style={{ backgroundColor: colors.surface, borderRadius: 12, flexDirection: "row", alignItems: "center", borderWidth: errors.password ? 1 : 0, borderColor: colors.error }}>
          <TextInput
            placeholder="Contraseña"
            placeholderTextColor={colors.textMuted}
            value={password}
            onChangeText={(t) => { setPassword(t); setErrors({ ...errors, password: null }); }}
            secureTextEntry={!showPassword}
            style={{ color: colors.textPrimary, padding: 16, fontSize: 15, flex: 1 }}
          />
          <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={{ paddingRight: 16 }}>
            <Ionicons name={showPassword ? "eye" : "eye-off"} size={22} color={colors.textMuted} />
          </TouchableOpacity>
        </View>
        {errors.password && <Text style={{ color: colors.error, fontSize: 13, marginTop: 4 }}>{errors.password}</Text>}

        {/* Error API */}
        {error && !errors.email && !errors.password && (
          <View style={{ backgroundColor: "#EF444420", borderRadius: 10, padding: 12, marginTop: 16, borderWidth: 1, borderColor: colors.error }}>
            <Text style={{ color: colors.error, fontSize: 14, textAlign: "center" }}>{error}</Text>
          </View>
        )}

        {/* Botón */}
        <TouchableOpacity
          onPress={handleLogin}
          disabled={loading}
          activeOpacity={0.85}
          style={{ borderRadius: 14, overflow: "hidden", marginTop: 32, shadowColor: colors.primary, shadowOffset: { width: 0, height: 6 }, shadowOpacity: 0.5, shadowRadius: 12, elevation: 8, opacity: loading ? 0.7 : 1 }}
        >
          <LinearGradient colors={colors.gradientPrimary} start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }} style={{ paddingVertical: 16, alignItems: "center" }}>
            <Text style={{ color: colors.textPrimary, fontSize: 16, fontWeight: "700" }}>
              {loading ? "Iniciando sesión..." : "Iniciar sesión"}
            </Text>
          </LinearGradient>
        </TouchableOpacity>

        {/* Links */}
        <View style={{ flexDirection: "row", justifyContent: "center", marginTop: 24 }}>
          <Text style={{ color: colors.textSecondary, fontSize: 14 }}>¿No tienes una cuenta? </Text>
          <TouchableOpacity onPress={() => navigation.navigate("Register")}>
            <Text style={{ color: colors.primary, fontSize: 14, fontWeight: "600" }}>crea tu cuenta.</Text>
          </TouchableOpacity>
        </View>
        <View style={{ flexDirection: "row", justifyContent: "center", marginTop: 12 }}>
          <Text style={{ color: colors.textSecondary, fontSize: 14 }}>¿Olvidaste tu contraseña? </Text>
          <TouchableOpacity onPress={() => navigation.navigate("ForgotPassword")}>
            <Text style={{ color: colors.primary, fontSize: 14, fontWeight: "600" }}>Recupera el acceso.</Text>
          </TouchableOpacity>
        </View>

        {/* Divider */}
        <View style={{ flexDirection: "row", alignItems: "center", marginTop: 28 }}>
          <View style={{ flex: 1, height: 1, backgroundColor: colors.surfaceAlt }} />
          <Text style={{ color: colors.textMuted, marginHorizontal: 12, fontSize: 13 }}>Inicia sesión con:</Text>
          <View style={{ flex: 1, height: 1, backgroundColor: colors.surfaceAlt }} />
        </View>

        {/* OAuth */}
        <View style={{ gap: 12, marginTop: 20 }}>
          <TouchableOpacity 
            activeOpacity={0.85} 
            disabled={oauthLoading !== null}
            onPress={() => handleOAuthLogin("google")}
            style={{ flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 10, backgroundColor: "#FFFFFF", borderRadius: 14, paddingVertical: 14, opacity: oauthLoading === "google" ? 0.7 : 1 }}
          >
            {oauthLoading === "google" ? (
              <ActivityIndicator size="small" color="#EA4335" />
            ) : (
              <Ionicons name="logo-google" size={20} color="#EA4335" />
            )}
            <Text style={{ color: "#111827", fontSize: 15, fontWeight: "600" }}>
              {oauthLoading === "google" ? "Conectando..." : "Continuar con Google"}
            </Text>
          </TouchableOpacity>
          <TouchableOpacity 
            activeOpacity={0.85} 
            disabled={oauthLoading !== null}
            onPress={() => handleOAuthLogin("facebook")}
            style={{ flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 10, backgroundColor: "#1877F2", borderRadius: 14, paddingVertical: 14, opacity: oauthLoading === "facebook" ? 0.7 : 1 }}
          >
            {oauthLoading === "facebook" ? (
              <ActivityIndicator size="small" color="#FFFFFF" />
            ) : (
              <Ionicons name="logo-facebook" size={20} color="#FFFFFF" />
            )}
            <Text style={{ color: "#FFFFFF", fontSize: 15, fontWeight: "600" }}>
              {oauthLoading === "facebook" ? "Conectando..." : "Continuar con Facebook"}
            </Text>
          </TouchableOpacity>
        </View>

        {/* Términos */}
        <View style={{ flexDirection: "row", justifyContent: "center", marginTop: 28, marginBottom: 40, flexWrap: "wrap" }}>
          <Text style={{ color: colors.textMuted, fontSize: 13 }}>Consulta nuestros </Text>
          <TouchableOpacity onPress={() => navigation.navigate("Terms")}>
            <Text style={{ color: colors.primary, fontSize: 13, fontWeight: "600" }}>términos y condiciones del sistema</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </ScreenLayout>
  );
}
