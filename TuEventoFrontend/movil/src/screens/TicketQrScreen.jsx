import { useState, useEffect, useCallback, useRef } from "react";
import {
  View, Text, TouchableOpacity, ActivityIndicator, StatusBar,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useRoute } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import QRCode from "react-native-qrcode-svg";
import { useTheme } from "../context/ThemeContext";
import { generateTicketQr, generateOrderQr } from "../services/ticketService";
import { mapErrorMessage } from "../utils/errorMessages";

/**
 * Pantalla que muestra el QR de un ticket o de una orden.
 * El QR vive 60s (expiresInSeconds); al expirar se difumina y ofrece regenerar.
 * Regenerar invalida el QR anterior en el backend (anti-reventa).
 */
export default function TicketQrScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const route = useRoute();

  const { mode, id, title, subtitle } = route.params ?? {};

  const [qr, setQr]           = useState(null);   // { qrToken, expiresInSeconds, ... }
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);
  const [remaining, setRemaining] = useState(0);  // segundos restantes

  const timerRef = useRef(null);

  const generate = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = mode === "order"
        ? await generateOrderQr(id)
        : await generateTicketQr(id);
      setQr(data);
      setRemaining(data.expiresInSeconds ?? 60);
    } catch (e) {
      setError(mapErrorMessage(e.message) || "No se pudo generar el QR.");
      setQr(null);
    } finally {
      setLoading(false);
    }
  }, [mode, id]);

  useEffect(() => { generate(); }, [generate]);

  // Cuenta regresiva
  useEffect(() => {
    if (!qr || remaining <= 0) return;
    timerRef.current = setInterval(() => {
      setRemaining((r) => {
        if (r <= 1) {
          clearInterval(timerRef.current);
          return 0;
        }
        return r - 1;
      });
    }, 1000);
    return () => clearInterval(timerRef.current);
  }, [qr, remaining > 0]);

  const expired = qr && remaining <= 0;
  const progress = qr ? remaining / (qr.expiresInSeconds || 60) : 0;

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <StatusBar barStyle="light-content" backgroundColor={colors.surface} />

      {/* Header */}
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
        <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "800", flex: 1 }}>
          {mode === "order" ? "QR de la orden" : "QR de la entrada"}
        </Text>
      </View>

      <View style={{ flex: 1, alignItems: "center", justifyContent: "center", paddingHorizontal: 28, gap: 20 }}>

        {/* Título del objetivo */}
        <View style={{ alignItems: "center", gap: 4 }}>
          {!!title && <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "800" }}>{title}</Text>}
          {!!subtitle && <Text style={{ color: colors.textMuted, fontSize: 13 }}>{subtitle}</Text>}
        </View>

        {/* Tarjeta del QR */}
        <View style={{
          backgroundColor: "#ffffff", borderRadius: 24, padding: 24,
          alignItems: "center", justifyContent: "center",
          width: 280, height: 280,
          borderWidth: 1, borderColor: colors.primary + "30",
        }}>
          {loading ? (
            <ActivityIndicator size="large" color={colors.primary} />
          ) : error ? (
            <View style={{ alignItems: "center", gap: 10, paddingHorizontal: 16 }}>
              <Ionicons name="alert-circle-outline" size={40} color={colors.error} />
              <Text style={{ color: "#1f2937", fontSize: 13, textAlign: "center" }}>{error}</Text>
            </View>
          ) : qr ? (
            <View style={{ opacity: expired ? 0.15 : 1 }}>
              <QRCode value={qr.qrToken} size={224} backgroundColor="#ffffff" color="#0f172a" />
            </View>
          ) : null}

          {/* Overlay de expirado */}
          {expired && (
            <View style={{ position: "absolute", alignItems: "center", gap: 6 }}>
              <Ionicons name="time-outline" size={36} color="#64748b" />
              <Text style={{ color: "#334155", fontSize: 14, fontWeight: "700" }}>QR expirado</Text>
            </View>
          )}
        </View>

        {/* Cuenta regresiva / barra */}
        {qr && !expired && (
          <View style={{ width: 240, gap: 8, alignItems: "center" }}>
            <Text style={{ color: colors.textSecondary, fontSize: 13 }}>
              Caduca en <Text style={{ color: colors.accent, fontWeight: "800" }}>{remaining}s</Text>
            </Text>
            <View style={{ width: "100%", height: 6, borderRadius: 3, backgroundColor: colors.primary + "22", overflow: "hidden" }}>
              <View style={{ width: `${progress * 100}%`, height: "100%", backgroundColor: colors.primary }} />
            </View>
          </View>
        )}

        {/* Botón regenerar */}
        {qr && (
          <TouchableOpacity
            onPress={generate}
            disabled={loading}
            activeOpacity={0.85}
            style={{
              flexDirection: "row", alignItems: "center", gap: 8,
              backgroundColor: expired ? colors.primary : colors.primary + "18",
              borderWidth: 1, borderColor: colors.primary + (expired ? "" : "40"),
              borderRadius: 14, paddingHorizontal: 20, paddingVertical: 13,
            }}
          >
            <Ionicons name="refresh" size={16} color={expired ? colors.textPrimary : colors.accent} />
            <Text style={{ color: expired ? colors.textPrimary : colors.accent, fontSize: 14, fontWeight: "700" }}>
              {expired ? "Generar nuevo QR" : "Regenerar"}
            </Text>
          </TouchableOpacity>
        )}

        {/* Nota de seguridad */}
        <View style={{ flexDirection: "row", alignItems: "flex-start", gap: 8, paddingHorizontal: 8 }}>
          <Ionicons name="shield-checkmark-outline" size={14} color={colors.textMuted} style={{ marginTop: 2 }} />
          <Text style={{ color: colors.textMuted, fontSize: 11, flex: 1, lineHeight: 16 }}>
            Muestra este código al organizador para validar tu entrada. Es de un solo uso y se renueva cada pocos segundos por seguridad.
          </Text>
        </View>
      </View>
    </View>
  );
}
