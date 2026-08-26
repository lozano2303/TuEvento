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
  Alert,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useRoute } from "@react-navigation/native";
import { useTheme } from "../context/ThemeContext";
import { useAuth } from "../context/AuthContext";
import { getEventDetail, getEventMedia, getEventLayout, getEventSections } from "../services/eventService";
import * as seatService from "../services/seatService";
import { connectSeatSocket, disconnectSeatSocket } from "../services/websocketService";
import SeatMapCanvas from "../components/SeatMapCanvas";

const { width } = Dimensions.get("window");

/**
 * Pantalla de detalle de un evento.
 * Muestra información completa: imagen, nombre, descripción, fechas, ubicación.
 * Todavía NO incluye mapa de sillas ni selección - solo lectura.
 */
export default function EventDetailScreen() {
  const { colors } = useTheme();
  const { user } = useAuth();
  const navigation = useNavigation();
  const route = useRoute();
  const styles = createStyles(colors);

  const { eventId } = route.params;
  const currentUserId = user?.userId ?? null;

  const [event, setEvent] = useState(null);
  const [media, setMedia] = useState([]);
  const [layout, setLayout] = useState(null);
  const [sections, setSections] = useState([]);
  const [selectedSectionId, setSelectedSectionId] = useState(null);
  const [seats, setSeats] = useState({});
  const [selectedQuantity, setSelectedQuantity] = useState(1);
  const [reserving, setReserving] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [canvasSize, setCanvasSize] = useState({ width: 0, height: 0 });

  // Carrito: sillas reservadas por el usuario actual
  const cart = Object.values(seats).filter(
    (s) => s.status === 'RESERVED' && s.reservedBy === currentUserId
  );

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

        // Cargar layout del evento (puede ser null si no existe)
        const layoutData = await getEventLayout(eventId);
        if (layoutData && layoutData.layoutData) {
          const parsedLayout = JSON.parse(layoutData.layoutData);
          setLayout(parsedLayout);
        }

        // Cargar secciones del evento
        const sectionsData = await getEventSections(eventId);
        setSections(sectionsData || []);
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

  // Cargar sillas al seleccionar una sección
  useEffect(() => {
    if (!selectedSectionId) {
      setSeats({}); // Limpiar sillas si no hay sección seleccionada
      return;
    }

    const loadSeats = async () => {
      try {
        const data = await seatService.getSeatsBySection(selectedSectionId);
        const seatsMap = {};
        data.forEach((s) => {
          seatsMap[s.seatId] = s;
        });
        setSeats(seatsMap);
      } catch (err) {
        console.error("[EventDetailScreen] Error loading seats:", err);
        Alert.alert("Error", "No se pudieron cargar las sillas");
      }
    };

    loadSeats();
  }, [selectedSectionId]);

  // Conectar WebSocket al montar
  useEffect(() => {
    if (!event?.eventId) {
      return;
    }
    
    let wsClient = null;
    let isCancelled = false;

    const connectWS = async () => {
      try {
        const client = await connectSeatSocket(event.eventId, (evt) => {
          setSeats((prev) => {
            // Si la silla no está cargada, ignorar (es de otra sección)
            if (!prev[evt.seatId]) {
              return prev;
            }
            
            return {
              ...prev,
              [evt.seatId]: {
                ...prev[evt.seatId],
                status: evt.newStatus,
                reservedBy: evt.newStatus === 'RESERVED' ? evt.changedBy : null,
                reservedUntil: evt.reservedUntil,
              },
            };
          });
        });

        if (!isCancelled && client) {
          wsClient = client;
        } else if (client) {
          // Si ya nos desmontamos, desconectar inmediatamente
          disconnectSeatSocket(client);
        }
      } catch (err) {
        console.warn('[EventDetailScreen] WebSocket connection failed:', err);
      }
    };

    connectWS();

    return () => {
      isCancelled = true;
      if (wsClient) {
        disconnectSeatSocket(wsClient);
        wsClient = null;
      }
    };
  }, [event?.eventId]);

  // Handler de reserva de silla (optimistic UI + rollback)
  const handleReserveSeat = async (seatId) => {
    if (!currentUserId) {
      Alert.alert("Inicia sesión", "Debes iniciar sesión para reservar sillas");
      return;
    }

    if (cart.length >= selectedQuantity) {
      Alert.alert(
        "Límite alcanzado",
        `Ya seleccionaste ${selectedQuantity} silla(s). Cambia la cantidad si necesitas más.`
      );
      return;
    }

    const previous = seats[seatId];

    // Actualización optimista
    setSeats((prev) => ({
      ...prev,
      [seatId]: {
        ...prev[seatId],
        status: 'RESERVED',
        reservedBy: currentUserId,
        reservedUntil: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
      },
    }));
    setReserving((prev) => new Set(prev).add(seatId));

    try {
      const result = await seatService.reserveSeat(seatId);
      setSeats((prev) => ({ ...prev, [seatId]: result }));
    } catch (err) {
      // Rollback en caso de error
      setSeats((prev) => ({ ...prev, [seatId]: previous }));
      Alert.alert('No se pudo reservar la silla', err.message);
    } finally {
      setReserving((prev) => {
        const next = new Set(prev);
        next.delete(seatId);
        return next;
      });
    }
  };

  // Handler de liberación de silla (optimistic UI + rollback)
  const handleReleaseSeat = async (seatId) => {
    const previous = seats[seatId];

    // Actualización optimista
    setSeats((prev) => ({
      ...prev,
      [seatId]: {
        ...prev[seatId],
        status: 'AVAILABLE',
        reservedBy: null,
        reservedUntil: null,
      },
    }));
    setReserving((prev) => new Set(prev).add(seatId));

    try {
      const result = await seatService.releaseSeat(seatId);
      setSeats((prev) => ({ ...prev, [seatId]: result }));
    } catch (err) {
      // Rollback en caso de error
      setSeats((prev) => ({ ...prev, [seatId]: previous }));
      Alert.alert('No se pudo liberar la reserva', err.message);
    } finally {
      setReserving((prev) => {
        const next = new Set(prev);
        next.delete(seatId);
        return next;
      });
    }
  };

  // Handler de tap en silla
  const onSeatPress = (seatId) => {
    if (reserving.has(seatId)) return; // Evitar doble-tap mientras está en vuelo

    const seat = seats[seatId];
    if (!seat) return;

    if (seat.status === 'AVAILABLE' && cart.length < selectedQuantity) {
      handleReserveSeat(seatId);
    } else if (seat.status === 'RESERVED' && seat.reservedBy === currentUserId) {
      handleReleaseSeat(seatId);
    }
    // Resto de estados: no-op (silla no clickeable)
  };

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

          {/* Mapa de sillas */}
          {layout && layout.elements && layout.elements.length > 0 && (
            <View style={styles.seatMapSection}>
              <Text style={styles.sectionTitle}>Mapa de Asientos</Text>
              
              {/* Selector de cantidad estilo cine */}
              <View style={styles.quantitySelectorContainer}>
                <Text style={styles.quantityLabel}>¿Cuántas sillas querés?</Text>
                <View style={styles.quantityControls}>
                  <TouchableOpacity
                    onPress={() => {
                      if (selectedQuantity > 1 && selectedQuantity > cart.length) {
                        setSelectedQuantity(selectedQuantity - 1);
                      }
                    }}
                    disabled={selectedQuantity <= 1 || selectedQuantity <= cart.length}
                    style={[
                      styles.quantityButton,
                      (selectedQuantity <= 1 || selectedQuantity <= cart.length) && styles.quantityButtonDisabled
                    ]}
                    activeOpacity={0.7}
                  >
                    <Ionicons 
                      name="remove" 
                      size={20} 
                      color={
                        (selectedQuantity <= 1 || selectedQuantity <= cart.length) 
                          ? colors.textMuted 
                          : colors.accent
                      } 
                    />
                  </TouchableOpacity>
                  
                  <Text style={styles.quantityValue}>{selectedQuantity}</Text>
                  
                  <TouchableOpacity
                    onPress={() => {
                      if (selectedQuantity < 10) {
                        setSelectedQuantity(selectedQuantity + 1);
                      }
                    }}
                    disabled={selectedQuantity >= 10}
                    style={[
                      styles.quantityButton,
                      selectedQuantity >= 10 && styles.quantityButtonDisabled
                    ]}
                    activeOpacity={0.7}
                  >
                    <Ionicons 
                      name="add" 
                      size={20} 
                      color={selectedQuantity >= 10 ? colors.textMuted : colors.accent} 
                    />
                  </TouchableOpacity>
                </View>
                <Text style={styles.quantityInfo}>
                  {cart.length} de {selectedQuantity} seleccionadas
                </Text>
              </View>

              {/* Menú de secciones */}
              {sections.length > 0 && (
                <View style={styles.sectionsMenuContainer}>
                  <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator={false}
                    contentContainerStyle={styles.sectionsMenu}
                  >
                    {selectedSectionId && (
                      <TouchableOpacity
                        style={styles.sectionChipAll}
                        onPress={() => setSelectedSectionId(null)}
                        activeOpacity={0.7}
                      >
                        <Ionicons name="apps-outline" size={16} color={colors.textPrimary} />
                        <Text style={styles.sectionChipAllText}>Ver todo</Text>
                      </TouchableOpacity>
                    )}
                    
                    {sections.map((section) => {
                      const isActive = selectedSectionId === section.eventSectionId;
                      return (
                        <TouchableOpacity
                          key={section.eventSectionId}
                          style={[
                            styles.sectionChip,
                            isActive && styles.sectionChipActive,
                          ]}
                          onPress={() => setSelectedSectionId(
                            isActive ? null : section.eventSectionId
                          )}
                          activeOpacity={0.7}
                        >
                          <Text style={[
                            styles.sectionChipName,
                            isActive && styles.sectionChipNameActive,
                          ]}>
                            {section.sectionTypeName}
                          </Text>
                          <Text style={[
                            styles.sectionChipPrice,
                            isActive && styles.sectionChipPriceActive,
                          ]}>
                            ${section.price.toLocaleString()}
                          </Text>
                        </TouchableOpacity>
                      );
                    })}
                  </ScrollView>
                </View>
              )}
              
              <View
                style={styles.seatMapContainer}
                onLayout={(e) => {
                  const { width, height } = e.nativeEvent.layout;
                  setCanvasSize({ width, height });
                }}
              >
                {canvasSize.width > 0 && canvasSize.height > 0 && (
                  <SeatMapCanvas
                    layoutData={layout}
                    containerWidth={canvasSize.width}
                    containerHeight={canvasSize.height}
                    focusedSectionId={selectedSectionId}
                    sections={sections}
                    seats={seats}
                    onSeatPress={onSeatPress}
                    currentUserId={currentUserId}
                  />
                )}
              </View>
            </View>
          )}
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
    seatMapSection: {
      marginBottom: 24,
    },
    quantitySelectorContainer: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: 16,
      paddingHorizontal: 16,
      paddingVertical: 12,
      backgroundColor: colors.surface,
      borderRadius: 12,
      borderWidth: 1,
      borderColor: colors.primary + "20",
    },
    quantityLabel: {
      fontSize: 14,
      fontWeight: "600",
      color: colors.textSecondary,
    },
    quantityControls: {
      flexDirection: "row",
      alignItems: "center",
      gap: 12,
    },
    quantityButton: {
      width: 36,
      height: 36,
      borderRadius: 18,
      backgroundColor: colors.primary + "20",
      alignItems: "center",
      justifyContent: "center",
      borderWidth: 1,
      borderColor: colors.primary + "40",
    },
    quantityButtonDisabled: {
      opacity: 0.3,
    },
    quantityValue: {
      fontSize: 20,
      fontWeight: "800",
      color: colors.textPrimary,
      minWidth: 30,
      textAlign: "center",
    },
    quantityInfo: {
      fontSize: 11,
      color: colors.textMuted,
    },
    sectionsMenuContainer: {
      marginBottom: 16,
    },
    sectionsMenu: {
      paddingVertical: 4,
      gap: 8,
    },
    sectionChipAll: {
      flexDirection: "row",
      alignItems: "center",
      gap: 6,
      paddingHorizontal: 14,
      paddingVertical: 8,
      borderRadius: 20,
      backgroundColor: colors.surface,
      borderWidth: 1,
      borderColor: colors.primary + "40",
    },
    sectionChipAllText: {
      fontSize: 13,
      fontWeight: "600",
      color: colors.textPrimary,
    },
    sectionChip: {
      paddingHorizontal: 14,
      paddingVertical: 8,
      borderRadius: 20,
      backgroundColor: colors.surface,
      borderWidth: 1,
      borderColor: colors.primary + "30",
      gap: 2,
    },
    sectionChipActive: {
      backgroundColor: colors.primary,
      borderColor: colors.primary,
    },
    sectionChipName: {
      fontSize: 13,
      fontWeight: "600",
      color: colors.textPrimary,
    },
    sectionChipNameActive: {
      color: colors.background,
    },
    sectionChipPrice: {
      fontSize: 11,
      fontWeight: "500",
      color: colors.textSecondary,
    },
    sectionChipPriceActive: {
      color: colors.background + "CC",
    },
    seatMapContainer: {
      width: "100%",
      height: 400,
      borderRadius: 12,
      overflow: "hidden",
      backgroundColor: colors.surfaceAlt,
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
