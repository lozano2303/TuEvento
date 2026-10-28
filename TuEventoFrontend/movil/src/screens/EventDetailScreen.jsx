import { useState, useEffect, useRef, useCallback } from "react";
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
  Keyboard,
  KeyboardAvoidingView,
  Platform,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useIsFocused, useNavigation, useRoute } from "@react-navigation/native";
import { useTranslation } from "react-i18next";
import { useTheme } from "../context/ThemeContext";
import { useAuth } from "../context/AuthContext";
import { useLanguage } from "../context/LanguageContext";
import { getEventDetail, getEventMedia, getEventLayout, getEventSections } from "../services/eventService";
import * as seatService from "../services/seatService";
import { connectSeatSocket, disconnectSeatSocket } from "../services/websocketService";
import SeatMapCanvas from "../components/SeatMapCanvas";
import CartBubble from "../components/CartBubble";
import EventComments from "../components/EventComments";
import Toast from "../components/Toast";
import AppModal from "../components/AppModal";
import { useToast } from "../hooks/useToast";
import { useSeatStore } from "../hooks/useSeatStore";

const { width } = Dimensions.get("window");

// Secciones que se descargan a la vez en segundo plano (para hidratar el carrito)
const SEAT_LOAD_CONCURRENCY = 4;

/**
 * Pantalla de detalle de un evento.
 * Muestra información completa: imagen, nombre, descripción, fechas, ubicación.
 * Todavía NO incluye mapa de sillas ni selección - solo lectura.
 */
export default function EventDetailScreen() {
  const { colors } = useTheme();
  const { user } = useAuth();
  const { t } = useTranslation();
  const { language } = useLanguage();
  const navigation = useNavigation();
  // La pantalla sigue montada cuando se avanza a otra (checkout, pago...): el aviso de
  // evento cancelado solo debe verse mientras el detalle está en primer plano.
  const isFocused = useIsFocused();
  const route = useRoute();
  const styles = createStyles(colors);
  const { toast, showToast, hideToast } = useToast();

  const { eventId } = route.params;
  const currentUserId = user?.userId ?? null;

  const [event, setEvent] = useState(null);
  // Aviso en vivo: el organizador/admin canceló el evento mientras se veía.
  // null = sin aviso; { cause } = cancelado (cause 'ORGANIZER_REVOKED' si se quitó el rol al organizador)
  const [cancelledLive, setCancelledLive] = useState(null);
  const [media, setMedia] = useState([]);
  const [layout, setLayout] = useState(null);
  const [sections, setSections] = useState([]);
  const [selectedSectionId, setSelectedSectionId] = useState(null);
  const [currentSubSectionIndex, setCurrentSubSectionIndex] = useState(0);
  const [currentRowPage, setCurrentRowPage] = useState(0); // Nuevo: paginación por filas
  const [totalRowPages, setTotalRowPages] = useState(1); // Nuevo: total de páginas
  const [currentColPage, setCurrentColPage] = useState(0); // Paginacion horizontal
  const [totalColPages, setTotalColPages] = useState(1);   // Total paginas de columnas

  const MIN_SEAT_TOUCH_RADIUS_PX = 22; // Tamaño táctil mínimo deseado en píxeles
  
  // 🚀 DOD: Usar SeatDataStore para 100k+ sillas
  const { store: seatStore, version: seatsVersion, cart, upsertSeats, updateSeat, getSeat } = useSeatStore(currentUserId);

  // Sillas por sección: cada sección se descarga una sola vez (promesa compartida)
  const sectionLoadsRef = useRef(new Map());

  // ── Teclado de los comentarios ──
  // Con edge-to-edge (Expo 54) Android ya no encoge la ventana al abrir el teclado: el
  // KeyboardAvoidingView hace sitio y, al enfocar un campo, se desplaza lo justo para dejarlo
  // a la vista. Mientras el teclado está abierto se oculta el carrito flotante, que lo tapaba.
  const scrollRef = useRef(null);
  const scrollYRef = useRef(0);
  const keyboardHeightRef = useRef(0);
  const [keyboardVisible, setKeyboardVisible] = useState(false);

  useEffect(() => {
    const showEvent = Platform.OS === "ios" ? "keyboardWillShow" : "keyboardDidShow";
    const hideEvent = Platform.OS === "ios" ? "keyboardWillHide" : "keyboardDidHide";
    const showSub = Keyboard.addListener(showEvent, (e) => {
      keyboardHeightRef.current = e.endCoordinates?.height ?? 0;
      setKeyboardVisible(true);
    });
    const hideSub = Keyboard.addListener(hideEvent, () => {
      keyboardHeightRef.current = 0;
      setKeyboardVisible(false);
    });
    return () => {
      showSub.remove();
      hideSub.remove();
    };
  }, []);

  const scrollInputIntoView = useCallback((input) => {
    if (typeof input?.measureInWindow !== "function") return;
    // Espera a que el teclado termine de abrirse para medir con su altura real
    setTimeout(() => {
      input.measureInWindow((_x, y, _w, h) => {
        const visibleBottom = Dimensions.get("window").height - keyboardHeightRef.current - 24;
        const overflow = y + h - visibleBottom;
        if (overflow > 0) {
          scrollRef.current?.scrollTo({ y: scrollYRef.current + overflow, animated: true });
        }
      });
    }, 300);
  }, []);
  const [loadedSectionIds, setLoadedSectionIds] = useState(() => new Set());

  const ensureSectionSeats = useCallback((sectionId) => {
    const loads = sectionLoadsRef.current;
    if (!loads.has(sectionId)) {
      const promise = seatService.getSeatsBySection(sectionId)
        .then((data) => {
          upsertSeats(data || []);
          setLoadedSectionIds((prev) => new Set(prev).add(sectionId));
          return data || [];
        })
        .catch((err) => {
          loads.delete(sectionId); // permitir reintento al volver a abrir la sección
          throw err;
        });
      loads.set(sectionId, promise);
    }
    return loads.get(sectionId);
  }, [upsertSeats]);
  
  const [selectedQuantity, setSelectedQuantity] = useState(1);
  const [reserving, setReserving] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [canvasSize, setCanvasSize] = useState({ width: 0, height: 0 });

  // Carrito ya viene del hook useSeatStore

  // Agrupar elementos de layout por backendSectionId
  const groupedSections = (() => {
    if (!layout || !layout.elements) return {};
    
    const groups = {};
    layout.elements.forEach((el) => {
      if (el.type === 'section' && el.backendSectionId) {
        const key = el.backendSectionId;
        if (!groups[key]) groups[key] = [];
        groups[key].push(el);
      }
    });
    return groups;
  })();

  // Sub-secciones de la sección actualmente seleccionada
  const currentSubSections = selectedSectionId ? (groupedSections[selectedSectionId] || []) : [];
  const hasMultipleSubSections = currentSubSections.length > 1;

  // Reset de índice al cambiar de sección
  useEffect(() => {
    setCurrentSubSectionIndex(0);
    setCurrentRowPage(0);
    setCurrentColPage(0);
  }, [selectedSectionId]);

  // Hidratar el stepper con el número de sillas ya reservadas
  useEffect(() => {
    if (cart.length > 0 && cart.length > selectedQuantity) {
      setSelectedQuantity(cart.length);
    }
  }, [cart.length]);

  useEffect(() => {
    const loadEventDetail = async () => {
      try {
        setLoading(true);
        setError(null);

        // Detalle (traducido al idioma activo), imágenes, layout y secciones en paralelo
        const [eventData, mediaData, layoutData, sectionsData] = await Promise.all([
          getEventDetail(eventId, language),
          getEventMedia(eventId),
          getEventLayout(eventId),
          getEventSections(eventId),
        ]);
        setEvent(eventData);

        // Procesar URLs de imágenes (reemplazar localhost por MinIO host)
        const processedMedia = (mediaData || []).map((item) => ({
          ...item,
          imgUrl: item.imgUrl
            ? item.imgUrl.replace("localhost", process.env.EXPO_PUBLIC_MINIO_HOST ?? "localhost")
            : null,
        }));
        setMedia(processedMedia);

        // Layout del evento (puede ser null si no existe)
        if (layoutData && layoutData.layoutData) {
          setLayout(JSON.parse(layoutData.layoutData));
        }
        setSections(sectionsData || []);

        // Con sesión, las sillas de todas las secciones se cargan en segundo plano para
        // detectar reservas existentes (stepper y carrito); la pantalla no las espera.
        // Sin sesión basta con cargar cada sección al abrirla.
        if (sectionsData && sectionsData.length > 0 && currentUserId) {
          void loadAllSectionsInBackground(sectionsData);
        }
      } catch (err) {
        console.error("[EventDetailScreen] Error loading event:", err);
        setError(err.message || "Error al cargar el evento");
      } finally {
        setLoading(false);
      }
    };

    const loadAllSectionsInBackground = async (sectionsData) => {
      const queue = sectionsData.map((section) => section.eventSectionId);
      const worker = async () => {
        while (queue.length > 0) {
          const sectionId = queue.shift();
          try {
            await ensureSectionSeats(sectionId);
          } catch (err) {
            console.warn(`[EventDetailScreen] Could not load seats for section ${sectionId}:`, err);
          }
        }
      };
      await Promise.all(Array.from({ length: Math.min(SEAT_LOAD_CONCURRENCY, queue.length) }, worker));
    };

    if (eventId) {
      void loadEventDetail();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventId, currentUserId, language]);

  // Cargar sillas al seleccionar una sección
  useEffect(() => {
    if (!selectedSectionId) {
      // No limpiar sillas - mantener las reservadas en memoria para el carrito
      return;
    }

    const loadSeatsForSection = async () => {
      try {
        await ensureSectionSeats(selectedSectionId);

        // CASO 5: Detectar sección sin sillas disponibles (estado actual del store)
        const indices = seatStore.getSortedSectionIndices(selectedSectionId);
        const hasAvailable = indices.some((idx) => seatStore.statuses[idx] === 0);
        if (indices.length > 0 && !hasAvailable) {
          showToast('section-no-seats', t("mobileSeats.sectionSoldOut"), 'warning');
        }
      } catch (err) {
        console.error("[EventDetailScreen] Error loading seats:", err);
        Alert.alert(t("common.error"), t("mobileSeats.loadError"));
      }
    };

    void loadSeatsForSection();
  }, [selectedSectionId, showToast, ensureSectionSeats]);

  // Conectar WebSocket al montar
  useEffect(() => {
    if (!event?.eventId) {
      return;
    }
    
    let wsClient = null;
    let isCancelled = false;
    let reconnectAttempts = 0;
    const MAX_RECONNECT_ATTEMPTS = 3;

    const connectWS = async () => {
      try {
        const client = await connectSeatSocket(event.eventId, (evt) => {
          // 🚀 DOD: Actualizar silla en el store
          updateSeat(evt.seatId, {
            status: evt.newStatus,
            reservedBy: evt.newStatus === 'RESERVED' ? evt.changedBy : null,
            reservedUntil: evt.reservedUntil,
          });
        }, (statusEvt) => {
          // El organizador/admin canceló el evento mientras se veía
          if (statusEvt?.type === 'EVENT_CANCELLED') setCancelledLive({ cause: statusEvt.cause ?? null });
        });

        if (!isCancelled && client) {
          wsClient = client;
          reconnectAttempts = 0; // Reset en conexión exitosa
          
          // CASO 6: Detectar desconexión de WebSocket
          if (client.ws) {
            client.ws.onclose = () => {
              if (!isCancelled && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                showToast('ws-disconnected', t("mobileSeats.wsDisconnected"), 'warning');
                reconnectAttempts++;
                setTimeout(connectWS, 2000 * reconnectAttempts); // Backoff exponencial
              }
            };
          }
        } else if (client) {
          // Si ya nos desmontamos, desconectar inmediatamente
          disconnectSeatSocket(client);
        }
      } catch (err) {
        console.warn('[EventDetailScreen] WebSocket connection failed:', err);
        if (!isCancelled && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
          showToast('ws-disconnected', t("mobileSeats.wsDisconnected"), 'warning');
          reconnectAttempts++;
          setTimeout(connectWS, 2000 * reconnectAttempts);
        }
      }
    };

    void connectWS();

    return () => {
      isCancelled = true;
      if (wsClient) {
        disconnectSeatSocket(wsClient);
        wsClient = null;
      }
    };
  }, [event?.eventId, showToast]);

  // Handler de reserva de silla (optimistic UI + rollback)
  const handleReserveSeat = async (seatId) => {
    // CASO 9: Usuario anónimo intenta reservar
    if (!currentUserId) {
      showToast('auth-required', t("seats.loginRequired"), 'warning');
      return;
    }

    // CASO 11: Evento ya no disponible (COMPLETED/CANCELLED)
    if (event.status === 'COMPLETED' || event.status === 'CANCELLED') {
      showToast('event-unavailable', t("seats.eventUnavailable"), 'error');
      return;
    }

    const previous = getSeat(seatId);

    // Actualización optimista usando el store
    updateSeat(seatId, {
      status: 'RESERVED',
      reservedBy: currentUserId,
      reservedUntil: new Date(Date.now() + 10 * 60 * 1000).toISOString(),
    });

    try {
      const result = await seatService.reserveSeat(seatId);
      updateSeat(seatId, result);
    } catch (err) {
      // Rollback en caso de error
      if (previous) {
        updateSeat(seatId, previous);
      }
      
      // CASO 10: Token expirado (401)
      if (err.message?.includes('401') || err.message?.toLowerCase().includes('unauthorized')) {
        showToast('token-expired', t("auth.sessionExpired"), 'error');
      }
      // CASO 3: Silla ya reservada por otro (condición de carrera)
      else if (err.message?.includes('SEAT_NOT_AVAILABLE') || err.message?.includes('no está disponible')) {
        showToast('seat-taken', t("seats.alreadyTaken"), 'warning');
      }
      // CASO 7: Error genérico
      else {
        showToast('reserve-error', t("mobileSeats.reserveError"), 'error');
      }
    }
  };

  // Handler de liberación de silla (optimistic UI + rollback)
  const handleReleaseSeat = async (seatId) => {
    const previous = getSeat(seatId);

    // Actualización optimista usando el store
    updateSeat(seatId, {
      status: 'AVAILABLE',
      reservedBy: null,
      reservedUntil: null,
    });

    try {
      const result = await seatService.releaseSeat(seatId);
      updateSeat(seatId, result);
    } catch (err) {
      // Rollback en caso de error
      if (previous) {
        updateSeat(seatId, previous);
      }
      
      // CASO 10: Token expirado (401)
      if (err.message?.includes('401') || err.message?.toLowerCase().includes('unauthorized')) {
        showToast('token-expired', t("auth.sessionExpired"), 'error');
      }
      // CASO 7: Error genérico al liberar
      else {
        showToast('release-error', t("mobileSeats.releaseError"), 'error');
      }
    }
  };

  // Handler de expiración optimista de silla (solo cliente, sin llamada al backend)
  // El scheduler del backend se encargará de liberar la silla en su próximo ciclo (cada 10s)
  const handleSeatExpire = (seatId) => {
    // CASO 4: Silla expirada
    showToast('seat-expired', t("seats.expired"), 'info');
    
    updateSeat(seatId, {
      status: 'AVAILABLE',
      reservedBy: null,
      reservedUntil: null,
    });
  };

  // Handler de tap en silla
  const onSeatPress = (seatId) => {
    const seat = getSeat(seatId);
    if (!seat) return;

    if (seat.status === 'AVAILABLE') {
      // CASO 1: Límite del stepper alcanzado
      if (cart.length >= selectedQuantity) {
        showToast('stepper-limit', t("mobileSeats.addSeatFirst"), 'warning');
        return;
      }
      void handleReserveSeat(seatId);
    } else if (seat.status === 'RESERVED' && seat.reservedBy === currentUserId) {
      void handleReleaseSeat(seatId);
    }
    // Resto de estados: no-op (silla no clickeable)
  };

  // Contenido a mostrar: usa la traducción si el idioma no es español y existe.
  const displayName = language !== "es" && event?.translatedName ? event.translatedName : event?.eventName;
  const displayDescription = language !== "es" && event?.translatedDescription ? event.translatedDescription : event?.description;
  const showTranslating = language !== "es" && event?.translationGenerating;

  const formatDate = (startDate, finishDate) => {
    if (!startDate) return t("events.dateToConfirm");

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
      DRAFT: t("events.status.draft"),
      PUBLISHED: t("events.status.published"),
      CANCELLED: t("events.status.cancelled"),
      COMPLETED: t("events.status.completed"),
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
          <Text style={styles.headerTitle}>{t("eventDetail.title")}</Text>
          <View style={{ width: 40 }} />
        </View>

        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={colors.primary} />
          <Text style={styles.loadingText}>{t("eventDetail.loading")}</Text>
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
          <Text style={styles.headerTitle}>{t("eventDetail.title")}</Text>
          <View style={{ width: 40 }} />
        </View>

        <View style={styles.errorContainer}>
          <View style={styles.errorIconContainer}>
            <Ionicons name="alert-circle-outline" size={64} color={colors.error} />
          </View>
          <Text style={styles.errorTitle}>{t("eventDetail.errorTitle")}</Text>
          <Text style={styles.errorMessage}>{error || t("eventDetail.notFound")}</Text>
          <TouchableOpacity
            style={styles.retryButton}
            onPress={() => navigation.goBack()}
            activeOpacity={0.75}
          >
            <Ionicons name="arrow-back" size={18} color={colors.textPrimary} />
            <Text style={styles.retryButtonText}>{t("common.back")}</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  // Imagen de portada (primera imagen disponible)
  const coverImage = media.length > 0 ? media[0].imgUrl : null;

  // Pago desde el modal del carrito
  const handleCheckout = () => {
    const seatIds = cart.map((s) => s.seatId);
    const cartItems = cart.map((s) => {
      const section = sections.find((sec) => sec.eventSectionId === s.eventSectionId);
      return {
        seatId:      s.seatId,
        code:        s.code,
        sectionName: section?.sectionTypeName ?? t("seats.section"),
        price:       section?.price ?? 0,
      };
    });
    navigation.navigate("Checkout", {
      eventId,
      seatIds,
      cartItems,
      eventTitle: displayName ?? event?.eventName,
    });
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      <KeyboardAvoidingView style={styles.keyboardAvoider} behavior="padding">
      <ScrollView
        ref={scrollRef}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode={Platform.OS === "ios" ? "interactive" : "on-drag"}
        onScroll={(e) => { scrollYRef.current = e.nativeEvent.contentOffset.y; }}
        scrollEventThrottle={32}
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
          <Text style={styles.eventTitle}>{displayName}</Text>

          {/* Indicador de traducción en curso */}
          {showTranslating && (
            <View style={styles.translatingBanner}>
              <ActivityIndicator size="small" color={colors.accent} />
              <Text style={styles.translatingText}>{t("events.translation.generating")}</Text>
            </View>
          )}

          {/* Información básica */}
          <View style={styles.infoSection}>
            {/* Fecha */}
            <View style={styles.infoRow}>
              <View style={styles.infoIcon}>
                <Ionicons name="calendar-outline" size={20} color={colors.accent} />
              </View>
              <View style={styles.infoContent}>
                <Text style={styles.infoLabel}>{t("events.startDate")}</Text>
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
                  <Text style={styles.infoLabel}>{t("events.location")}</Text>
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
                <Text style={styles.infoLabel}>{t("eventDetail.availability")}</Text>
                <Text style={styles.infoValue}>
                  {event.availableSeats > 0 
                    ? t("events.availableSeats", { count: event.availableSeats })
                    : t("events.soldOut")}
                </Text>
              </View>
            </View>
          </View>

          {/* Descripción */}
          {displayDescription && (
            <View style={styles.descriptionSection}>
              <Text style={styles.sectionTitle}>{t("events.description")}</Text>
              <Text style={styles.descriptionText}>{displayDescription}</Text>
            </View>
          )}

          {/* Galería de imágenes (si hay más de una) */}
          {media.length > 1 && (
            <View style={styles.gallerySection}>
              <Text style={styles.sectionTitle}>{t("eventDetail.gallery")}</Text>
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
              <Text style={styles.sectionTitle}>{t("seats.seatMap")}</Text>
              
              {/* Selector de cantidad estilo cine */}
              <View style={styles.quantitySelectorContainer}>
                <Text style={styles.quantityLabel}>{t("seats.howMany")}</Text>
                <View style={styles.quantityControls}>
                  <TouchableOpacity
                    onPress={() => {
                      // CASO 8: Bajar el stepper por debajo de lo reservado
                      if (selectedQuantity <= cart.length) {
                        showToast('stepper-below-cart', t("mobileSeats.releaseSeatFirst"), 'warning');
                        return;
                      }
                      if (selectedQuantity > 1) {
                        setSelectedQuantity(selectedQuantity - 1);
                      }
                    }}
                    disabled={selectedQuantity <= 1}
                    style={[
                      styles.quantityButton,
                      selectedQuantity <= 1 && styles.quantityButtonDisabled
                    ]}
                    activeOpacity={0.7}
                  >
                    <Ionicons 
                      name="remove" 
                      size={20} 
                      color={selectedQuantity <= 1 ? colors.textMuted : colors.accent} 
                    />
                  </TouchableOpacity>
                  
                  <Text style={styles.quantityValue}>{selectedQuantity}</Text>
                  
                  <TouchableOpacity
                    onPress={() => {
                      // CASO 2: Límite máximo global de sillas
                      if (selectedQuantity >= 10) {
                        showToast('max-seats', t("mobileSeats.maxSeats", { max: 10 }), 'warning');
                        return;
                      }
                      setSelectedQuantity(selectedQuantity + 1);
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
              </View>
              <Text style={styles.quantityInfo}>
                {t("mobileCart.selectedOf", { count: cart.length, total: selectedQuantity })}
              </Text>

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
                        <Text style={styles.sectionChipAllText}>{t("seats.viewAll")}</Text>
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
              
              {/* Controles de navegación entre sub-secciones */}
              {selectedSectionId && hasMultipleSubSections && (
                <View style={styles.subSectionNavigation}>
                  <TouchableOpacity
                    onPress={() => {
                      if (currentSubSectionIndex > 0) {
                        setCurrentSubSectionIndex(currentSubSectionIndex - 1);
                        setCurrentRowPage(0); // Reset page al cambiar sub-sección
                        setCurrentColPage(0);
                      }
                    }}
                    disabled={currentSubSectionIndex === 0}
                    style={[
                      styles.subSectionButton,
                      currentSubSectionIndex === 0 && styles.subSectionButtonDisabled
                    ]}
                    activeOpacity={0.7}
                  >
                    <Ionicons 
                      name="chevron-back" 
                      size={20} 
                      color={currentSubSectionIndex === 0 ? colors.textMuted : colors.accent} 
                    />
                  </TouchableOpacity>
                  
                  <Text style={styles.subSectionIndicator}>
                    {currentSubSectionIndex + 1}/{currentSubSections.length}
                  </Text>
                  
                  <TouchableOpacity
                    onPress={() => {
                      if (currentSubSectionIndex < currentSubSections.length - 1) {
                        setCurrentSubSectionIndex(currentSubSectionIndex + 1);
                        setCurrentRowPage(0); // Reset page al cambiar sub-sección
                        setCurrentColPage(0);
                      }
                    }}
                    disabled={currentSubSectionIndex === currentSubSections.length - 1}
                    style={[
                      styles.subSectionButton,
                      currentSubSectionIndex === currentSubSections.length - 1 && styles.subSectionButtonDisabled
                    ]}
                    activeOpacity={0.7}
                  >
                    <Ionicons 
                      name="chevron-forward" 
                      size={20} 
                      color={currentSubSectionIndex === currentSubSections.length - 1 ? colors.textMuted : colors.accent} 
                    />
                  </TouchableOpacity>
                </View>
              )}
              
              {/* Controles de paginación por filas */}
              {/* Controles de paginacion horizontal (columnas) */}
              {selectedSectionId && totalColPages > 1 && (
                <View style={styles.rowPaginationNavigation}>
                  <TouchableOpacity
                    onPress={() => {
                      if (currentColPage > 0) {
                        setCurrentColPage(currentColPage - 1);
                      }
                    }}
                    disabled={currentColPage === 0}
                    style={[styles.rowPageButton, currentColPage === 0 && styles.rowPageButtonDisabled]}
                    activeOpacity={0.7}
                  >
                    <Ionicons name="chevron-back" size={20} color={currentColPage === 0 ? colors.textMuted : colors.textSecondary} />
                  </TouchableOpacity>
                  <Text style={styles.rowPageIndicator}>
                    Col {currentColPage + 1}/{totalColPages}
                  </Text>
                  <TouchableOpacity
                    onPress={() => {
                      if (currentColPage < totalColPages - 1) {
                        setCurrentColPage(currentColPage + 1);
                      }
                    }}
                    disabled={currentColPage === totalColPages - 1}
                    style={[styles.rowPageButton, currentColPage === totalColPages - 1 && styles.rowPageButtonDisabled]}
                    activeOpacity={0.7}
                  >
                    <Ionicons name="chevron-forward" size={20} color={currentColPage === totalColPages - 1 ? colors.textMuted : colors.textSecondary} />
                  </TouchableOpacity>
                </View>
              )}

              {selectedSectionId && totalRowPages > 1 && (
                <View style={styles.rowPaginationNavigation}>
                  <TouchableOpacity
                    onPress={() => {
                      if (currentRowPage > 0) {
                        setCurrentRowPage(currentRowPage - 1);
                      }
                    }}
                    disabled={currentRowPage === 0}
                    style={[
                      styles.rowPageButton,
                      currentRowPage === 0 && styles.rowPageButtonDisabled
                    ]}
                    activeOpacity={0.7}
                  >
                    <Ionicons 
                      name="chevron-up" 
                      size={20} 
                      color={currentRowPage === 0 ? colors.textMuted : colors.textSecondary} 
                    />
                  </TouchableOpacity>
                  
                  <Text style={styles.rowPageIndicator}>
                    {currentRowPage + 1}/{totalRowPages}
                  </Text>
                  
                  <TouchableOpacity
                    onPress={() => {
                      if (currentRowPage < totalRowPages - 1) {
                        setCurrentRowPage(currentRowPage + 1);
                      }
                    }}
                    disabled={currentRowPage === totalRowPages - 1}
                    style={[
                      styles.rowPageButton,
                      currentRowPage === totalRowPages - 1 && styles.rowPageButtonDisabled
                    ]}
                    activeOpacity={0.7}
                  >
                    <Ionicons 
                      name="chevron-down" 
                      size={20} 
                      color={currentRowPage === totalRowPages - 1 ? colors.textMuted : colors.textSecondary} 
                    />
                  </TouchableOpacity>
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
                    currentSubSectionIndex={currentSubSectionIndex}
                    currentRowPage={currentRowPage}
                    currentColPage={currentColPage}
                    sections={sections}
                    seatStore={seatStore}
                    storeVersion={seatsVersion}
                    onSeatPress={onSeatPress}
                    onSectionPress={setSelectedSectionId}
                    currentUserId={currentUserId}
                    onRowPagesChange={setTotalRowPages}
                    onColPagesChange={setTotalColPages}
                  />
                )}
                {selectedSectionId && !loadedSectionIds.has(selectedSectionId) && (
                  <View style={styles.seatsLoadingOverlay} pointerEvents="none">
                    <ActivityIndicator size="small" color={colors.primary} />
                    <Text style={styles.seatsLoadingText}>{t("mobileSeats.loadingSeats")}</Text>
                  </View>
                )}
              </View>

            </View>
          )}

          {/* Comentarios, calificaciones y respuestas */}
          <EventComments eventId={eventId} organizerUserId={event.userId} onInputFocus={scrollInputIntoView} />
        </View>
      </ScrollView>
      </KeyboardAvoidingView>

      {/* Carrito flotante (burbuja + modal con resumen y pago); oculto mientras se escribe */}
      <CartBubble
        cart={cart}
        sections={sections}
        selectedQuantity={selectedQuantity}
        reserving={reserving}
        onRelease={handleReleaseSeat}
        onExpire={handleSeatExpire}
        onCheckout={handleCheckout}
        hidden={keyboardVisible}
      />
      {/* Toast de alertas */}
      <Toast toast={toast} onHide={hideToast} />

      {/* Evento cancelado en vivo: solo se puede salir al inicio */}
      <AppModal
        visible={!!cancelledLive && isFocused}
        type="error"
        title={t("eventDetail.cancelledLiveTitle")}
        message={cancelledLive?.cause === "ORGANIZER_REVOKED"
          ? t("eventDetail.cancelledLiveOrganizerMessage")
          : t("eventDetail.cancelledLiveMessage")}
        confirmText={t("eventDetail.cancelledLiveButton")}
        onConfirm={() => {
          setCancelledLive(null);
          // navigate (no reset): vuelve a la ruta de inicio existente sin eliminarla. Un reset
          // quitaría "Main" y su guardia beforeRemove abriría el modal de cerrar sesión.
          navigation.navigate(user ? "Main" : "Landing");
        }}
      />
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
    keyboardAvoider: {
      flex: 1,
    },
    scrollContent: {
      flexGrow: 1,
      paddingBottom: 110, // espacio para la burbuja del carrito
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
    translatingBanner: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
      backgroundColor: colors.accent + "15",
      borderRadius: 10,
      paddingHorizontal: 12,
      paddingVertical: 8,
      marginTop: -8,
      marginBottom: 20,
    },
    translatingText: {
      fontSize: 12,
      color: colors.accent,
      fontWeight: "600",
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
      textAlign: "center",
      marginTop: 6,
      marginBottom: 4,
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
    seatsLoadingOverlay: {
      position: "absolute",
      top: 12,
      alignSelf: "center",
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
      paddingHorizontal: 12,
      paddingVertical: 6,
      borderRadius: 16,
      backgroundColor: "rgba(0,0,0,0.6)",
    },
    seatsLoadingText: {
      color: "#FFFFFF",
      fontSize: 12,
    },
    subSectionNavigation: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      gap: 12,
      paddingVertical: 12,
      marginBottom: 8,
      backgroundColor: colors.surface,
      borderRadius: 12,
      borderWidth: 1,
      borderColor: colors.primary + "20",
    },
    subSectionButton: {
      width: 36,
      height: 36,
      borderRadius: 18,
      backgroundColor: colors.primary + "20",
      alignItems: "center",
      justifyContent: "center",
      borderWidth: 1,
      borderColor: colors.primary + "40",
    },
    subSectionButtonDisabled: {
      opacity: 0.3,
    },
    subSectionIndicator: {
      fontSize: 14,
      fontWeight: "700",
      color: colors.textPrimary,
      minWidth: 50,
      textAlign: "center",
    },
    rowPaginationNavigation: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      gap: 12,
      paddingVertical: 8,
      marginBottom: 8,
      backgroundColor: colors.surfaceAlt,
      borderRadius: 10,
      borderWidth: 1,
      borderColor: colors.textMuted + "30",
    },
    rowPageButton: {
      width: 32,
      height: 32,
      borderRadius: 16,
      backgroundColor: colors.textMuted + "20",
      alignItems: "center",
      justifyContent: "center",
      borderWidth: 1,
      borderColor: colors.textMuted + "40",
    },
    rowPageButtonDisabled: {
      opacity: 0.3,
    },
    rowPageIndicator: {
      fontSize: 12,
      fontWeight: "600",
      color: colors.textSecondary,
      minWidth: 40,
      textAlign: "center",
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
