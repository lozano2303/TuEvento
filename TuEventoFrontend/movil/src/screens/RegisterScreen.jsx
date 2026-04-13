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
import { useNavigation } from "@react-navigation/native";
import { authService } from "../services/authService";
import { mapErrorMessage } from "../utils/errorMessages";

export default function RegisterScreen() {
  const navigation = useNavigation();
  const [form, setForm] = useState({ name: "", email: "", password: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [apiError, setApiError] = useState(null);
  const [apiLoading, setApiLoading] = useState(false);

  const update = (field, value) => {
    setForm({ ...form, [field]: value });
    setErrors({ ...errors, [field]: null });
  };

  const validate = () => {
    const e = {};
    if (!form.name.trim()) e.name = "El nombre es requerido";
    if (!form.email.trim()) e.email = "El correo es requerido";
    else if (!/\S+@\S+\.\S+/.test(form.email)) e.email = "Correo inválido";
    if (!form.password) e.password = "La contraseña es requerida";
    else if (form.password.length < 8) e.password = "Mínimo 8 caracteres";
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleRegister = async () => {
    if (validate()) {
      setApiLoading(true);
      setApiError(null);
      try {
        await authService.register(form.name, form.email, form.password);
        navigation.navigate("Activate", { email: form.email });
      } catch (e) {
        setApiError(mapErrorMessage(e.message));
      } finally {
        setApiLoading(false);
      }
    }
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === "ios" ? "padding" : "height"}
    >
      <StatusBar barStyle="light-content" backgroundColor="#1E0A3C" />
      <LinearGradient colors={["#1E0A3C", "#2D1B4E", "#1E0A3C"]} style={{ flex: 1 }}>
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

          <Text style={{
            color: "#FFFFFF", fontSize: 28, fontWeight: "800",
            textAlign: "center", marginBottom: 40,
          }}>
            Crea tu cuenta
          </Text>

          {/* Nombre completo */}
          <Text style={{ color: "#FFFFFF", fontWeight: "600", marginBottom: 8 }}>
            Nombre completo
          </Text>
          <View style={{
            backgroundColor: "#2D1B4E", borderRadius: 12,
            borderWidth: errors.name ? 1 : 0, borderColor: "#EF4444",
          }}>
            <TextInput
              placeholder="Tu nombre completo"
              placeholderTextColor="#6B7280"
              value={form.name}
              onChangeText={(t) => update("name", t)}
              style={{ color: "#FFFFFF", padding: 16, fontSize: 15 }}
            />
          </View>
          {errors.name && (
            <Text style={{ color: "#EF4444", fontSize: 13, marginTop: 4 }}>{errors.name}</Text>
          )}

          {/* Email */}
          <Text style={{ color: "#FFFFFF", fontWeight: "600", marginTop: 20, marginBottom: 8 }}>
            Correo electrónico
          </Text>
          <View style={{
            backgroundColor: "#2D1B4E", borderRadius: 12,
            borderWidth: errors.email ? 1 : 0, borderColor: "#EF4444",
          }}>
            <TextInput
              placeholder="tucorreo@ejemplo.com"
              placeholderTextColor="#6B7280"
              value={form.email}
              onChangeText={(t) => update("email", t)}
              style={{ color: "#FFFFFF", padding: 16, fontSize: 15 }}
              keyboardType="email-address"
              autoCapitalize="none"
            />
          </View>
          {errors.email && (
            <Text style={{ color: "#EF4444", fontSize: 13, marginTop: 4 }}>{errors.email}</Text>
          )}

          {/* Contraseña */}
          <Text style={{ color: "#FFFFFF", fontWeight: "600", marginTop: 20, marginBottom: 8 }}>
            Contraseña
          </Text>
          <View style={{
            backgroundColor: "#2D1B4E", borderRadius: 12,
            flexDirection: "row", alignItems: "center",
            borderWidth: errors.password ? 1 : 0, borderColor: "#EF4444",
          }}>
            <TextInput
              placeholder="Mínimo 8 caracteres"
              placeholderTextColor="#6B7280"
              value={form.password}
              onChangeText={(t) => update("password", t)}
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

          {/* Botón registrar */}
          {apiError && (
            <View style={{
              backgroundColor: "#EF444422", borderRadius: 10,
              padding: 12, marginTop: 8, borderWidth: 1, borderColor: "#EF4444",
            }}>
              <Text style={{ color: "#EF4444", fontSize: 13, textAlign: "center" }}>{apiError}</Text>
            </View>
          )}
          <TouchableOpacity
            onPress={handleRegister}
            disabled={apiLoading}
            activeOpacity={0.85}
            style={{
              borderRadius: 14, overflow: "hidden", marginTop: 32,
              shadowColor: "#7C3AED", shadowOffset: { width: 0, height: 6 },
              shadowOpacity: 0.5, shadowRadius: 12, elevation: 8,
              opacity: apiLoading ? 0.7 : 1,
            }}
          >
            <LinearGradient
              colors={["#7C3AED", "#6D28D9"]}
              start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }}
              style={{ paddingVertical: 16, alignItems: "center" }}
            >
              <Text style={{ color: "#FFFFFF", fontSize: 16, fontWeight: "700" }}>
                {apiLoading ? "Creando cuenta..." : "Siguiente"}
              </Text>
            </LinearGradient>
          </TouchableOpacity>

          {/* OAuth */}
          <Text style={{ color: "#9CA3AF", textAlign: "center", marginTop: 24, marginBottom: 16 }}>
            Regístrate con:
          </Text>
          <View style={{ gap: 12 }}>
            <TouchableOpacity
              activeOpacity={0.85}
              style={{
                flexDirection: "row", alignItems: "center",
                justifyContent: "center", gap: 10,
                backgroundColor: "#FFFFFF", borderRadius: 14, paddingVertical: 14,
              }}
            >
              <Ionicons name="logo-google" size={20} color="#EA4335" />
              <Text style={{ color: "#111827", fontSize: 15, fontWeight: "600" }}>Continuar con Google</Text>
            </TouchableOpacity>
            <TouchableOpacity
              activeOpacity={0.85}
              style={{
                flexDirection: "row", alignItems: "center",
                justifyContent: "center", gap: 10,
                backgroundColor: "#1877F2", borderRadius: 14, paddingVertical: 14,
              }}
            >
              <Ionicons name="logo-facebook" size={20} color="#FFFFFF" />
              <Text style={{ color: "#FFFFFF", fontSize: 15, fontWeight: "600" }}>Continuar con Facebook</Text>
            </TouchableOpacity>
          </View>

          {/* Términos */}
          <View style={{ flexDirection: "row", justifyContent: "center", marginTop: 24, marginBottom: 40, flexWrap: "wrap" }}>
            <Text style={{ color: "#6B7280", fontSize: 13 }}>Crea tu cuenta bajo nuestros </Text>
            <TouchableOpacity onPress={() => navigation.navigate("Terms")}>
              <Text style={{ color: "#7C3AED", fontSize: 13, fontWeight: "600" }}>términos y condiciones del sistema</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </LinearGradient>
    </KeyboardAvoidingView>
  );
}
