import { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StatusBar,
  KeyboardAvoidingView,
  Platform,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useRoute } from "@react-navigation/native";
import { authService } from "../services/authService";
import { mapErrorMessage } from "../utils/errorMessages";

const BASE_URL = `${process.env.EXPO_PUBLIC_API_URL}/auth`;

export default function ActivateScreen() {
  const navigation = useNavigation();
  const route = useRoute();
  const email = route.params?.email || "";
  const [code, setCode] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleActivate = async () => {
    if (!code.trim()) {
      setError("El código es requerido");
      return;
    }
    if (code.trim().length !== 8) {
      setError("El código debe tener exactamente 8 caracteres");
      return;
    }
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
    <View style={{ flex: 1, backgroundColor: "#1E0A3C" }}>
      <StatusBar barStyle="light-content" backgroundColor="#1E0A3C" />
      <LinearGradient
        colors={["#1E0A3C", "#2D1B4E", "#1E0A3C"]}
        style={{ position: "absolute", top: 0, left: 0, right: 0, bottom: 0 }}
      />
      <KeyboardAvoidingView
        style={{ flex: 1, paddingHorizontal: 28, paddingTop: 80 }}
        behavior={Platform.OS === "ios" ? "padding" : "height"}
      >
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          style={{
            width: 40, height: 40, borderRadius: 20,
            backgroundColor: "#2D1B4E", alignItems: "center",
            justifyContent: "center", marginBottom: 32,
            borderWidth: 1, borderColor: "#3D2B5E",
          }}
        >
          <Ionicons name="arrow-back" size={20} color="#FFFFFF" />
        </TouchableOpacity>

        <Ionicons name="mail-outline" size={48} color="#7C3AED" style={{ marginBottom: 20 }} />

        <Text style={{ color: "#FFFFFF", fontSize: 26, fontWeight: "800", marginBottom: 10 }}>
          Activa tu cuenta
        </Text>
        <Text style={{ color: "#9CA3AF", fontSize: 14, marginBottom: 8 }}>
          Enviamos un código a:
        </Text>
        <Text style={{ color: "#A78BFA", fontSize: 15, fontWeight: "600", marginBottom: 32 }}>
          {email}
        </Text>

        <Text style={{ color: "#FFFFFF", fontWeight: "600", marginBottom: 8 }}>
          Código de activación
        </Text>
        <View style={{
          backgroundColor: "#2D1B4E", borderRadius: 12,
          borderWidth: error ? 1 : 0, borderColor: "#EF4444",
        }}>
          <TextInput
            placeholder="Ingresa el código"
            placeholderTextColor="#6B7280"
            value={code}
            onChangeText={setCode}
            style={{ color: "#FFFFFF", padding: 16, fontSize: 15, letterSpacing: 4 }}
            keyboardType="default"
          />
        </View>

        {error && (
          <View style={{
            backgroundColor: "#EF444422", borderRadius: 10,
            padding: 12, marginTop: 8, borderWidth: 1, borderColor: "#EF4444",
          }}>
            <Text style={{ color: "#EF4444", fontSize: 13, textAlign: "center" }}>{error}</Text>
          </View>
        )}

        <TouchableOpacity
          onPress={handleActivate}
          activeOpacity={0.85}
          style={{
            borderRadius: 14, overflow: "hidden", marginTop: 32,
            shadowColor: "#7C3AED", shadowOffset: { width: 0, height: 6 },
            shadowOpacity: 0.5, shadowRadius: 12, elevation: 8,
          }}
        >
          <LinearGradient
            colors={["#7C3AED", "#6D28D9"]}
            start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }}
            style={{ paddingVertical: 16, alignItems: "center" }}
          >
            <Text style={{ color: "#FFFFFF", fontSize: 16, fontWeight: "700" }}>
              {loading ? "Verificando..." : "Activar cuenta"}
            </Text>
          </LinearGradient>
        </TouchableOpacity>
      </KeyboardAvoidingView>
    </View>
  );
}
