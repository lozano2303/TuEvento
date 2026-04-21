import { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StatusBar,
  Dimensions,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useFocusEffect } from "@react-navigation/native";
import { useCallback } from "react";
import { useAuth } from "../context/AuthContext";

const { width, height } = Dimensions.get("window");

export default function LoginScreen() {
  const navigation = useNavigation();
  const { login, loading, error } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});

  useFocusEffect(
    useCallback(() => {
      setEmail("");
      setPassword("");
      setErrors({});
    }, [])
  );

  const validate = () => {
    const newErrors = {};
    if (!email) newErrors.email = "Nombre o Correo Incorrecto";
    if (!password) newErrors.password = "Contraseña Incorrecta";
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleLogin = async () => {
    if (!validate()) return;
    try {
      const result = await login(email, password);
      if (result.success) {
        navigation.navigate("Main");
      }
    } finally {
      // loading es manejado por AuthContext
    }
  };

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
          {/* Título */}
          <Text style={{
            color: "#FFFFFF", fontSize: 28, fontWeight: "800",
            textAlign: "center", marginBottom: 40,
          }}>
            Iniciar Sesión
          </Text>

          {/* Campo email */}
          <Text style={{ color: "#FFFFFF", fontWeight: "600", marginBottom: 8 }}>
            Ingresa tu Correo Electronico
          </Text>
          <View style={{
            backgroundColor: "#2D1B4E", borderRadius: 12,
            borderWidth: errors.email ? 1 : 0, borderColor: "#EF4444",
          }}>
            <TextInput
              placeholder="Correo o nombre"
              placeholderTextColor="#6B7280"
              value={email}
              onChangeText={(t) => { setEmail(t); setErrors({ ...errors, email: null }); }}
              style={{ color: "#FFFFFF", padding: 16, fontSize: 15 }}
              keyboardType="email-address"
              autoCapitalize="none"
            />
          </View>
          {errors.email && (
            <Text style={{ color: "#EF4444", fontSize: 13, marginTop: 4 }}>{errors.email}</Text>
          )}

          {/* Campo password */}
          <Text style={{ color: "#FFFFFF", fontWeight: "600", marginTop: 20, marginBottom: 8 }}>
            Ingresa tu contraseña
          </Text>
          <View style={{
            backgroundColor: "#2D1B4E", borderRadius: 12,
            flexDirection: "row", alignItems: "center",
            borderWidth: errors.password ? 1 : 0, borderColor: "#EF4444",
          }}>
            <TextInput
              placeholder="Contraseña"
              placeholderTextColor="#6B7280"
              value={password}
              onChangeText={(t) => { setPassword(t); setErrors({ ...errors, password: null }); }}
              secureTextEntry={!showPassword}
              style={{ color: "#FFFFFF", padding: 16, fontSize: 15, flex: 1 }}
            />
            <TouchableOpacity
              onPress={() => setShowPassword(!showPassword)}
              style={{ paddingRight: 16 }}
            >
              <Ionicons name={showPassword ? "eye" : "eye-off"} size={22} color="#6B7280" />
            </TouchableOpacity>
          </View>
          {errors.password && (
            <Text style={{ color: "#EF4444", fontSize: 13, marginTop: 4 }}>{errors.password}</Text>
          )}

          {/* Botón iniciar */}
          {error && !errors.email && !errors.password && (
            <View style={{
              backgroundColor: "#EF444420", borderRadius: 10,
              padding: 12, marginTop: 16,
              borderWidth: 1, borderColor: "#EF4444",
            }}>
              <Text style={{ color: "#EF4444", fontSize: 14, textAlign: "center" }}>{error}</Text>
            </View>
          )}
          <TouchableOpacity
            onPress={handleLogin}
            disabled={loading}
            activeOpacity={0.85}
            style={{
              borderRadius: 14, overflow: "hidden", marginTop: 32,
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
                {loading ? "Iniciando sesión..." : "Iniciar sesión"}
              </Text>
            </LinearGradient>
          </TouchableOpacity>

          {/* Registro */}
          <View style={{ flexDirection: "row", justifyContent: "center", marginTop: 24 }}>
            <Text style={{ color: "#9CA3AF", fontSize: 14 }}>¿No tienes una cuenta? </Text>
            <TouchableOpacity onPress={() => navigation.navigate("Register")}>
              <Text style={{ color: "#7C3AED", fontSize: 14, fontWeight: "600" }}>crea tu cuenta.</Text>
            </TouchableOpacity>
          </View>

          <View style={{ flexDirection: "row", justifyContent: "center", marginTop: 12 }}>
            <Text style={{ color: "#9CA3AF", fontSize: 14 }}>¿Olvidaste tu contraseña? </Text>
            <TouchableOpacity onPress={() => navigation.navigate("ForgotPassword")}>
              <Text style={{ color: "#7C3AED", fontSize: 14, fontWeight: "600" }}>Recupera el acceso.</Text>
            </TouchableOpacity>
          </View>

          {/* Divider */}
          <View style={{ flexDirection: "row", alignItems: "center", marginTop: 28 }}>
            <View style={{ flex: 1, height: 1, backgroundColor: "#3D2B5E" }} />
            <Text style={{ color: "#6B7280", marginHorizontal: 12, fontSize: 13 }}>Inicia sesión con:</Text>
            <View style={{ flex: 1, height: 1, backgroundColor: "#3D2B5E" }} />
          </View>

          {/* OAuth botones */}
          <View style={{ gap: 12, marginTop: 20 }}>
            {/* Google */}
            <TouchableOpacity
              activeOpacity={0.85}
              style={{
                flexDirection: "row", alignItems: "center",
                justifyContent: "center", gap: 10,
                backgroundColor: "#FFFFFF", borderRadius: 14,
                paddingVertical: 14,
              }}
            >
              <Ionicons name="logo-google" size={20} color="#EA4335" />
              <Text style={{ color: "#111827", fontSize: 15, fontWeight: "600" }}>Continuar con Google</Text>
            </TouchableOpacity>

            {/* Facebook */}
            <TouchableOpacity
              activeOpacity={0.85}
              style={{
                flexDirection: "row", alignItems: "center",
                justifyContent: "center", gap: 10,
                backgroundColor: "#1877F2", borderRadius: 14,
                paddingVertical: 14,
              }}
            >
              <Ionicons name="logo-facebook" size={20} color="#FFFFFF" />
              <Text style={{ color: "#FFFFFF", fontSize: 15, fontWeight: "600" }}>Continuar con Facebook</Text>
            </TouchableOpacity>
          </View>

          {/* Términos */}
          <View style={{ flexDirection: "row", justifyContent: "center", marginTop: 28, marginBottom: 40, flexWrap: "wrap" }}>
            <Text style={{ color: "#6B7280", fontSize: 13 }}>Consulta nuestros </Text>
            <TouchableOpacity onPress={() => navigation.navigate("Terms")}>
              <Text style={{ color: "#7C3AED", fontSize: 13, fontWeight: "600" }}>términos y condiciones del sistema</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}
