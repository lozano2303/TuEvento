import { useState, useRef, useEffect } from "react";
import {
  View, Text, ScrollView, TouchableOpacity,
  StatusBar, Dimensions, Animated, Image, ActivityIndicator,
  StyleSheet,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";
import { getFileUrl } from "../services/storageService";
import { getPublishedEvents } from "../services/eventService";

const { width } = Dimensions.get("window");

// ─── Helpers ─────────────────────────────────────────────────────────────────
function getDisplayName(fullName) {
  if (!fullName) return "Usuario";
  const parts = fullName.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "Usuario";
  if (parts.length === 1) return parts[0];
  const first = parts[0];
  const second = parts[1];
  if (first.length <= 3) return second.length > first.length ? second : first;
  return first;
}

function getInitial(fullName) {
  return getDisplayName(fullName).charAt(0).toUpperCase();
}

// ─── Componente principal ─────────────────────────────────────────────────────
export default function HomeScreen() {
  const { colors, syncTheme } = useTheme();
  const { user } = useAuth();
  const insets = useSafeAreaInsets();
  const navigation = useNavigation();
  const styles = createStyles(colors);

  // Avatar del usuario en la top bar
  const [avatarUrl, setAvatarUrl]         = useState(null);
  const [avatarLoading, setAvatarLoading] = useState(true);

  // Eventos próximos
  const [upcomingEvents, setUpcomingEvents] = useState([]);
  const [eventsLoading, setEventsLoading] = useState(true);

  useEffect(() => {
    const loadAvatar = async () => {
      if (!user?.storedFileId) {
        setAvatarLoading(false);
        return;
      }
      try {
        const token = await AsyncStorage.getItem("accessToken");
        const url = await getFileUrl(user.storedFileId, token);
        setAvatarUrl(url);
      } catch (_) {
        // mantiene fallback con inicial
      } finally {
        setAvatarLoading(false);
      }
    };
    loadAvatar();
  }, [user?.storedFileId]);

  // Cargar eventos próximos
  useEffect(() => {
    const loadUpcomingEvents = async () => {
      try {
        setEventsLoading(true);
        const data = await getPublishedEvents();
        
        const now = new Date();
        
        // Filtrar eventos futuros, ordenar por fecha ascendente, tomar los 4 más cercanos
        const upcoming = (data || [])
          .filter((event) => {
            if (!event.startDate) return false;
            const eventDate = new Date(event.startDate);
            return eventDate >= now;
          })
          .sort((a, b) => new Date(a.startDate) - new Date(b.startDate))
          .slice(0, 4);
        
        setUpcomingEvents(upcoming);
      } catch (err) {
        console.error("[HomeScreen] Error loading upcoming events:", err);
        setUpcomingEvents([]);
      } finally {
        setEventsLoading(false);
      }
    };
    loadUpcomingEvents();
  }, []);

  // Sincronizar tema con el backend al montar — garantiza que las
  // customizaciones del usuario se apliquen apenas llega al Home
  useEffect(() => {
    syncTheme();
  }, []);

  // Animaciones de entrada con stagger
  const heroAnim   = useRef(new Animated.Value(0)).current;
  const bentoAnim  = useRef(new Animated.Value(0)).current;
  const eventsAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.stagger(120, [
      Animated.spring(heroAnim,   { toValue: 1, useNativeDriver: true, tension: 60, friction: 8 }),
      Animated.spring(bentoAnim,  { toValue: 1, useNativeDriver: true, tension: 60, friction: 8 }),
      Animated.spring(eventsAnim, { toValue: 1, useNativeDriver: true, tension: 60, friction: 8 }),
    ]).start();
  }, []);

  const animStyle = (anim) => ({
    opacity: anim,
    transform: [{ translateY: anim.interpolate({ inputRange: [0, 1], outputRange: [24, 0] }) }],
  });

  const displayName = getDisplayName(user?.fullName);
  const initial     = getInitial(user?.fullName);

  const formatDate = (startDate, finishDate) => {
    if (!startDate) return "Fecha por confirmar";
    
    const start = new Date(startDate);
    const finish = finishDate ? new Date(finishDate) : null;
    
    const options = { day: "numeric", month: "short", year: "numeric" };
    const startStr = start.toLocaleDateString("es-ES", options);
    
    if (finish && finish.getTime() !== start.getTime()) {
      const finishStr = finish.toLocaleDateString("es-ES", options);
      return `${startStr} - ${finishStr}`;
    }
    
    return startStr;
  };

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      {/* Orbes de luz ambiental */}
      <View pointerEvents="none" style={{ position: "absolute", top: -60, left: -60,
        width: 260, height: 260, borderRadius: 130,
        backgroundColor: colors.primary + "1E" }} />
      <View pointerEvents="none" style={{ position: "absolute", top: 180, right: -80,
        width: 220, height: 220, borderRadius: 110,
        backgroundColor: colors.accent + "14" }} />
      <View pointerEvents="none" style={{ position: "absolute", bottom: 120, left: -40,
        width: 180, height: 180, borderRadius: 90,
        backgroundColor: colors.primaryDark + "18" }} />

      {/* ── Top bar ── */}
      <View style={{
        paddingTop: insets.top + 8,
        paddingBottom: 12,
        paddingHorizontal: 24,
        flexDirection: "row",
        alignItems: "center",
        justifyContent: "space-between",
        borderBottomWidth: 1,
        borderBottomColor: colors.primary + "20",
      }}>
        {/* Logo */}
        <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
          <View style={{
            width: 30, height: 30, borderRadius: 8,
            backgroundColor: colors.primary,
            alignItems: "center", justifyContent: "center",
          }}>
            <Ionicons name="calendar" size={16} color={colors.textPrimary} />
          </View>
          <Text style={{ color: colors.textPrimary, fontSize: 17, fontWeight: "800", letterSpacing: 0.2 }}>
            Tu<Text style={{ color: colors.accent }}>Evento</Text>
          </Text>
        </View>

        {/* Avatar */}
        <TouchableOpacity
          onPress={() => navigation.navigate("Perfil")}
          activeOpacity={0.75}
          style={{
            width: 38, height: 38, borderRadius: 19,
            backgroundColor: colors.primary + "40",
            borderWidth: 2, borderColor: colors.accent + "80",
            alignItems: "center", justifyContent: "center",
            overflow: "hidden",
          }}
        >
          {avatarLoading ? (
            <ActivityIndicator color={colors.textPrimary} size="small" />
          ) : avatarUrl ? (
            <Image
              source={{ uri: avatarUrl }}
              style={{ width: "100%", height: "100%" }}
              resizeMode="cover"
            />
          ) : (
            <Text style={{ color: colors.textPrimary, fontSize: 16, fontWeight: "800" }}>
              {getInitial(user?.fullName)}
            </Text>
          )}
        </TouchableOpacity>
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: insets.bottom + 32 }}
      >
        {/* ── Hero ── */}
        <Animated.View style={[{ paddingHorizontal: 24, paddingTop: 28, paddingBottom: 8 }, animStyle(heroAnim)]}>
          <Text style={{ color: colors.textSecondary, fontSize: 14, marginBottom: 8 }}>
            Hola, {displayName} 👋
          </Text>
          <Text style={{ color: colors.textPrimary, fontSize: 26, fontWeight: "800", lineHeight: 34 }}>
            Organiza y vive{"\n"}
            <Text style={{ color: colors.primary }}>eventos</Text>
            <Text style={{ color: colors.textPrimary }}> únicos.</Text>
          </Text>
        </Animated.View>

        {/* ── Bento accesos rápidos ── */}
        <Animated.View style={[{ paddingHorizontal: 24, marginTop: 24 }, animStyle(bentoAnim)]}>
          <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700", marginBottom: 12 }}>
            Accesos rápidos
          </Text>
          <View style={{ flexDirection: "row", gap: 12 }}>
            {/* Card Buscar */}
            <TouchableOpacity
              activeOpacity={0.75}
              style={{
                flex: 1, height: 90,
                backgroundColor: colors.primary + "28",
                borderRadius: 14, borderWidth: 1,
                borderColor: colors.primary + "50",
                padding: 14, justifyContent: "space-between",
              }}
            >
              <View style={{
                width: 36, height: 36, borderRadius: 10,
                backgroundColor: colors.primary + "40",
                alignItems: "center", justifyContent: "center",
              }}>
                <Text style={{ fontSize: 18 }}>🔍</Text>
              </View>
              <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
                <Text style={{ color: colors.textPrimary, fontSize: 13, fontWeight: "700" }}>
                  Buscar{"\n"}eventos
                </Text>
                <View style={{
                  width: 26, height: 26, borderRadius: 13,
                  backgroundColor: colors.primary,
                  alignItems: "center", justifyContent: "center",
                }}>
                  <Ionicons name="arrow-forward" size={13} color={colors.textPrimary} />
                </View>
              </View>
            </TouchableOpacity>

            {/* Card Mis tickets */}
            <TouchableOpacity
              activeOpacity={0.75}
              style={{
                flex: 1, height: 90,
                backgroundColor: colors.success + "28",
                borderRadius: 14, borderWidth: 1,
                borderColor: colors.success + "50",
                padding: 14, justifyContent: "space-between",
              }}
            >
              <View style={{
                width: 36, height: 36, borderRadius: 10,
                backgroundColor: colors.success + "40",
                alignItems: "center", justifyContent: "center",
              }}>
                <Text style={{ fontSize: 18 }}>🎟</Text>
              </View>
              <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
                <Text style={{ color: colors.textPrimary, fontSize: 13, fontWeight: "700" }}>
                  Mis{"\n"}tickets
                </Text>
                <View style={{
                  width: 26, height: 26, borderRadius: 13,
                  backgroundColor: colors.success,
                  alignItems: "center", justifyContent: "center",
                }}>
                  <Ionicons name="arrow-forward" size={13} color={colors.textPrimary} />
                </View>
              </View>
            </TouchableOpacity>
          </View>
        </Animated.View>

        {/* ── Próximos eventos ── */}
        {!eventsLoading && upcomingEvents.length > 0 && (
          <Animated.View style={[{ marginTop: 28 }, animStyle(eventsAnim)]}>
            <Text style={styles.sectionTitle}>
              Próximos eventos
            </Text>

            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.eventsCarousel}
            >
              {upcomingEvents.map((event) => (
                <TouchableOpacity
                  key={event.eventId}
                  activeOpacity={0.75}
                  onPress={() => navigation.navigate("EventDetail", { eventId: event.eventId })}
                  style={styles.eventCard}
                >
                  {/* Imagen de portada */}
                  <View style={styles.eventImageContainer}>
                    {event.coverUrl ? (
                      <Image
                        source={{ uri: event.coverUrl }}
                        style={styles.eventImage}
                        resizeMode="cover"
                      />
                    ) : (
                      <View style={[styles.eventImage, styles.placeholderImage]}>
                        <Ionicons name="calendar" size={32} color={colors.textMuted} />
                      </View>
                    )}
                    
                    {/* Badge de asientos disponibles */}
                    {event.availableSeats > 0 && (
                      <View style={styles.seatsBadge}>
                        <Ionicons name="people" size={10} color={colors.textPrimary} />
                        <Text style={styles.seatsBadgeText}>{event.availableSeats}</Text>
                      </View>
                    )}
                  </View>

                  {/* Información del evento */}
                  <View style={styles.eventInfo}>
                    <Text style={styles.eventName} numberOfLines={2}>
                      {event.eventName}
                    </Text>
                    <View style={styles.eventMetaRow}>
                      <Ionicons name="calendar-outline" size={12} color={colors.textSecondary} />
                      <Text style={styles.eventDate} numberOfLines={1}>
                        {formatDate(event.startDate, event.finishDate)}
                      </Text>
                    </View>
                    {event.siteName && (
                      <View style={styles.eventMetaRow}>
                        <Ionicons name="location-outline" size={12} color={colors.textSecondary} />
                        <Text style={styles.eventLocation} numberOfLines={1}>
                          {event.siteName}
                        </Text>
                      </View>
                    )}
                  </View>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </Animated.View>
        )}
      </ScrollView>
    </View>
  );
}

// ─── Estilos ─────────────────────────────────────────────────────────────────
function createStyles(colors) {
  return StyleSheet.create({
    sectionTitle: {
      color: colors.textPrimary,
      fontSize: 15,
      fontWeight: "700",
      paddingHorizontal: 24,
      marginBottom: 14,
    },
    eventsCarousel: {
      paddingHorizontal: 24,
      gap: 12,
    },
    eventCard: {
      width: 180,
      backgroundColor: colors.surface + "CC",
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.surfaceAlt,
      overflow: "hidden",
    },
    eventImageContainer: {
      width: "100%",
      height: 120,
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
      gap: 3,
      backgroundColor: colors.primary + "E6",
      borderRadius: 10,
      paddingHorizontal: 6,
      paddingVertical: 3,
    },
    seatsBadgeText: {
      fontSize: 10,
      fontWeight: "700",
      color: colors.textPrimary,
    },
    eventInfo: {
      padding: 12,
    },
    eventName: {
      fontSize: 14,
      fontWeight: "700",
      color: colors.textPrimary,
      marginBottom: 8,
      lineHeight: 18,
    },
    eventMetaRow: {
      flexDirection: "row",
      alignItems: "center",
      gap: 4,
      marginBottom: 4,
    },
    eventDate: {
      fontSize: 11,
      color: colors.textSecondary,
      flex: 1,
    },
    eventLocation: {
      fontSize: 11,
      color: colors.textSecondary,
      flex: 1,
    },
  });
}
