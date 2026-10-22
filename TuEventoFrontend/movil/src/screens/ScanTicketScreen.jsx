import { useState, useRef, useCallback } from "react";
import {
  View, Text, TouchableOpacity, ActivityIndicator, StatusBar,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { CameraView, useCameraPermissions } from "expo-camera";
import { useTheme } from "../context/ThemeContext";
import { scanQr } from "../services/ticketService";
import { mapErrorMessage } from "../utils/errorMessages";

/**
 * Pantalla del organizador para escanear los QR de las entradas.
 * Lee el token con la cámara y lo valida en el backend. El backend verifica que
 * el organizador sea el dueño del evento y marca el/los ticket(s) como USED.
 */
export default function ScanTicketScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();

  const [permission, requestPermission] = useCameraPermissions();
  const [scanning, setScanning]   = useState(true);   // cámara activa esperando QR
  const [submitting, setSubmitting] = useState(false); // validando en backend
  const [result, setResult]       = useState(null);    // ScanResultResponse
  const [error, setError]         = useState(null);

  const lockRef = useRef(false); // evita múltiples lecturas del mismo frame

  const handleScanned = useCallback(async ({ data }) => {
    if (lockRef.current || submitting) return;
    lockRef.current = true;
    setScanning(false);
    setSubmitting(true);
    setError(null);
    setResult(null);
    try {
      const res = await scanQr(data);
      setResult(res);
    } catch (e) {
      setError(mapErrorMessage(e.message) || "No se pudo validar el QR.");
    } finally {
      setSubmitting(false);
    }
  }, [submitting]);

  const reset = () => {
    setResult(null);
    setError(null);
    lockRef.current = false;
    setScanning(true);
  };

  const Header = (
    <View style={{
      flexDirection: "row", alignItems: "center", gap: 12,
      paddingTop: insets.top + 16, paddingBottom: 16, paddingHorizontal: 20,
      backgroundColor: colors.surface, borderBottomWidth: 1, borderBottomColor: colors.primary + "30",
    }}>
      <TouchableOpacity
        onPress={() => navigation.goBack()} activeOpacity={0.75}
        style={{ width: 38, height: 38, borderRadius: 12, backgroundColor: colors.primary + "28", borderWidth: 1, borderColor: colors.primary + "40", alignItems: "center", justifyContent: "center" }}
      >
        <Ionicons name="arrow-back" size={20} color={colors.accent} />
      </TouchableOpacity>
      <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "800", flex: 1 }}>Validar entradas</Text>
    </View>
  );

  // ── Permiso de cámara ───────────────────────────────────────────────────────
  if (!permission) {
    return (
      <View style={{ flex: 1, backgroundColor: colors.background }}>
        <StatusBar barStyle="light-content" backgroundColor={colors.surface} />
        {Header}
        <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      </View>
    );
  }

  if (!permission.granted) {
    return (
      <View style={{ flex: 1, backgroundColor: colors.background }}>
        <StatusBar barStyle="light-content" backgroundColor={colors.surface} />
        {Header}
        <View style={{ flex: 1, alignItems: "center", justifyContent: "center", gap: 16, paddingHorizontal: 40 }}>
          <Ionicons name="camera-outline" size={64} color={colors.textMuted} />
          <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "700", textAlign: "center" }}>
            Necesitamos la cámara
          </Text>
          <Text style={{ color: colors.textSecondary, fontSize: 13, textAlign: "center", lineHeight: 20 }}>
            Para escanear los códigos QR de las entradas, concede el permiso de cámara.
          </Text>
          <TouchableOpacity
            onPress={requestPermission}
            activeOpacity={0.85}
            style={{ backgroundColor: colors.primary, borderRadius: 14, paddingHorizontal: 24, paddingVertical: 14 }}
          >
            <Text style={{ color: colors.textPrimary, fontSize: 14, fontWeight: "700" }}>Permitir cámara</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // ── Resultado (éxito / ya usada / error) ─────────────────────────────────────
  if (result || error) {
    const validated = result?.validatedCount ?? 0;
    const alreadyUsed = result?.alreadyUsedCount ?? 0;

    // Tres desenlaces visuales:
    //  success  → se validó al menos una entrada (puede ingresar)
    //  warning  → el QR era válido pero todo ya estaba usado (ya escaneada antes)
    //  error    → QR inválido/caducado/sin permiso
    let kind = "error";
    if (result) kind = validated > 0 ? "success" : "warning";

    const STYLE = {
      success: { color: "#34d399", icon: "checkmark-circle",  title: "¡Entrada válida!" },
      warning: { color: "#f59e0b", icon: "alert-circle",      title: "Entrada ya escaneada" },
      error:   { color: colors.error, icon: "close-circle",   title: "QR no válido" },
    }[kind];

    return (
      <View style={{ flex: 1, backgroundColor: colors.background }}>
        <StatusBar barStyle="light-content" backgroundColor={colors.surface} />
        {Header}
        <View style={{ flex: 1, alignItems: "center", justifyContent: "center", gap: 18, paddingHorizontal: 32 }}>
          <View style={{
            width: 96, height: 96, borderRadius: 48,
            backgroundColor: STYLE.color + "22",
            alignItems: "center", justifyContent: "center",
          }}>
            <Ionicons name={STYLE.icon} size={64} color={STYLE.color} />
          </View>

          <Text style={{ color: colors.textPrimary, fontSize: 24, fontWeight: "900", textAlign: "center" }}>
            {STYLE.title}
          </Text>

          {kind === "success" && (
            <View style={{ alignItems: "center", gap: 6 }}>
              <Text style={{ color: "#34d399", fontSize: 16, fontWeight: "700", textAlign: "center" }}>
                Puedes ingresar
              </Text>
              <Text style={{ color: colors.textSecondary, fontSize: 14, textAlign: "center" }}>
                {validated === 1
                  ? "1 entrada validada"
                  : `${validated} entradas validadas`}
                {alreadyUsed > 0 && ` · ${alreadyUsed} ya usada${alreadyUsed === 1 ? "" : "s"}`}
              </Text>
              {result?.orderId != null && (
                <Text style={{ color: colors.textMuted, fontSize: 12, marginTop: 2 }}>
                  Orden #{result.orderId}
                </Text>
              )}
            </View>
          )}

          {kind === "warning" && (
            <Text style={{ color: colors.textSecondary, fontSize: 15, textAlign: "center", lineHeight: 22 }}>
              {alreadyUsed === 1
                ? "Esta entrada ya había sido escaneada. No se permite el reingreso."
                : "Estas entradas ya habían sido escaneadas. No se permite el reingreso."}
            </Text>
          )}

          {kind === "error" && (
            <Text style={{ color: colors.textSecondary, fontSize: 14, textAlign: "center", lineHeight: 20 }}>
              {error}
            </Text>
          )}

          <TouchableOpacity
            onPress={reset}
            activeOpacity={0.85}
            style={{
              flexDirection: "row", alignItems: "center", gap: 8,
              backgroundColor: colors.primary, borderRadius: 14,
              paddingHorizontal: 24, paddingVertical: 14, marginTop: 4,
            }}
          >
            <Ionicons name="qr-code-outline" size={16} color={colors.textPrimary} />
            <Text style={{ color: colors.textPrimary, fontSize: 14, fontWeight: "700" }}>Escanear otra</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // ── Cámara activa ─────────────────────────────────────────────────────────────
  return (
    <View style={{ flex: 1, backgroundColor: "#000" }}>
      <StatusBar barStyle="light-content" backgroundColor={colors.surface} />
      {Header}

      <View style={{ flex: 1 }}>
        <CameraView
          style={{ flex: 1 }}
          facing="back"
          barcodeScannerSettings={{ barcodeTypes: ["qr"] }}
          onBarcodeScanned={scanning ? handleScanned : undefined}
        />

        {/* Marco guía */}
        <View style={{ position: "absolute", top: 0, left: 0, right: 0, bottom: 0, alignItems: "center", justifyContent: "center" }} pointerEvents="none">
          <View style={{
            width: 240, height: 240, borderRadius: 24,
            borderWidth: 3, borderColor: "#ffffffCC",
          }} />
        </View>

        {/* Pie / estado */}
        <View style={{ position: "absolute", bottom: insets.bottom + 28, left: 24, right: 24, alignItems: "center" }}>
          {submitting ? (
            <View style={{ flexDirection: "row", alignItems: "center", gap: 10, backgroundColor: "#000000AA", borderRadius: 14, paddingHorizontal: 18, paddingVertical: 12 }}>
              <ActivityIndicator size="small" color="#fff" />
              <Text style={{ color: "#fff", fontSize: 14, fontWeight: "600" }}>Validando…</Text>
            </View>
          ) : (
            <View style={{ backgroundColor: "#000000AA", borderRadius: 14, paddingHorizontal: 18, paddingVertical: 12 }}>
              <Text style={{ color: "#fff", fontSize: 14, fontWeight: "600", textAlign: "center" }}>
                Apunta al código QR de la entrada
              </Text>
            </View>
          )}
        </View>
      </View>
    </View>
  );
}
