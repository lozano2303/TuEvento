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
import { useTranslation } from "react-i18next";
import { useTheme } from "../context/ThemeContext";
import { useAuth } from "../context/AuthContext";
import { useLanguage } from "../context/LanguageContext";
import { uploadFile, getFileUrl } from "../services/storageService";
import { profileService } from "../services/profileService";
import { getActiveLanguages } from "../services/languageService";
import { changeUILanguage } from "../i18n";
import AppModal from "../components/AppModal";
import { LANGUAGES } from "../components/LanguageSelector";
import { mapErrorMessage } from "../utils/errorMessages";

const MAX_BIO = 255;

export default function EditProfileScreen({ navigation, route }) {
  const { colors } = useTheme();
  const { user, setUser, logout, setShowLogoutModal } = useAuth();
  const insets = useSafeAreaInsets();
  const { t } = useTranslation();
  const { language, languages } = useLanguage();

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
  // Biografía traducida al idioma activo (solo lectura, igual que la web)
  const [translatedBio, setTranslatedBio] = useState(null);
  const [translationGenerating, setTranslationGenerating] = useState(false);
  // El perfil se guarda en español (idioma fuente de las traducciones): fuera de
  // español solo se puede consultar, exista o no la traducción.
  const editLocked = language !== "es";
  const [loadingProfile, setLoadingProfile] = useState(true);
  const [saving, setSaving]     = useState(false);

  // ── Estado idioma preferido (se aplica al guardar, igual que la web) ────────
  const [selectedLanguage, setSelectedLanguage] = useState(language);
  const [savingLanguage, setSavingLanguage]     = useState(false);
  const [languageSaveResult, setLanguageSaveResult] = useState(null); // "success" | "error"

  // Si el idioma activo cambia (p. ej. sincronización con el perfil), seguirlo.
  useEffect(() => { setSelectedLanguage(language); }, [language]);

  // ── Estado del modal ───────────────────────────────────────────────────────
  const [modal, setModal] = useState({
    visible: false, type: "info", title: "", message: "", onConfirm: null, onCancel: null,
  });
  const showModal = (type, title, message, onConfirm = null, onCancel = null) =>
    setModal({ visible: true, type, title, message, onConfirm, onCancel });
  const hideModal = () => setModal((prev) => ({ ...prev, visible: false }));

  const roleLabel =
    user?.role === "ADMIN" ? t("profile.roles.admin") :
    user?.role === "ORGANIZER" ? t("profile.roles.organizer") : t("profile.roles.user");

  // ── Cargar perfil (nombre + bio) ─────────────────────────────────────────────
  useEffect(() => {
    const load = async () => {
      try {
        const token = await AsyncStorage.getItem("accessToken");
        const profile = await profileService.getByUserId(user.userId, token, language);
        if (profile?.fullName) setFullName(profile.fullName);
        if (profile?.bio) setBio(profile.bio);
        setTranslatedBio(profile?.translatedBio || null);
        setTranslationGenerating(!!profile?.translationGenerating);
      } catch {
        // mantiene valores por defecto
      } finally {
        setLoadingProfile(false);
      }
    };
    if (user?.userId) load();
    else setLoadingProfile(false);
  }, [user?.userId, language]);

  // Polling cada 3s mientras el backend genera la traducción de la biografía
  useEffect(() => {
    if (!translationGenerating || translatedBio || language === "es" || !user?.userId) return;
    const interval = setInterval(async () => {
      try {
        const token = await AsyncStorage.getItem("accessToken");
        const profile = await profileService.getByUserId(user.userId, token, language);
        if (profile) {
          setTranslatedBio(profile.translatedBio || null);
          setTranslationGenerating(!!profile.translationGenerating);
        }
      } catch {
        // error puntual — se reintenta en el próximo ciclo
      }
    }, 3000);
    return () => clearInterval(interval);
  }, [translationGenerating, translatedBio, language, user?.userId]);

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
      showModal("info", t("mobileEditProfile.permissionTitle"), t("mobileEditProfile.permissionMsg"));
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
      showModal("success", t("mobileEditProfile.doneTitle"), t("profile.photoUpdated"));
    } catch (err) {
      showModal("error", t("common.error"), err.message ? mapErrorMessage(err.message) : t("profile.photoUpdateError"));
    } finally {
      setUploading(false);
    }
  };

  // ── Guardar nombre + bio ────────────────────────────────────────────────────
  const handleSave = async () => {
    if (editLocked) return;
    if (!user?.profileId) {
      showModal("error", t("common.error"), t("profile.profileNotFound"));
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
      showModal("success", t("mobileEditProfile.savedTitle"), t("profile.profileSaved"));
    } catch (err) {
      showModal("error", t("common.error"), err.message ? mapErrorMessage(err.message) : t("profile.saveError"));
    } finally {
      setSaving(false);
    }
  };

  // ── Guardar idioma preferido ────────────────────────────────────────────────
  // Persiste en el perfil y luego cambia la UI (el mensaje se traduce al nuevo idioma).
  const handleSaveLanguage = async () => {
    if (!user?.profileId || selectedLanguage === language) return;
    setSavingLanguage(true);
    setLanguageSaveResult(null);
    try {
      const list = languages.length ? languages : await getActiveLanguages();
      const match = list.find((l) => l.code === selectedLanguage);
      if (!match) throw new Error("Language not found");
      const token = await AsyncStorage.getItem("accessToken");
      await profileService.updateProfile(user.profileId, { preferredLanguageId: match.languageId }, token);
      await changeUILanguage(selectedLanguage);
      setLanguageSaveResult("success");
    } catch {
      setLanguageSaveResult("error");
    } finally {
      setSavingLanguage(false);
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
      setDeactivateError(t("mobileEditProfile.deactivate.passwordRequired"));
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
      setDeactivateError(err.message ? mapErrorMessage(err.message) : t("mobileEditProfile.deactivate.error"));
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
        <Text style={styles.headerTitle}>{t("profile.myProfile")}</Text>
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
            <Text style={styles.changePhotoText}>{uploading ? t("mobileEditProfile.uploading") : t("mobileEditProfile.changePhoto")}</Text>
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
              {t("profile.personalInfo")}
            </Text>
          </View>

          {loadingProfile ? (
            <ActivityIndicator color={colors.primary} style={{ paddingVertical: 20 }} />
          ) : (
            <>
              {/* Nombre completo */}
              <Text style={styles.label}>{t("profile.fullName")}</Text>
              <TextInput
                value={fullName}
                onChangeText={setFullName}
                placeholder={t("profile.fullNamePlaceholder")}
                placeholderTextColor={colors.textMuted}
                editable={!editLocked}
                style={[styles.input, editLocked && { opacity: 0.75 }]}
              />

              {/* Biografía */}
              <View style={{ flexDirection: "row", alignItems: "center", gap: 8, marginTop: 16 }}>
                <Text style={styles.label}>{t("profile.bio")}</Text>
                {!!translatedBio && (
                  <Text style={[styles.label, { color: colors.accent, textTransform: "none" }]}>
                    {t("profile.translation.translated", { lang: language.toUpperCase() })}
                  </Text>
                )}
              </View>
              <TextInput
                value={translatedBio ?? bio}
                onChangeText={(text) => text.length <= MAX_BIO && setBio(text)}
                placeholder={t("profile.bioPlaceholder")}
                placeholderTextColor={colors.textMuted}
                multiline
                numberOfLines={3}
                editable={!editLocked}
                style={[styles.input, styles.inputMultiline, editLocked && { opacity: 0.75 }]}
              />
              {translatedBio ? (
                <Text style={[styles.charCount, { color: colors.accent }]}>{t("profile.biographyTranslatedAutomatically")}</Text>
              ) : translationGenerating && language !== "es" ? (
                <View style={{ flexDirection: "row", alignItems: "center", gap: 8, marginTop: 6 }}>
                  <ActivityIndicator size="small" color={colors.accent} />
                  <Text style={{ color: colors.textMuted, fontSize: 12, flex: 1 }}>
                    {t("profile.translation.generating")} {t("profile.translation.autoUpdate")}
                  </Text>
                </View>
              ) : (
                <Text style={styles.charCount}>{bio.length}/{MAX_BIO}</Text>
              )}

              {/* Guardar */}
              <TouchableOpacity
                style={[styles.saveBtn, (saving || editLocked) && { opacity: 0.6 }]}
                onPress={handleSave}
                disabled={saving || editLocked}
                activeOpacity={0.8}
              >
                {saving
                  ? <ActivityIndicator color={colors.textPrimary} size="small" />
                  : <Text style={styles.saveBtnText}>{t("profile.saveChanges")}</Text>
                }
              </TouchableOpacity>
              {editLocked && (
                <Text style={{ color: colors.textMuted, fontSize: 12, marginTop: 10, lineHeight: 17 }}>
                  {t("profile.spanishOnlyEdit")}
                </Text>
              )}
            </>
          )}
        </View>

        {/* ── Idioma preferido ── */}
        <View style={styles.section}>
          <View style={{ flexDirection: "row", alignItems: "center", gap: 8, marginBottom: 14 }}>
            <Ionicons name="language-outline" size={17} color={colors.primary} />
            <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700" }}>
              {t("profile.languagePreferred")}
            </Text>
          </View>

          <View style={{ gap: 8 }}>
            {LANGUAGES.map((lng) => {
              const isSelected = selectedLanguage === lng.code;
              return (
                <TouchableOpacity
                  key={lng.code}
                  onPress={() => { setSelectedLanguage(lng.code); setLanguageSaveResult(null); }}
                  disabled={savingLanguage}
                  activeOpacity={0.75}
                  style={{
                    flexDirection: "row", alignItems: "center", gap: 10,
                    paddingHorizontal: 14, paddingVertical: 11, borderRadius: 12,
                    borderWidth: 1,
                    borderColor: isSelected ? colors.primary + "66" : colors.surfaceAlt,
                    backgroundColor: isSelected ? colors.primary + "1A" : colors.background,
                    opacity: savingLanguage ? 0.5 : 1,
                  }}
                >
                  <Text style={{ fontSize: 18 }}>{lng.flag}</Text>
                  <Text style={{
                    flex: 1, fontSize: 14, fontWeight: "500",
                    color: isSelected ? colors.textPrimary : colors.textSecondary,
                  }}>
                    {lng.label} ({lng.code.toUpperCase()})
                  </Text>
                  {/* Radio */}
                  <View style={{
                    width: 16, height: 16, borderRadius: 8, borderWidth: 2,
                    borderColor: isSelected ? colors.primary : colors.surfaceAlt,
                    backgroundColor: isSelected ? colors.primary : "transparent",
                    alignItems: "center", justifyContent: "center",
                  }}>
                    {isSelected && <View style={{ width: 6, height: 6, borderRadius: 3, backgroundColor: colors.textPrimary }} />}
                  </View>
                </TouchableOpacity>
              );
            })}
          </View>

          {/* Cambios sin guardar */}
          {selectedLanguage !== language && !languageSaveResult && (
            <View style={{
              flexDirection: "row", alignItems: "flex-start", gap: 8, marginTop: 12,
              paddingHorizontal: 12, paddingVertical: 9, borderRadius: 10,
              backgroundColor: colors.accent + "1A", borderWidth: 1, borderColor: colors.accent + "4D",
            }}>
              <Ionicons name="information-circle" size={15} color={colors.accent} />
              <Text style={{ color: colors.accent, fontSize: 12, flex: 1 }}>{t("profile.themeSection.unsavedChanges")}</Text>
            </View>
          )}

          {/* Resultado del guardado */}
          {languageSaveResult && (
            <View style={{
              flexDirection: "row", alignItems: "center", gap: 8, marginTop: 12,
              paddingHorizontal: 12, paddingVertical: 9, borderRadius: 10,
              backgroundColor: (languageSaveResult === "success" ? colors.success : colors.error) + "1A",
              borderWidth: 1,
              borderColor: (languageSaveResult === "success" ? colors.success : colors.error) + "4D",
            }}>
              <Ionicons
                name={languageSaveResult === "success" ? "checkmark-circle" : "alert-circle"}
                size={15}
                color={languageSaveResult === "success" ? colors.success : colors.error}
              />
              <Text style={{ color: languageSaveResult === "success" ? colors.success : colors.error, fontSize: 12, flex: 1 }}>
                {languageSaveResult === "success" ? t("profile.languageUpdated") : t("profile.languageUpdateError")}
              </Text>
            </View>
          )}

          {/* Guardar idioma */}
          {selectedLanguage !== language && (
            <TouchableOpacity
              style={[styles.saveBtn, savingLanguage && { opacity: 0.6 }]}
              onPress={handleSaveLanguage}
              disabled={savingLanguage}
              activeOpacity={0.8}
            >
              {savingLanguage
                ? <ActivityIndicator color={colors.textPrimary} size="small" />
                : <Text style={styles.saveBtnText}>{t("profile.themeSection.saveLanguage")}</Text>
              }
            </TouchableOpacity>
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
                {t("profile.customizePanel.customizeTheme")}
              </Text>
              <Text style={{ color: colors.textMuted, fontSize: 12, marginTop: 2 }}>
                {t("mobileEditProfile.themeDesc")}
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
              {t("profile.accountSecurity")}
            </Text>
          </View>

          {/* Cerrar sesión */}
          <View style={{
            flexDirection: "row", alignItems: "center", justifyContent: "space-between",
            paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: colors.primary + "18",
          }}>
            <View style={{ flex: 1, paddingRight: 12 }}>
              <Text style={{ color: colors.textPrimary, fontSize: 14, fontWeight: "600" }}>
                {t("profile.logout")}
              </Text>
              <Text style={{ color: colors.textMuted, fontSize: 12, marginTop: 2 }}>
                {t("profile.logoutDescription")}
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
              <Text style={{ color: colors.accent, fontSize: 13, fontWeight: "700" }}>{t("mobileEditProfile.exit")}</Text>
            </TouchableOpacity>
          </View>

          {/* Zona de peligro */}
          <View style={{
            marginTop: 14, borderRadius: 12, padding: 14,
            backgroundColor: colors.error + "0D",
            borderWidth: 1, borderColor: colors.error + "40",
          }}>
            <Text style={{ color: colors.error, fontSize: 13, fontWeight: "800" }}>
              {t("profile.dangerZone")}
            </Text>
            <Text style={{ color: colors.textSecondary, fontSize: 12, marginTop: 3, lineHeight: 18 }}>
              {t("mobileEditProfile.deactivateDesc")}
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
                {t("profile.deactivateAccount")}
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
              {t("mobileEditProfile.deactivate.title")}
            </Text>
            <Text style={{ color: colors.textMuted, fontSize: 13, textAlign: "center", lineHeight: 20, marginBottom: 20 }}>
              {deactivateStep === 1
                ? t("mobileEditProfile.deactivate.step1Desc")
                : t("mobileEditProfile.deactivate.step2Desc")}
            </Text>

            {/* ── Paso 1: consecuencias ── */}
            {deactivateStep === 1 && (
              <>
                <View style={{
                  backgroundColor: colors.primary + "0D", borderRadius: 12, padding: 14,
                  borderWidth: 1, borderColor: colors.primary + "25", marginBottom: 20, gap: 10,
                }}>
                  {[
                    t("mobileEditProfile.deactivate.consequence1"),
                    t("mobileEditProfile.deactivate.consequence2"),
                    t("mobileEditProfile.deactivate.consequence3"),
                  ].map((line) => (
                    <View key={line} style={{ flexDirection: "row", alignItems: "flex-start", gap: 8 }}>
                      <View style={{ width: 6, height: 6, borderRadius: 3, backgroundColor: colors.accent, marginTop: 6 }} />
                      <Text style={{ color: colors.textSecondary, fontSize: 13, flex: 1, lineHeight: 19 }}>{line}</Text>
                    </View>
                  ))}
                </View>

                <View style={{ flexDirection: "row", gap: 12 }}>
                  <TouchableOpacity
                    onPress={closeDeactivate} activeOpacity={0.8}
                    style={{ flex: 1, paddingVertical: 13, borderRadius: 13, borderWidth: 1, borderColor: colors.primary + "40", alignItems: "center" }}
                  >
                    <Text style={{ color: colors.textSecondary, fontWeight: "700" }}>{t("common.cancel")}</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={() => { setDeactivateStep(2); setDeactivateError(""); }}
                    activeOpacity={0.8}
                    style={{ flex: 1, paddingVertical: 13, borderRadius: 13, backgroundColor: colors.error, alignItems: "center" }}
                  >
                    <Text style={{ color: "#fff", fontWeight: "700" }}>{t("mobileEditProfile.deactivate.continue")}</Text>
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
                    onChangeText={(text) => { setDeactivatePassword(text); setDeactivateError(""); }}
                    placeholder={t("mobileEditProfile.deactivate.passwordPlaceholder")}
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
                    <Text style={{ color: colors.textSecondary, fontWeight: "700" }}>{t("common.back")}</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={handleDeactivate}
                    disabled={deactivating || !deactivatePassword.trim()}
                    activeOpacity={0.8}
                    style={{ flex: 1, paddingVertical: 13, borderRadius: 13, backgroundColor: colors.error, alignItems: "center", opacity: (deactivating || !deactivatePassword.trim()) ? 0.5 : 1 }}
                  >
                    {deactivating
                      ? <ActivityIndicator size="small" color="#fff" />
                      : <Text style={{ color: "#fff", fontWeight: "700" }}>{t("common.confirm")}</Text>
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
