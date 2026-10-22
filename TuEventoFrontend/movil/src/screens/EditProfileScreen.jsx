import { useState, useEffect } from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Image,
  ActivityIndicator,
  StatusBar,
  TextInput,
  Modal,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import * as ImagePicker from "expo-image-picker";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import { useAuth } from "../context/AuthContext";
import { uploadFile, getFileUrl } from "../services/storageService";
import { profileService } from "../services/profileService";
import AppModal from "../components/AppModal";

const MAX_BIO = 255;

export default function EditProfileScreen({ navigation, route }) {
  const { colors } = useTheme();
  const { user, setUser, logout, setShowLogoutModal } = useAuth();
  const insets = useSafeAreaInsets();

  // ── Estado de desactivación de cuenta ──────────────────────────────────────
  const [deactivateOpen, setDeactivateOpen] = useState(false);
  const [deactivateStep, setDeactivateStep] = useState(1); // 1: consecuencias, 2: contraseña
  const [deactivatePassword, setDeactivatePassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [deactivating, setDeactivating] = useState(false);
  const [deactivateError, setDeactivateError] = useState("");

  const currentAvatarUrl = route.params?.currentAvatarUrl ?? null;

  // ── Estado avatar ──────────────────────────────────────────────────────────
  const [avatarUrl, setAvatarUrl] = useState(currentAvatarUrl);
  const [uploading, setUploading] = useState(false);

  // ── Estado formulario ──────────────────────────────────────────────────────
  const [fullName, setFullName] = useState(user?.fullName ?? "");
  const [bio, setBio]           = useState("");
  const [loadingProfile, setLoadingProfile] = useState(true);
  const [saving, setSaving]     = useState(false);

  // ── Estado del modal ───────────────────────────────────────────────────────
  const [modal, setModal] = useState({
    visible: false, type: "info", title: "", message: "", onConfirm: null, onCancel: null,
  });
  const showModal = (type, title, message, onConfirm = null, onCancel = null) =>
    setModal({ visible: true, type, title, message, onConfirm, onCancel });
  const hideModal = () => setModal((prev) => ({ ...prev, visible: false }));

  const roleLabel =
    user?.role === "ADMIN" ? "Administrador" :
    user?.role === "ORGANIZER" ? "Organizador" : "Usuario";

  // ── Cargar perfil (nombre + bio) ─────────────────────────────────────────────
  useEffect(() => {
    const load = async () => {
      try {
        const token = await AsyncStorage.getItem("accessToken");
        const profile = await profileService.getByUserId(user.userId, token);
        if (profile?.fullName) setFullName(profile.fullName);
        if (profile?.bio) setBio(profile.bio);
      } catch {
        // mantiene valores por defecto
      } finally {
        setLoadingProfile(false);
      }
    };
    if (user?.userId) load();
    else setLoadingProfile(false);
  }, [user?.userId]);

  // ── Estilos ───────────────────────────────────────────────────────────────
  const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: colors.background },
    header: {
      flexDirection: "row", alignItems: "center",
      paddingHorizontal: 20, paddingTop: insets.top + 16, paddingBottom: 16,
      backgroundColor: colors.surface,
      borderBottomWidth: 1, borderBottomColor: colors.primary + "30",
      gap: 12,
    },
    backBtn: {
      width: 38, height: 38, borderRadius: 12,
      backgroundColor: colors.primary + "28",
      borderWidth: 1, borderColor: colors.primary + "40",
      alignItems: "center", justifyContent: "center",
    },
    headerTitle: { fontSize: 18, fontWeight: "800", color: colors.textPrimary, flex: 1 },

    // Avatar
    avatarSection: { alignItems: "center", paddingVertical: 32 },
    avatarWrapper: {
      width: 110, height: 110, borderRadius: 55, overflow: "hidden",
      borderWidth: 3, borderColor: colors.accent,
    },
    avatar: { width: "100%", height: "100%" },
    avatarPlaceholder: {
      width: 110, height: 110, borderRadius: 55,
      backgroundColor: colors.primary,
      alignItems: "center", justifyContent: "center",
      borderWidth: 3, borderColor: colors.accent,
    },
    avatarInitial: { fontSize: 44, color: colors.textPrimary, fontWeight: "700" },
    uploadingOverlay: {
      position: "absolute", width: 110, height: 110, borderRadius: 55,
      backgroundColor: "rgba(0,0,0,0.45)", alignItems: "center", justifyContent: "center",
    },
    changePhotoBtn: {
      marginTop: 14, paddingHorizontal: 20, paddingVertical: 8,
      borderRadius: 20, borderWidth: 1.5, borderColor: colors.primary,
      flexDirection: "row", alignItems: "center", gap: 6,
    },
    changePhotoText: { color: colors.primary, fontSize: 14, fontWeight: "600" },
    roleBadge: {
      marginTop: 12, backgroundColor: colors.primary + "22",
      borderRadius: 20, paddingHorizontal: 14, paddingVertical: 5,
      borderWidth: 1, borderColor: colors.primary + "55",
    },
    roleBadgeText: { color: colors.accent, fontSize: 11, fontWeight: "700", letterSpacing: 0.4 },

    // Secciones
    section: {
      marginHorizontal: 20, marginBottom: 16,
      backgroundColor: colors.surface + "CC",
      borderRadius: 16, borderWidth: 1, borderColor: colors.primary + "25",
      padding: 18,
    },
    sectionTitle: {
      color: colors.textPrimary, fontSize: 15, fontWeight: "700",
      marginBottom: 14, flexDirection: "row", alignItems: "center",
    },
    label: {
      color: colors.textMuted, fontSize: 11, fontWeight: "700",
      textTransform: "uppercase", letterSpacing: 0.8, marginBottom: 8,
    },
    input: {
      backgroundColor: colors.surface, borderRadius: 12,
      paddingHorizontal: 14, paddingVertical: 12,
      color: colors.textPrimary, fontSize: 14,
      borderWidth: 1, borderColor: colors.primary + "30",
    },
    inputMultiline: { minHeight: 90, textAlignVertical: "top" },
    charCount: { color: colors.textMuted, fontSize: 11, textAlign: "right", marginTop: 4 },
    saveBtn: {
      marginTop: 18, backgroundColor: colors.primary,
      borderRadius: 13, paddingVertical: 14,
      alignItems: "center", justifyContent: "center",
    },
    saveBtnText: { color: colors.textPrimary, fontSize: 15, fontWeight: "700" },

    // Botón personalizar tema
    themeRow: {
      flexDirection: "row", alignItems: "center", gap: 14,
    },
    themeIconWrap: {
      width: 44, height: 44, borderRadius: 12,
      backgroundColor: colors.accent + "22",
      borderWidth: 1, borderColor: colors.accent + "40",
      alignItems: "center", justifyContent: "center",
    },
  });

  // ── Lógica de imagen ──────────────────────────────────────────────────────
  const pickImage = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== "granted") {
      showModal("info", "Permiso requerido", "Necesitamos acceso a tu galería para cambiar la foto.");
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true, aspect: [1, 1], quality: 0.8,
    });
    if (result.canceled) return;
    await handleUpload(result.assets[0].uri);
  };

  const handleUpload = async (imageUri) => {
    setUploading(true);
    try {
      const token = await AsyncStorage.getItem("accessToken");
      const uploaded = await uploadFile(imageUri, "PROFILE_PICTURE", token);
      const newStoredFileId = uploaded.storedFileId ?? uploaded.id ?? uploaded.fileId;
      await profileService.updateProfile(user.profileId, { storedFileId: newStoredFileId }, token);
      const newUrl = await getFileUrl(newStoredFileId, token);
      setAvatarUrl(newUrl);
      setUser((prev) => ({ ...prev, storedFileId: newStoredFileId }));
      showModal("success", "¡Listo!", "Tu foto de perfil fue actualizada.");
    } catch (err) {
      showModal("error", "Error", err.message ?? "No se pudo actualizar la foto.");
    } finally {
      setUploading(false);
    }
  };

  // ── Guardar nombre + bio ────────────────────────────────────────────────────
  const handleSave = async () => {
    if (!user?.profileId) {
      showModal("error", "Error", "No se encontró tu perfil.");
      return;
    }
    setSaving(true);
    try {
      const token = await AsyncStorage.getItem("accessToken");
      await profileService.updateProfile(
        user.profileId,
        { fullName: fullName.trim(), bio: bio.trim() },
        token
      );
      setUser((prev) => ({ ...prev, fullName: fullName.trim() }));
      showModal("success", "Guardado", "Tu información fue actualizada correctamente.");
    } catch (err) {
      showModal("error", "Error", err.message ?? "No se pudo guardar la información.");
    } finally {
      setSaving(false);
    }
  };

  // ── Cerrar sesión ───────────────────────────────────────────────────────────
  const handleLogout = () => {
    // Reutiliza el modal global de logout gestionado por AppNavigator
    setShowLogoutModal(true);
  };

  // ── Desactivar cuenta ─────────────────────────────────────────────────────────
  const closeDeactivate = () => {
    if (deactivating) return;
    setDeactivateOpen(false);
    setDeactivateStep(1);
    setDeactivatePassword("");
    setShowPassword(false);
    setDeactivateError("");
  };

  const handleDeactivate = async () => {
    if (!deactivatePassword.trim()) {
      setDeactivateError("Ingresa tu contraseña para continuar.");
      return;
    }
    setDeactivating(true);
    setDeactivateError("");
    try {
      const token = await AsyncStorage.getItem("accessToken");
      await profileService.deactivateAccount(deactivatePassword, token);
      // Éxito → cerrar sesión y volver al login
      await logout();
    } catch (err) {
      setDeactivateError(err.message || "No se pudo desactivar la cuenta. Intenta de nuevo.");
      setDeactivating(false);
    }
  };

  const initial = (fullName || user?.fullName || "?").charAt(0).toUpperCase();

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={colors.surface} />

      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()} activeOpacity={0.75}>
          <Ionicons name="arrow-back" size={20} color={colors.accent} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Mi perfil</Text>
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: insets.bottom + 32 }}
      >
        {/* ── Avatar ── */}
        <View style={styles.avatarSection}>
          <View>
            {avatarUrl ? (
              <View style={styles.avatarWrapper}>
                <Image source={{ uri: avatarUrl }} style={styles.avatar} resizeMode="cover" />
              </View>
            ) : (
              <View style={styles.avatarPlaceholder}>
                <Text style={styles.avatarInitial}>{initial}</Text>
              </View>
            )}
            {uploading && (
              <View style={styles.uploadingOverlay}>
                <ActivityIndicator color="#fff" size="large" />
              </View>
            )}
          </View>

          <TouchableOpacity style={styles.changePhotoBtn} onPress={pickImage} disabled={uploading} activeOpacity={0.75}>
            <Ionicons name="camera-outline" size={15} color={colors.primary} />
            <Text style={styles.changePhotoText}>{uploading ? "Subiendo..." : "Cambiar foto"}</Text>
          </TouchableOpacity>

          {/* Email + badge de rol */}
          <Text style={{ color: colors.textSecondary, fontSize: 13, marginTop: 12 }}>
            {user?.alias ?? ""}
          </Text>
          <View style={styles.roleBadge}>
            <Text style={styles.roleBadgeText}>{roleLabel}</Text>
          </View>
        </View>

        {/* ── Información personal ── */}
        <View style={styles.section}>
          <View style={{ flexDirection: "row", alignItems: "center", gap: 8, marginBottom: 14 }}>
            <Ionicons name="person-outline" size={17} color={colors.primary} />
            <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700" }}>
              Información personal
            </Text>
          </View>

          {loadingProfile ? (
            <ActivityIndicator color={colors.primary} style={{ paddingVertical: 20 }} />
          ) : (
            <>
              {/* Nombre completo */}
              <Text style={styles.label}>Nombre completo</Text>
              <TextInput
                value={fullName}
                onChangeText={setFullName}
                placeholder="Tu nombre completo"
                placeholderTextColor={colors.textMuted}
                style={styles.input}
              />

              {/* Biografía */}
              <Text style={[styles.label, { marginTop: 16 }]}>Biografía</Text>
              <TextInput
                value={bio}
                onChangeText={(t) => t.length <= MAX_BIO && setBio(t)}
                placeholder="Cuéntanos algo sobre ti…"
                placeholderTextColor={colors.textMuted}
                multiline
                numberOfLines={3}
                style={[styles.input, styles.inputMultiline]}
              />
              <Text style={styles.charCount}>{bio.length}/{MAX_BIO}</Text>

              {/* Guardar */}
              <TouchableOpacity
                style={[styles.saveBtn, saving && { opacity: 0.6 }]}
                onPress={handleSave}
                disabled={saving}
                activeOpacity={0.8}
              >
                {saving
                  ? <ActivityIndicator color={colors.textPrimary} size="small" />
                  : <Text style={styles.saveBtnText}>Guardar cambios</Text>
                }
              </TouchableOpacity>
            </>
          )}
        </View>

        {/* ── Apariencia / Personalizar tema ── */}
        <TouchableOpacity
          style={styles.section}
          onPress={() => navigation.navigate("Settings")}
          activeOpacity={0.75}
        >
          <View style={styles.themeRow}>
            <View style={styles.themeIconWrap}>
              <Ionicons name="color-palette-outline" size={20} color={colors.accent} />
            </View>
            <View style={{ flex: 1 }}>
              <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "600" }}>
                Personalizar tema
              </Text>
              <Text style={{ color: colors.textMuted, fontSize: 12, marginTop: 2 }}>
                Elige y personaliza los colores de la app
              </Text>
            </View>
            <Ionicons name="chevron-forward" size={18} color={colors.textMuted} />
          </View>
        </TouchableOpacity>

        {/* ── Seguridad de la cuenta ── */}
        <View style={styles.section}>
          <View style={{ flexDirection: "row", alignItems: "center", gap: 8, marginBottom: 14 }}>
            <Ionicons name="shield-checkmark-outline" size={17} color={colors.primary} />
            <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700" }}>
              Seguridad de la cuenta
            </Text>
          </View>

          {/* Cerrar sesión */}
          <View style={{
            flexDirection: "row", alignItems: "center", justifyContent: "space-between",
            paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: colors.primary + "18",
          }}>
            <View style={{ flex: 1, paddingRight: 12 }}>
              <Text style={{ color: colors.textPrimary, fontSize: 14, fontWeight: "600" }}>
                Cerrar sesión
              </Text>
              <Text style={{ color: colors.textMuted, fontSize: 12, marginTop: 2 }}>
                Salir de la cuenta en este dispositivo.
              </Text>
            </View>
            <TouchableOpacity
              onPress={handleLogout}
              activeOpacity={0.8}
              style={{
                flexDirection: "row", alignItems: "center", gap: 6,
                paddingHorizontal: 14, paddingVertical: 9, borderRadius: 11,
                borderWidth: 1, borderColor: colors.primary + "40",
              }}
            >
              <Ionicons name="log-out-outline" size={15} color={colors.accent} />
              <Text style={{ color: colors.accent, fontSize: 13, fontWeight: "700" }}>Salir</Text>
            </TouchableOpacity>
          </View>

          {/* Zona de peligro */}
          <View style={{
            marginTop: 14, borderRadius: 12, padding: 14,
            backgroundColor: colors.error + "0D",
            borderWidth: 1, borderColor: colors.error + "40",
          }}>
            <Text style={{ color: colors.error, fontSize: 13, fontWeight: "800" }}>
              Zona de peligro
            </Text>
            <Text style={{ color: colors.textSecondary, fontSize: 12, marginTop: 3, lineHeight: 18 }}>
              Al desactivar tu cuenta se cerrará tu sesión de inmediato y no podrás ingresar hasta contactar soporte.
            </Text>
            <TouchableOpacity
              onPress={() => { setDeactivateOpen(true); setDeactivateStep(1); }}
              activeOpacity={0.8}
              style={{
                marginTop: 12, alignSelf: "flex-start",
                paddingHorizontal: 16, paddingVertical: 10, borderRadius: 11,
                borderWidth: 1, borderColor: colors.error + "55",
              }}
            >
              <Text style={{ color: colors.error, fontSize: 13, fontWeight: "700" }}>
                Desactivar cuenta
              </Text>
            </TouchableOpacity>
          </View>
        </View>
      </ScrollView>

      {/* ── Modal de desactivación (2 pasos) ── */}
      <Modal visible={deactivateOpen} transparent animationType="fade" statusBarTranslucent onRequestClose={closeDeactivate}>
        <View style={{
          flex: 1, backgroundColor: "rgba(0,0,0,0.7)",
          justifyContent: "center", alignItems: "center", paddingHorizontal: 28,
        }}>
          <View style={{
            backgroundColor: colors.surface, borderRadius: 22, width: "100%",
            padding: 24, borderWidth: 1, borderColor: colors.error + "40",
          }}>
            {/* Ícono */}
            <View style={{ alignItems: "center", marginBottom: 12 }}>
              <View style={{
                width: 56, height: 56, borderRadius: 28,
                backgroundColor: colors.error + "1A",
                alignItems: "center", justifyContent: "center",
              }}>
                <Ionicons name="warning-outline" size={30} color={colors.error} />
              </View>
            </View>

            <Text style={{ color: colors.textPrimary, fontSize: 19, fontWeight: "800", textAlign: "center", marginBottom: 8 }}>
              ¿Desactivar tu cuenta?
            </Text>
            <Text style={{ color: colors.textMuted, fontSize: 13, textAlign: "center", lineHeight: 20, marginBottom: 20 }}>
              {deactivateStep === 1
                ? "Esta acción cerrará tu sesión de inmediato y desactivará tu cuenta temporalmente."
                : "Ingresa tu contraseña para confirmar la desactivación."}
            </Text>

            {/* ── Paso 1: consecuencias ── */}
            {deactivateStep === 1 && (
              <>
                <View style={{
                  backgroundColor: colors.primary + "0D", borderRadius: 12, padding: 14,
                  borderWidth: 1, borderColor: colors.primary + "25", marginBottom: 20, gap: 10,
                }}>
                  {[
                    "Tu sesión cerrará inmediatamente.",
                    "No podrás iniciar sesión hasta contactar soporte.",
                    "Tus datos se conservan, no se elimina nada.",
                  ].map((t) => (
                    <View key={t} style={{ flexDirection: "row", alignItems: "flex-start", gap: 8 }}>
                      <View style={{ width: 6, height: 6, borderRadius: 3, backgroundColor: colors.accent, marginTop: 6 }} />
                      <Text style={{ color: colors.textSecondary, fontSize: 13, flex: 1, lineHeight: 19 }}>{t}</Text>
                    </View>
                  ))}
                </View>

                <View style={{ flexDirection: "row", gap: 12 }}>
                  <TouchableOpacity
                    onPress={closeDeactivate} activeOpacity={0.8}
                    style={{ flex: 1, paddingVertical: 13, borderRadius: 13, borderWidth: 1, borderColor: colors.primary + "40", alignItems: "center" }}
                  >
                    <Text style={{ color: colors.textSecondary, fontWeight: "700" }}>Cancelar</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={() => { setDeactivateStep(2); setDeactivateError(""); }}
                    activeOpacity={0.8}
                    style={{ flex: 1, paddingVertical: 13, borderRadius: 13, backgroundColor: colors.error, alignItems: "center" }}
                  >
                    <Text style={{ color: "#fff", fontWeight: "700" }}>Continuar</Text>
                  </TouchableOpacity>
                </View>
              </>
            )}

            {/* ── Paso 2: contraseña ── */}
            {deactivateStep === 2 && (
              <>
                <View style={{
                  flexDirection: "row", alignItems: "center",
                  backgroundColor: colors.background, borderRadius: 12,
                  borderWidth: 1, borderColor: deactivateError ? colors.error + "66" : colors.primary + "30",
                  paddingHorizontal: 14, marginBottom: deactivateError ? 8 : 20,
                }}>
                  <TextInput
                    value={deactivatePassword}
                    onChangeText={(t) => { setDeactivatePassword(t); setDeactivateError(""); }}
                    placeholder="Contraseña actual"
                    placeholderTextColor={colors.textMuted}
                    secureTextEntry={!showPassword}
                    autoFocus
                    editable={!deactivating}
                    style={{ flex: 1, color: colors.textPrimary, fontSize: 14, paddingVertical: 13 }}
                  />
                  <TouchableOpacity onPress={() => setShowPassword((v) => !v)} activeOpacity={0.7}>
                    <Ionicons name={showPassword ? "eye-off-outline" : "eye-outline"} size={18} color={colors.textMuted} />
                  </TouchableOpacity>
                </View>

                {deactivateError ? (
                  <View style={{ flexDirection: "row", alignItems: "center", gap: 6, marginBottom: 16 }}>
                    <Ionicons name="alert-circle-outline" size={15} color={colors.error} />
                    <Text style={{ color: colors.error, fontSize: 12, flex: 1 }}>{deactivateError}</Text>
                  </View>
                ) : null}

                <View style={{ flexDirection: "row", gap: 12 }}>
                  <TouchableOpacity
                    onPress={() => { setDeactivateStep(1); setDeactivatePassword(""); setDeactivateError(""); }}
                    disabled={deactivating} activeOpacity={0.8}
                    style={{ flex: 1, paddingVertical: 13, borderRadius: 13, borderWidth: 1, borderColor: colors.primary + "40", alignItems: "center", opacity: deactivating ? 0.5 : 1 }}
                  >
                    <Text style={{ color: colors.textSecondary, fontWeight: "700" }}>Volver</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={handleDeactivate}
                    disabled={deactivating || !deactivatePassword.trim()}
                    activeOpacity={0.8}
                    style={{ flex: 1, paddingVertical: 13, borderRadius: 13, backgroundColor: colors.error, alignItems: "center", opacity: (deactivating || !deactivatePassword.trim()) ? 0.5 : 1 }}
                  >
                    {deactivating
                      ? <ActivityIndicator size="small" color="#fff" />
                      : <Text style={{ color: "#fff", fontWeight: "700" }}>Confirmar</Text>
                    }
                  </TouchableOpacity>
                </View>
              </>
            )}
          </View>
        </View>
      </Modal>

      {/* ── Modal genérico ── */}
      <AppModal
        {...modal}
        onConfirm={() => { modal.onConfirm?.(); hideModal(); }}
        onCancel={modal.onCancel ? hideModal : undefined}
      />
    </View>
  );
}
