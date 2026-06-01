import {
  Modal,
  View,
  Text,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useTheme } from "../context/ThemeContext";

// ─── Configuración de íconos y colores por type ───────────────────────────────
const TYPE_CONFIG = {
  confirm: { icon: "help-circle-outline",        colorKey: "primary"  },
  success: { icon: "checkmark-circle-outline",   colorKey: "success"  },
  error:   { icon: "close-circle-outline",       colorKey: "error"    },
  info:    { icon: "information-circle-outline", colorKey: "primary"  },
};

// ─── Componente ───────────────────────────────────────────────────────────────
export default function AppModal({
  visible,
  type = "info",
  title,
  message,
  confirmText = "Aceptar",
  cancelText  = "Cancelar",
  onConfirm,
  onCancel,
  loading = false,
}) {
  const { colors } = useTheme();

  const config     = TYPE_CONFIG[type] ?? TYPE_CONFIG.info;
  const accentColor = colors[config.colorKey];

  const styles = StyleSheet.create({
    overlay: {
      flex: 1,
      backgroundColor: "rgba(0,0,0,0.55)",
      justifyContent: "center",
      alignItems: "center",
      paddingHorizontal: 32,
    },
    container: {
      backgroundColor: colors.surface,
      borderRadius: 20,
      padding: 28,
      width: "100%",
      borderWidth: 1,
      borderColor: colors.surfaceAlt,
    },
    iconWrap: {
      alignItems: "center",
      marginBottom: 12,
    },
    title: {
      color: colors.textPrimary,
      fontSize: 20,
      fontWeight: "800",
      textAlign: "center",
      marginBottom: 10,
    },
    message: {
      color: colors.textSecondary,
      fontSize: 14,
      lineHeight: 22,
      textAlign: "center",
      marginBottom: 28,
    },
    btnConfirm: {
      backgroundColor: colors.primary,
      borderRadius: 14,
      paddingVertical: 14,
      alignItems: "center",
      justifyContent: "center",
      marginBottom: 12,
    },
    btnConfirmText: {
      color: colors.textPrimary,
      fontSize: 15,
      fontWeight: "700",
    },
    btnCancel: {
      borderRadius: 14,
      paddingVertical: 14,
      alignItems: "center",
      borderWidth: 1,
      borderColor: colors.surfaceAlt,
    },
    btnCancelText: {
      color: colors.accent,
      fontSize: 15,
      fontWeight: "600",
    },
  });

  return (
    <Modal
      transparent
      visible={visible}
      animationType="fade"
      statusBarTranslucent
    >
      <View style={styles.overlay}>
        <View style={styles.container}>

          {/* Ícono */}
          <View style={styles.iconWrap}>
            <Ionicons name={config.icon} size={48} color={accentColor} />
          </View>

          {/* Título */}
          {!!title && <Text style={styles.title}>{title}</Text>}

          {/* Mensaje */}
          {!!message && <Text style={styles.message}>{message}</Text>}

          {/* Botón principal */}
          <TouchableOpacity
            onPress={onConfirm}
            activeOpacity={0.75}
            disabled={loading}
            style={[styles.btnConfirm, loading && { opacity: 0.7 }]}
          >
            {loading ? (
              <ActivityIndicator color="#FFFFFF" />
            ) : (
              <Text style={styles.btnConfirmText}>{confirmText}</Text>
            )}
          </TouchableOpacity>

          {/* Botón cancelar — solo si se pasa onCancel */}
          {!!onCancel && (
            <TouchableOpacity
              onPress={onCancel}
              activeOpacity={0.75}
              disabled={loading}
              style={styles.btnCancel}
            >
              <Text style={styles.btnCancelText}>{cancelText}</Text>
            </TouchableOpacity>
          )}

        </View>
      </View>
    </Modal>
  );
}
