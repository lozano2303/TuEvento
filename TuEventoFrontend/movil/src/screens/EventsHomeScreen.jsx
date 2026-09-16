import { useState, useEffect } from "react";
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  Image,
  StyleSheet,
  RefreshControl,
  StatusBar,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";
import { getPublishedEvents } from "../services/eventService";

/**
 * Pantalla principal del tab "Eventos".
 * Muestra diferentes contenidos según el rol del usuario:
 * - ORGANIZER: Muestra código QR (placeholder por ahora)
 * - Otros roles: Muestra lista de eventos publicados
 */
export default function EventsHomeScreen() {
  const { colors } = useTheme();
  const { user } = useAuth();
  const navigation = useNavigation();
  const styles = createStyles(colors);

  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [processedEvents, setProcessedEvents] = useState([]);

  const loadEvents = async (isRefresh = false) => {
    try {
      if (!isRefresh) setLoading(true);
      setError(null);
      
      const data = await getPublishedEvents();
      setEvents(data || []);

      // Procesar URLs de imágenes (reemplazar localhost por MinIO host)
      const processed = (data || []).map((event) => ({
        ...event,
        coverUrl: event.coverUrl
          ? event.coverUrl.replace("localhost", process.env.EXPO_PUBLIC_MINIO_HOST ?? "localhost")
          : null,
      }));
      setProcessedEvents(processed);
    } catch (err) {
      console.error("[EventsHomeScreen] Error loading events:", err);
      setError(err.message || "Error al cargar eventos");
      setProcessedEvents([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    // Solo cargar eventos si el usuario NO es organizador
    if (user?.role !== "ORGANIZER") {
      loadEvents();
    } else {
      setLoading(false);
    }
  }, [user?.role]);

  const handleRefresh = () => {
    setRefreshing(true);
    loadEvents(true);
  };

  const handleEventPress = (eventId) => {
    navigation.navigate("EventDetail", { eventId });
  };

  const handleSearchPress = () => {
    navigation.navigate("EventSearch");
  };

  const formatDate = (startDate, finishDate) => {
    if (!startDate) return "Fecha por confirmar";
    
    const start = new Date(startDate);
    const finish = finishDate ? new Date(finishDate) : null;
    
    const options = { day: "numeric", month: "short" };
    const startStr = start.toLocaleDateString("es-ES", options);
    
    if (finish && finish.getTime() !== start.getTime()) {
      const finishStr = finish.toLocaleDateString("es-ES", options);
      return `${startStr} - ${finishStr}`;
    }
    
    return startStr;
  };

  // Si el usuario es ORGANIZER, mostrar placeholder de QR
  if (user?.role === "ORGANIZER") {
    return (
      <SafeAreaView style={styles.container} edges={["top"]}>
        <StatusBar barStyle="light-content" backgroundColor={colors.background} />
        
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Código QR</Text>
        </View>

        <View style={styles.qrPlaceholder}>
          <View style={styles.qrIconContainer}>
            <Ionicons name="qr-code" size={80} color={colors.primary} />
          </View>
          <Text style={styles.qrTitle}>Código QR del Organizador</Text>
          <Text style={styles.qrMessage}>
            Aquí se mostrará tu código QR para validación de tickets
          </Text>
        </View>
      </SafeAreaView>
    );
  }

  // Si NO es organizador, mostrar lista de eventos
  const renderEventCard = ({ item }) => (
    <TouchableOpacity
      style={styles.eventCard}
      activeOpacity={0.75}
      onPress={() => handleEventPress(item.eventId)}
    >
      {/* Imagen del evento */}
      <View style={styles.imageContainer}>
        {item.coverUrl ? (
          <Image
            source={{ uri: item.coverUrl }}
            style={styles.eventImage}
            resizeMode="cover"
          />
        ) : (
          <View style={[styles.eventImage, styles.placeholderImage]}>
            <Ionicons name="calendar" size={40} color={colors.textMuted} />
          </View>
        )}
        
        {/* Badge de asientos disponibles */}
        {item.availableSeats > 0 && (
          <View style={styles.seatsBadge}>
            <Ionicons name="people" size={12} color={colors.textPrimary} />
            <Text style={styles.seatsBadgeText}>
              {item.availableSeats}
            </Text>
          </View>
        )}
      </View>

      {/* Información del evento */}
      <View style={styles.eventInfo}>
        <Text style={styles.eventName} numberOfLines={2}>
          {item.eventName}
        </Text>
        
        <View style={styles.eventMeta}>
          <View style={styles.metaRow}>
            <Ionicons name="calendar-outline" size={14} color={colors.textSecondary} />
            <Text style={styles.metaText}>
              {formatDate(item.startDate, item.finishDate)}
            </Text>
          </View>
          
          {item.siteName && (
            <View style={styles.metaRow}>
              <Ionicons name="location-outline" size={14} color={colors.textSecondary} />
              <Text style={styles.metaText} numberOfLines={1}>
                {item.siteName}
              </Text>
            </View>
          )}
        </View>

        {/* Indicador de disponibilidad */}
        {item.availableSeats === 0 && (
          <View style={styles.soldOutBadge}>
            <Text style={styles.soldOutText}>Agotado</Text>
          </View>
        )}
      </View>

      {/* Flecha de navegación */}
      <View style={styles.arrowContainer}>
        <Ionicons name="chevron-forward" size={20} color={colors.accent} />
      </View>
    </TouchableOpacity>
  );

  const renderEmptyState = () => (
    <View style={styles.emptyContainer}>
      <View style={styles.emptyIconContainer}>
        <Ionicons name="calendar-outline" size={64} color={colors.textMuted} />
      </View>
      <Text style={styles.emptyTitle}>No hay eventos disponibles</Text>
      <Text style={styles.emptyMessage}>
        Por ahora no hay eventos publicados.{"\n"}
        Vuelve pronto para ver nuevas opciones.
      </Text>
      <TouchableOpacity
        style={styles.refreshButton}
        onPress={handleRefresh}
        activeOpacity={0.75}
      >
        <Ionicons name="refresh" size={18} color={colors.textPrimary} />
        <Text style={styles.refreshButtonText}>Actualizar</Text>
      </TouchableOpacity>
    </View>
  );

  const renderErrorState = () => (
    <View style={styles.emptyContainer}>
      <View style={styles.emptyIconContainer}>
        <Ionicons name="alert-circle-outline" size={64} color={colors.error} />
      </View>
      <Text style={styles.emptyTitle}>Error al cargar eventos</Text>
      <Text style={styles.emptyMessage}>{error}</Text>
      <TouchableOpacity
        style={styles.refreshButton}
        onPress={handleRefresh}
        activeOpacity={0.75}
      >
        <Ionicons name="refresh" size={18} color={colors.textPrimary} />
        <Text style={styles.refreshButtonText}>Reintentar</Text>
      </TouchableOpacity>
    </View>
  );

  if (loading && !refreshing) {
    return (
      <SafeAreaView style={styles.container} edges={["top"]}>
        <StatusBar barStyle="light-content" backgroundColor={colors.background} />
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Eventos</Text>
        </View>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={colors.primary} />
          <Text style={styles.loadingText}>Cargando eventos...</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />
      
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Eventos</Text>
        <View style={styles.headerActions}>
          <TouchableOpacity
            style={styles.headerButton}
            onPress={handleSearchPress}
            activeOpacity={0.75}
          >
            <Ionicons name="search" size={22} color={colors.accent} />
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.headerButton}
            onPress={handleRefresh}
            activeOpacity={0.75}
          >
            <Ionicons name="refresh" size={22} color={colors.accent} />
          </TouchableOpacity>
        </View>
      </View>

      {/* Lista de eventos */}
      <FlatList
        data={processedEvents}
        renderItem={renderEventCard}
        keyExtractor={(item) => item.eventId.toString()}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={handleRefresh}
            tintColor={colors.primary}
            colors={[colors.primary]}
          />
        }
        ListEmptyComponent={error ? renderErrorState : renderEmptyState}
      />
    </SafeAreaView>
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
      paddingHorizontal: 20,
      paddingVertical: 16,
      borderBottomWidth: 1,
      borderBottomColor: colors.primary + "20",
    },
    headerTitle: {
      fontSize: 24,
      fontWeight: "800",
      color: colors.textPrimary,
    },
    headerActions: {
      flexDirection: "row",
      gap: 8,
    },
    headerButton: {
      width: 40,
      height: 40,
      borderRadius: 20,
      backgroundColor: colors.surface,
      alignItems: "center",
      justifyContent: "center",
    },
    listContent: {
      padding: 16,
      flexGrow: 1,
    },
    eventCard: {
      backgroundColor: colors.surface,
      borderRadius: 16,
      marginBottom: 16,
      overflow: "hidden",
      borderWidth: 1,
      borderColor: colors.surfaceAlt,
      flexDirection: "row",
    },
    imageContainer: {
      width: 120,
      height: 140,
      position: "relative",
    },
    eventImage: {
      width: "100%",
      height: "100%",
    },
    placeholderImage: {
      backgroundColor: colors.surfaceAlt,
      alignItems: "center",
      justifyContent: "center",
    },
    seatsBadge: {
      position: "absolute",
      top: 8,
      right: 8,
      flexDirection: "row",
      alignItems: "center",
      gap: 4,
      backgroundColor: colors.primary + "E6",
      borderRadius: 12,
      paddingHorizontal: 8,
      paddingVertical: 4,
    },
    seatsBadgeText: {
      fontSize: 11,
      fontWeight: "700",
      color: colors.textPrimary,
    },
    eventInfo: {
      flex: 1,
      padding: 12,
      justifyContent: "space-between",
    },
    eventName: {
      fontSize: 16,
      fontWeight: "700",
      color: colors.textPrimary,
      marginBottom: 8,
      lineHeight: 22,
    },
    eventMeta: {
      gap: 6,
    },
    metaRow: {
      flexDirection: "row",
      alignItems: "center",
      gap: 6,
    },
    metaText: {
      fontSize: 13,
      color: colors.textSecondary,
      flex: 1,
    },
    soldOutBadge: {
      alignSelf: "flex-start",
      backgroundColor: colors.error + "20",
      borderRadius: 8,
      paddingHorizontal: 8,
      paddingVertical: 4,
      marginTop: 6,
    },
    soldOutText: {
      fontSize: 11,
      fontWeight: "700",
      color: colors.error,
    },
    arrowContainer: {
      width: 40,
      alignItems: "center",
      justifyContent: "center",
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
    emptyContainer: {
      flex: 1,
      alignItems: "center",
      justifyContent: "center",
      paddingHorizontal: 32,
      paddingVertical: 48,
    },
    emptyIconContainer: {
      width: 120,
      height: 120,
      borderRadius: 60,
      backgroundColor: colors.surfaceAlt,
      alignItems: "center",
      justifyContent: "center",
      marginBottom: 24,
    },
    emptyTitle: {
      fontSize: 20,
      fontWeight: "800",
      color: colors.textPrimary,
      marginBottom: 12,
      textAlign: "center",
    },
    emptyMessage: {
      fontSize: 14,
      color: colors.textSecondary,
      textAlign: "center",
      lineHeight: 22,
      marginBottom: 24,
    },
    refreshButton: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
      backgroundColor: colors.primary,
      borderRadius: 12,
      paddingHorizontal: 20,
      paddingVertical: 12,
    },
    refreshButtonText: {
      fontSize: 15,
      fontWeight: "700",
      color: colors.textPrimary,
    },
    // QR Placeholder styles
    qrPlaceholder: {
      flex: 1,
      alignItems: "center",
      justifyContent: "center",
      paddingHorizontal: 32,
    },
    qrIconContainer: {
      width: 160,
      height: 160,
      borderRadius: 24,
      backgroundColor: colors.primary + "20",
      borderWidth: 2,
      borderColor: colors.primary + "40",
      alignItems: "center",
      justifyContent: "center",
      marginBottom: 24,
    },
    qrTitle: {
      fontSize: 22,
      fontWeight: "800",
      color: colors.textPrimary,
      marginBottom: 12,
      textAlign: "center",
    },
    qrMessage: {
      fontSize: 15,
      color: colors.textSecondary,
      textAlign: "center",
      lineHeight: 22,
    },
  });
}
