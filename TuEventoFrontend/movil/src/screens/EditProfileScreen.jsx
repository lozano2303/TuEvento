import { useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
  Image,
  ActivityIndicator,
  Alert,
  StatusBar,
} from "react-native";
import * as ImagePicker from "expo-image-picker";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useTheme } from "../context/ThemeContext";
import { useAuth } from "../context/AuthContext";
import { uploadFile, getFileUrl } from "../services/storageService";
import { profileService } from "../services/profileService";

export default function EditProfileScreen({ navigation, route }) {
  const { colors } = useTheme();
  const { user, setUser } = useAuth();

  const currentAvatarUrl = route.params?.currentAvatarUrl ?? null;
  const [avatarUrl, setAvatarUrl]   = useState(currentAvatarUrl);
  const [uploading, setUploading]   = useState(false);

  const styles = StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: colors.background,
    },
    header: {
      flexDirection: "row",
      alignItems: "center",
      paddingHorizontal: 20,
      paddingTop: 16,
      paddingBottom: 16,
      backgroundColor: colors.surface,
      borderBottomWidth: 1,
      borderBottomColor: colors.surfaceAlt,
    },
    backBtn: { padding: 4 },
    backText: { fontSize: 24, color: colors.accent },
    headerTitle: {
      fontSize: 18,
      fontWeight: "600",
      color: colors.textPrimary,
      marginLeft: 12,
    },
    avatarSection: {
      alignItems: "center",
      paddingVertical: 36,
      backgroundColor: colors.surface,
      marginBottom: 8,
    },
    avatarWrapper: {
      width: 110,
      height: 110,
      borderRadius: 55,
      overflow: "hidden",
      borderWidth: 3,
      borderColor: colors.accent,
    },
    avatar: {
      width: "100%",
      height: "100%",
    },
    avatarPlaceholder: {
      width: 110,
      height: 110,
      borderRadius: 55,
      backgroundColor: colors.primary,
      alignItems: "center",
      justifyContent: "center",
      borderWidth: 3,
      borderColor: colors.accent,
    },
    avatarInitial: {
      fontSize: 44,
      color: colors.textPrimary,
      fontWeight: "600",
    },
    uploadingOverlay: {
      position: "absolute",
      width: 110,
      height: 110,
      borderRadius: 55,
      backgroundColor: "rgba(0,0,0,0.45)",
      alignItems: "center",
      justifyContent: "center",
    },
    changePhotoBtn: {
      marginTop: 14,
      paddingHorizontal: 20,
      paddingVertical: 8,
      borderRadius: 20,
      borderWidth: 1.5,
      borderColor: colors.primary,
    },
    changePhotoText: {
      color: colors.primary,
      fontSize: 14,
      fontWeight: "500",
    },
  });

  const pickImage = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== "granted") {
      Alert.alert(
        "Permiso requerido",
        "Necesitamos acceso a tu galería para cambiar la foto de perfil."
      );
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.8,
    });

    if (result.canceled) return;
    await handleUpload(result.assets[0].uri);
  };

  const handleUpload = async (imageUri) => {
    setUploading(true);
    try {
      const token = await AsyncStorage.getItem("accessToken");

      // 1. Subir archivo a storage
      const uploaded = await uploadFile(imageUri, "PROFILE_PICTURE", token);
      const newStoredFileId = uploaded.storedFileId ?? uploaded.id ?? uploaded.fileId;

      // 2. Actualizar perfil con el nuevo storedFileId
      await profileService.updateProfile(user.profileId, { storedFileId: newStoredFileId }, token);

      // 3. Obtener URL pública del archivo
      const newUrl = await getFileUrl(newStoredFileId, token);

      // 4. Actualizar estado local y contexto
      setAvatarUrl(newUrl);
      setUser((prev) => ({ ...prev, storedFileId: newStoredFileId }));

      Alert.alert("¡Listo!", "Tu foto de perfil fue actualizada.");
    } catch (err) {
      Alert.alert("Error", err.message ?? "No se pudo actualizar la foto.");
    } finally {
      setUploading(false);
    }
  };

  const initial = user?.fullName?.charAt(0)?.toUpperCase() ?? "?";

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={colors.surface} />

      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity style={styles.backBtn} onPress={() => navigation.goBack()}>
          <Text style={styles.backText}>←</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Editar perfil</Text>
      </View>

      <ScrollView>
        {/* Sección avatar */}
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

          <TouchableOpacity
            style={styles.changePhotoBtn}
            onPress={pickImage}
            disabled={uploading}
          >
            <Text style={styles.changePhotoText}>
              {uploading ? "Subiendo..." : "Cambiar foto"}
            </Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
