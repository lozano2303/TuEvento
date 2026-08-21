import { useState, useEffect } from "react";
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Image,
  StyleSheet,
  StatusBar,
  Dimensions,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useRoute } from "@react-navigation/native";
import { useTheme } from "../context/ThemeContext";
import { getEventDetail, getEventMedia } from "../services/eventService";

const { width } = Dimensions.get("window");

/**
 * Pantalla de detalle de un evento.
 * Muestra información completa: imagen, nombre, descripción, fechas, ubicación.
 * Todavía NO incluye mapa de sillas ni selección - solo lectura.
 */
export default function EventDetailScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const route = useRoute();
  const styles = createStyles(colors);

  const { eventId } = route.params;

  const [event, setEvent] = useState(null);
  const [media, setMedia] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadEventDetail = async () => {
      try {
        setLoading(true);
        setError(null);

        // Cargar detalles del evento
        const eventData = await getEventDetail(eventId);
        setEvent(eventData);

        // Cargar imágenes del evento
        const mediaData = await getEventMedia(eventId);
        
        // Procesar URLs de imágenes (reemplazar localhost por MinIO host)
        const processedMedia = (mediaData || []).map((item) => ({
          ...item,
          imgUrl: item.imgUrl
            ? item.imgUrl.replace("localhost", process.env.EXPO_PUBLIC_MINIO_HOST ?? "localhost")
            : null,
        }));
        setMedia(processedMedia);
      } catch (err) {
        console.error("[EventDetailScreen] Error loading event:", err);
        setError(err.message || "Error al cargar el evento");
      } finally {
        setLoading(false);
      }
    };

    if (eventId) {
      loadEventDetail();
    }
  }, [eventId]);

  const formatDate = (startDate, finishDate) => {
    if (!startDate) return "Fecha por confirmar";

    const start = new Date(startDate);
    const finish = finishDate ? new Date(finishDate) : null;

    const options = { day: "numeric", month: "long", year: "numeric" };
    const startStr = start.toLocaleDateString("es-ES", options);

    if (finish && finish.getTime() !== start.getTime()) {
      const finishStr = finish.toLocaleDateString("es-ES", options);
      return `${startStr} - ${finishStr}`;
    }

    return startStr;
  };

  const getStatusLabel = (status) => {
    const statusMap = {
      DRAFT: "Borrador",
      PUBLISHED: "Publicado",
      CANCELLED: "Cancelado",
      COMPLETED: "Finalizado",
    };
    return statusMap[status] || status;
  };

  const getStatusColor = (status) => {
    const colorMap = {
      DRAFT: colors.textMuted,
      PUBLISHED: colors.success,
      CANCELLED: colors.error,
      COMPLETED: colors.textSecondary,
    };
    return colorMap[status] || colors.textSecondary;
  };

  if (loading) {
    return (
      <SafeAreaView style={styles.container} edges={["top"]}>
        <StatusBar barStyle="light-content" backgroundColor={colors.background} />
        
        {/* Header con botón de volver */}
        <View style={styles.header}>
          <TouchableOpacity
            onPress={() => navigation.goBack()}
            style={styles.backButton}
            activeOpacity={0.75}
          >
            <Ionicons name="arrow-back" size={24} color={colors.textPrimary} />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>Detalle del Evento</Text>
          <View style={{ width: 40 }} />
        </View>

        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={colors.primary} />
          <Text style={styles.loadingText}>Cargando evento...</Text>
        </View>
      </SafeAreaView>
    );
  }

  if (error || !event) {
    return (
      <SafeAreaView style={styles.container} edges={["top"]}>
        <StatusBar barStyle="light-content" backgroundColor={colors.background} />
        
        {/* Header con botón de volver */}
        <View style={styles.header}>
          <TouchableOpacity
            onPress={() => navigation.goBack()}
            style={styles.backButton}
            activeOpacity={0.75}
          >
            <Ionicons name="arrow-back" size={24} color={colors.textPrimary} />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>Detalle del Evento</Text>
          <View style={{ width: 40 }} />
        </View>

        <View style={styles.errorContainer}>
          <View style={styles.errorIconContainer}>
            <Ionicons name="alert-circle-outline" size={64} color={colors.error} />
          </View>
          <Text style={styles.errorTitle}>Error al cargar</Text>
          <Text style={styles.errorMessage}>{error || "Evento no encontrado"}</Text>
          <TouchableOpacity
            style={styles.retryButton}
            onPress={() => navigation.goBack()}
            activeOpacity={0.75}
          >
            <Ionicons name="arrow-back" size={18} color={colors.textPrimary} />
            <Text style={styles.retryButtonText}>Volver</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  // Imagen de portada (primera imagen disponible)
  const coverImage = media.length > 0 ? media[0].imgUrl : null;

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />
      
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {/* Imagen de portada */}
        <View style={styles.coverContainer}>
          {coverImage ? (
            <Image
              source={{ uri: coverImage }}
              style={styles.coverImage}
              resizeMode="cover"
            />
          ) : (
            <View style={[styles.coverImage, styles.placeholderCover]}>
              <Ionicons name="calendar" size={80} color={colors.textMuted} />
            </View>
          )}

          {/* Botón de volver flotante sobre la imagen */}
          <SafeAreaView edges={["top"]} style={styles.headerOverlay}>
            <TouchableOpacity
              onPress={() => navigation.goBack()}
              style={styles.backButtonOverlay}
              activeOpacity={0.75}
            >
              <Ionicons name="arrow-back" size={24} color={colors.textPrimary} />
            </TouchableOpacity>
          </SafeAreaView>

          {/* Badge de estado */}
          <View style={styles.statusBadgeOverlay}>
            <View style={[styles.statusBadge, { backgroundColor: getStatusColor(event.status) + "E6" }]}>
              <Text style={styles.statusBadgeText}>{getStatusLabel(event.status)}</Text>
            </View>
          </View>
        </View>

        {/* Contenido del evento */}
        <View style={styles.content}>
          {/* Título */}
          <Text style={styles.eventTitle}>{event.eventName}</Text>

          {/* Información básica */}
          <View style={styles.infoSection}>
            {/* Fecha */}
            <View style={styles.infoRow}>
              <View style={styles.infoIcon}>
                <Ionicons name="calendar-outline" size={20} color={colors.accent} />
              </View>
              <View style={styles.infoContent}>
                <Text style={styles.infoLabel}>Fecha</Text>
                <Text style={styles.infoValue}>{formatDate(event.startDate, event.finishDate)}</Text>
              </View>
            </View>

            {/* Ubicación */}
            {event.siteName && (
              <View style={styles.infoRow}>
                <View style={styles.infoIcon}>
                  <Ionicons name="location-outline" size={20} color={colors.accent} />
                </View>
                <View style={styles.infoContent}>
                  <Text style={styles.infoLabel}>Ubicación</Text>
                  <Text style={styles.infoValue}>{event.siteName}</Text>
                </View>
              </View>
            )}

            {/* Asientos disponibles */}
            <View style={styles.infoRow}>
              <View style={styles.infoIcon}>
                <Ionicons name="people-outline" size={20} color={colors.accent} />
              </View>
              <View style={styles.infoContent}>
                <Text style={styles.infoLabel}>Disponibilidad</Text>
                <Text style={styles.infoValue}>
                  {event.availableSeats > 0 
                    ? `${event.availableSeats} asientos disponibles` 
                    : "Agotado"}
                </Text>
              </View>
            </View>
          </View>

          {/* Descripción */}
          {event.description && (
            <View style={styles.descriptionSection}>
              <Text style={styles.sectionTitle}>Descripción</Text>
              <Text style={styles.descriptionText}>{event.description}</Text>
            </View>
          )}

          {/* Galería de imágenes (si hay más de una) */}
          {media.length > 1 && (
            <View style={styles.gallerySection}>
              <Text style={styles.sectionTitle}>Galería</Text>
              <ScrollView
                horizontal
                showsHorizontalScrollIndicator={false}
                contentContainerStyle={styles.galleryScroll}
              >
                {media.map((item, index) => (
                  <View key={item.mediaId} style={styles.galleryItem}>
                    {item.imgUrl ? (
                      <Image
                        source={{ uri: item.imgUrl }}
                        style={styles.galleryImage}
                        resizeMode="cover"
                      />
                    ) : (
                      <View style={[styles.galleryImage, styles.placeholderGallery]}>
                        <Ionicons name="image-outline" size={32} color={colors.textMuted} />
                      </View>
                    )}
                  </View>
                ))}
              </ScrollView>
            </View>
          )}

          {/* Nota: Selección de sillas se implementará en paso posterior */}
          <View style={styles.noticeSection}>
            <Ionicons name="information-circle-outline" size={20} color={colors.accent} />
            <Text style={styles.noticeText}>
              La selección de asientos estará disponible próximamente
            </Text>
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

function createStyles(colors) {
  return StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: colors.background,
    },
    header: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      paddingHorizontal: 16,
      paddingVertical: 12,
      borderBottomWidth: 1,
      borderBottomColor: colors.primary + "20",
    },
    headerTitle: {
      fontSize: 18,
      fontWeight: "800",
      color: colors.textPrimary,
    },
    backButton: {
      width: 40,
      height: 40,
      borderRadius: 20,
      backgroundColor: colors.surface,
      alignItems: "center",
      justifyContent: "center",
    },
    scrollContent: {
      flexGrow: 1,
    },
    coverContainer: {
      width: width,
      height: 300,
      position: "relative",
    },
    coverImage: {
      width: "100%",
      height: "100%",
    },
    placeholderCover: {
      backgroundColor: colors.surfaceAlt,
      alignItems: "center",
      justifyContent: "center",
    },
    headerOverlay: {
      position: "absolute",
      top: 0,
      left: 0,
      right: 0,
      flexDirection: "row",
      paddingHorizontal: 16,
      paddingTop: 8,
    },
    backButtonOverlay: {
      width: 44,
      height: 44,
      borderRadius: 22,
      backgroundColor: colors.surface + "E6",
      alignItems: "center",
      justifyContent: "center",
      shadowColor: "#000",
      shadowOffset: { width: 0, height: 2 },
      shadowOpacity: 0.25,
      shadowRadius: 4,
      elevation: 4,
    },
    statusBadgeOverlay: {
      position: "absolute",
      top: 16,
      right: 16,
    },
    statusBadge: {
      paddingHorizontal: 12,
      paddingVertical: 6,
      borderRadius: 12,
    },
    statusBadgeText: {
      fontSize: 12,
      fontWeight: "700",
      color: colors.textPrimary,
    },
    content: {
      padding: 20,
    },
    eventTitle: {
      fontSize: 28,
      fontWeight: "800",
      color: colors.textPrimary,
      marginBottom: 20,
      lineHeight: 36,
    },
    infoSection: {
      gap: 16,
      marginBottom: 24,
    },
    infoRow: {
      flexDirection: "row",
      alignItems: "flex-start",
      gap: 12,
    },
    infoIcon: {
      width: 40,
      height: 40,
      borderRadius: 12,
      backgroundColor: colors.primary + "20",
      alignItems: "center",
      justifyContent: "center",
    },
    infoContent: {
      flex: 1,
    },
    infoLabel: {
      fontSize: 12,
      color: colors.textSecondary,
      marginBottom: 2,
    },
    infoValue: {
      fontSize: 15,
      fontWeight: "600",
      color: colors.textPrimary,
      lineHeight: 22,
    },
    descriptionSection: {
      marginBottom: 24,
    },
    sectionTitle: {
      fontSize: 18,
      fontWeight: "700",
      color: colors.textPrimary,
      marginBottom: 12,
    },
    descriptionText: {
      fontSize: 15,
      color: colors.textSecondary,
      lineHeight: 24,
    },
    gallerySection: {
      marginBottom: 24,
    },
    galleryScroll: {
      gap: 12,
    },
    galleryItem: {
      width: 200,
      height: 150,
      borderRadius: 12,
      overflow: "hidden",
    },
    galleryImage: {
      width: "100%",
      height: "100%",
    },
    placeholderGallery: {
      backgroundColor: colors.surfaceAlt,
      alignItems: "center",
      justifyContent: "center",
    },
    noticeSection: {
      flexDirection: "row",
      alignItems: "center",
      gap: 12,
      backgroundColor: colors.primary + "15",
      borderRadius: 12,
      padding: 16,
      borderWidth: 1,
      borderColor: colors.primary + "30",
    },
    noticeText: {
      flex: 1,
      fontSize: 13,
      color: colors.textSecondary,
      lineHeight: 20,
    },
    loadingContainer: {
      flex: 1,
      alignItems: "center",
      justifyContent: "center",
      gap: 16,
    },
    loadingText: {
      fontSize: 14,
      color: colors.textSecondary,
    },
    errorContainer: {
      flex: 1,
      alignItems: "center",
      justifyContent: "center",
      paddingHorizontal: 32,
    },
    errorIconContainer: {
      width: 120,
      height: 120,
      borderRadius: 60,
      backgroundColor: colors.error + "20",
      alignItems: "center",
      justifyContent: "center",
      marginBottom: 24,
    },
    errorTitle: {
      fontSize: 20,
      fontWeight: "800",
      color: colors.textPrimary,
      marginBottom: 12,
      textAlign: "center",
    },
    errorMessage: {
      fontSize: 14,
      color: colors.textSecondary,
      textAlign: "center",
      lineHeight: 22,
      marginBottom: 24,
    },
    retryButton: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
      backgroundColor: colors.primary,
      borderRadius: 12,
      paddingHorizontal: 20,
      paddingVertical: 12,
    },
    retryButtonText: {
      fontSize: 15,
      fontWeight: "700",
      color: colors.textPrimary,
    },
  });
}
