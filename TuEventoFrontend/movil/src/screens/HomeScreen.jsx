import { useState, useRef, useEffect, useMemo } from "react";
import {
  View, Text, ScrollView, TouchableOpacity,
  StatusBar, Dimensions, Animated, Image, ActivityIndicator,
  StyleSheet,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useTranslation } from "react-i18next";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";
import { useLanguage } from "../context/LanguageContext";
import { getFileUrl } from "../services/storageService";
import { getPublishedEvents } from "../services/eventService";
import { getAllCategories } from "../services/catalogService";
import { useNotifications } from "../context/NotificationContext";
import EventCard from "../components/EventCard";
import { parseLocalDate, resolveCoverUrl } from "../utils/eventFormat";
import { onEventCancelled, withoutEvent } from "../utils/eventCancellation";

// Pasos de "Cómo funciona" (clave i18n en mobileHome.steps.*)
const STEPS = [
  { key: "explore", icon: "compass-outline" },
  { key: "seat",    icon: "grid-outline" },
  { key: "pay",     icon: "card-outline" },
  { key: "enter",   icon: "qr-code-outline" },
];

const { width } = Dimensions.get("window");
// Ancho de las tarjetas del carrusel: deja ver el inicio de la siguiente
const CARD_WIDTH = Math.min(width * 0.78, 320);

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
  const { t } = useTranslation();
  const { language } = useLanguage();
  const { unreadCount } = useNotifications();
  const insets = useSafeAreaInsets();
  const navigation = useNavigation();
  const styles = createStyles(colors);

  // Avatar del usuario en la top bar
  const [avatarUrl, setAvatarUrl]         = useState(null);
  const [avatarLoading, setAvatarLoading] = useState(true);

  // Eventos publicados y categorías
  const [events, setEvents] = useState([]);

  // Evento cancelado en vivo (WebSocket): se quita del listado sin recargar
  useEffect(() => onEventCancelled((eventId) => setEvents((prev) => withoutEvent(prev, eventId))), []);
  const [categories, setCategories] = useState([]);
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
    void loadAvatar();
  }, [user?.storedFileId]);

  // Cargar eventos publicados y categorías
  useEffect(() => {
    const loadEvents = async () => {
      try {
        setEventsLoading(true);
        const [data, cats] = await Promise.all([
          getPublishedEvents(language),
          getAllCategories(language).catch(() => []),
        ]);
        setEvents((data || []).map((event) => ({ ...event, coverUrl: resolveCoverUrl(event.coverUrl) })));
        setCategories(Array.isArray(cats) ? cats : []);
      } catch (err) {
        console.error("[HomeScreen] Error loading events:", err);
        setEvents([]);
      } finally {
        setEventsLoading(false);
      }
    };
    void loadEvents();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [language]);

  // Próximos 6 eventos (desde hoy), del más cercano al más lejano
  const upcomingEvents = useMemo(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return events
      .filter((e) => {
        const start = parseLocalDate(e.startDate);
        return start && (parseLocalDate(e.finishDate) ?? start) >= today;
      })
      .sort((a, b) => parseLocalDate(a.startDate) - parseLocalDate(b.startDate))
      .slice(0, 6);
  }, [events]);

  // Estadísticas rápidas
  const stats = useMemo(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const weekEnd = new Date(today);
    weekEnd.setDate(weekEnd.getDate() + 7);
    const thisWeek = events.filter((e) => {
      const start = parseLocalDate(e.startDate);
      return start && start < weekEnd && (parseLocalDate(e.finishDate) ?? start) >= today;
    }).length;
    const usedCategories = new Set(events.map((e) => e.categoryId).filter(Boolean));
    return { total: events.length, thisWeek, categories: usedCategories.size };
  }, [events]);

  // Categorías con eventos publicados, ordenadas por cantidad de eventos
  const topCategories = useMemo(() => {
    const counts = {};
    events.forEach((e) => { if (e.categoryId) counts[e.categoryId] = (counts[e.categoryId] ?? 0) + 1; });
    return categories
      .filter((c) => counts[c.categoryId])
      .sort((a, b) => counts[b.categoryId] - counts[a.categoryId])
      .slice(0, 8)
      .map((c) => ({ ...c, count: counts[c.categoryId] }));
  }, [events, categories]);

  const categoryName = (id) => {
    const cat = categories.find((c) => c.categoryId === id);
    return cat ? cat.translatedName || cat.name : null;
  };

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

  // Accesos rápidos: "Eventos" es un tab hermano; el resto son pantallas del stack
  const quickActions = [
    { key: "searchEvents", icon: "search", tint: colors.primary,
      onPress: () => navigation.navigate("Eventos", { focusSearch: true }) },
    { key: "myTickets", icon: "ticket-outline", tint: colors.success,
      onPress: () => navigation.navigate("MyTickets") },
    { key: "wallet", icon: "wallet-outline", tint: colors.accent,
      onPress: () => navigation.navigate("Wallet") },
    { key: "notifications", icon: "notifications-outline", tint: "#F59E0B",
      onPress: () => navigation.navigate("Notifications"), badge: unreadCount },
  ];

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

        {/* Campana + Avatar */}
        <View style={{ flexDirection: "row", alignItems: "center", gap: 10 }}>
          {/* Campana de notificaciones */}
          <TouchableOpacity
            onPress={() => navigation.navigate("Notifications")}
            activeOpacity={0.75}
            style={{ width: 36, height: 36, alignItems: "center", justifyContent: "center" }}
          >
            <Ionicons name="notifications-outline" size={22} color={colors.textSecondary} />
            {unreadCount > 0 && (
              <View style={{
                position: "absolute", top: 0, right: 0,
                backgroundColor: colors.error,
                borderRadius: 9, minWidth: 18, height: 18,
                alignItems: "center", justifyContent: "center",
                paddingHorizontal: 4,
                borderWidth: 1.5, borderColor: colors.background,
              }}>
                <Text style={{ color: "#fff", fontSize: 10, fontWeight: "800" }}>
                  {unreadCount > 99 ? "99+" : unreadCount}
                </Text>
              </View>
            )}
          </TouchableOpacity>

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
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: insets.bottom + 32 }}
      >
        {/* ── Hero ── */}
        <Animated.View style={[{ paddingHorizontal: 24, paddingTop: 28, paddingBottom: 8 }, animStyle(heroAnim)]}>
          <Text style={{ color: colors.textSecondary, fontSize: 14, marginBottom: 8 }}>
            {t("mobileHome.greeting", { name: displayName })}
          </Text>
          <Text style={{ color: colors.textPrimary, fontSize: 26, fontWeight: "800", lineHeight: 34 }}>
            {t("mobileHome.heroLine1")}{"\n"}
            <Text style={{ color: colors.primary }}>{t("mobileHome.heroEvents")}</Text>
            <Text style={{ color: colors.textPrimary }}>{t("mobileHome.heroLine2")}</Text>
          </Text>
        </Animated.View>

        {/* ── Accesos rápidos ── */}
        <Animated.View style={[{ paddingHorizontal: 24, marginTop: 24 }, animStyle(bentoAnim)]}>
          <Text style={styles.blockTitle}>{t("mobileHome.quickAccess")}</Text>
          <View style={styles.quickGrid}>
            {quickActions.map((action) => (
              <TouchableOpacity
                key={action.key}
                activeOpacity={0.75}
                onPress={action.onPress}
                style={[styles.quickCard, {
                  backgroundColor: action.tint + "22",
                  borderColor: action.tint + "50",
                }]}
              >
                <View style={[styles.quickIcon, { backgroundColor: action.tint + "40" }]}>
                  <Ionicons name={action.icon} size={18} color={colors.textPrimary} />
                  {action.badge > 0 && (
                    <View style={[styles.quickBadge, { backgroundColor: colors.error, borderColor: colors.background }]}>
                      <Text style={styles.quickBadgeText}>{action.badge > 99 ? "99+" : action.badge}</Text>
                    </View>
                  )}
                </View>
                <View style={styles.quickFooter}>
                  <Text style={styles.quickLabel}>{t(`mobileHome.${action.key}`)}</Text>
                  <View style={[styles.quickArrow, { backgroundColor: action.tint }]}>
                    <Ionicons name="arrow-forward" size={13} color="#FFFFFF" />
                  </View>
                </View>
              </TouchableOpacity>
            ))}
          </View>
        </Animated.View>

        {/* ── Estadísticas ── */}
        <Animated.View style={[styles.statsRow, animStyle(bentoAnim)]}>
          {[
            { key: "available", value: stats.total, icon: "sparkles-outline" },
            { key: "thisWeek", value: stats.thisWeek, icon: "flame-outline" },
            { key: "categories", value: stats.categories, icon: "pricetags-outline" },
          ].map((stat) => (
            <View key={stat.key} style={styles.statCard}>
              <Ionicons name={stat.icon} size={16} color={colors.accent} />
              <Text style={styles.statValue}>{eventsLoading ? "—" : stat.value}</Text>
              <Text style={styles.statLabel} numberOfLines={2}>{t(`mobileHome.stats.${stat.key}`)}</Text>
            </View>
          ))}
        </Animated.View>

        {/* ── Próximos eventos ── */}
        <Animated.View style={[{ marginTop: 28 }, animStyle(eventsAnim)]}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>{t("mobileHome.upcomingEvents")}</Text>
            <TouchableOpacity onPress={() => navigation.navigate("Eventos")} activeOpacity={0.75}>
              <Text style={styles.seeAll}>{t("mobileHome.seeAll")}</Text>
            </TouchableOpacity>
          </View>

          {eventsLoading ? (
            <ActivityIndicator color={colors.primary} style={{ marginVertical: 32 }} />
          ) : upcomingEvents.length === 0 ? (
            <View style={styles.emptyBox}>
              <Ionicons name="calendar-outline" size={28} color={colors.textMuted} />
              <Text style={styles.emptyText}>{t("mobileHome.noUpcoming")}</Text>
            </View>
          ) : (
            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.eventsCarousel}
              decelerationRate="fast"
              snapToInterval={CARD_WIDTH + 14}
            >
              {upcomingEvents.map((event) => (
                <EventCard
                  key={event.eventId}
                  event={event}
                  width={CARD_WIDTH}
                  category={categoryName(event.categoryId)}
                  onPress={() => navigation.navigate("EventDetail", { eventId: event.eventId })}
                />
              ))}
            </ScrollView>
          )}
        </Animated.View>

        {/* ── Explora por categoría ── */}
        {topCategories.length > 0 && (
          <Animated.View style={[{ marginTop: 28 }, animStyle(eventsAnim)]}>
            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>{t("mobileHome.exploreCategories")}</Text>
            </View>
            <View style={styles.categoryWrap}>
              {topCategories.map((cat) => (
                <TouchableOpacity
                  key={cat.categoryId}
                  activeOpacity={0.75}
                  onPress={() => navigation.navigate("Eventos", { categoryId: cat.categoryId })}
                  style={styles.categoryPill}
                >
                  <Text style={styles.categoryPillText} numberOfLines={1}>
                    {cat.translatedName || cat.name}
                  </Text>
                  <View style={styles.categoryCount}>
                    <Text style={styles.categoryCountText}>{cat.count}</Text>
                  </View>
                </TouchableOpacity>
              ))}
            </View>
          </Animated.View>
        )}

        {/* ── Cómo funciona ── */}
        <Animated.View style={[{ paddingHorizontal: 24, marginTop: 32 }, animStyle(eventsAnim)]}>
          <Text style={styles.blockTitle}>{t("mobileHome.steps.title")}</Text>
          <View style={styles.stepsCard}>
            {STEPS.map((step, index) => (
              <View key={step.key} style={styles.stepRow}>
                <View style={styles.stepIconCol}>
                  <View style={styles.stepIcon}>
                    <Ionicons name={step.icon} size={18} color={colors.textPrimary} />
                  </View>
                  {index < STEPS.length - 1 && <View style={styles.stepLine} />}
                </View>
                <View style={styles.stepTextCol}>
                  <Text style={styles.stepTitle}>{t(`mobileHome.steps.${step.key}.title`)}</Text>
                  <Text style={styles.stepDesc}>{t(`mobileHome.steps.${step.key}.desc`)}</Text>
                </View>
              </View>
            ))}
          </View>
        </Animated.View>
      </ScrollView>
    </View>
  );
}

// ─── Estilos ─────────────────────────────────────────────────────────────────
function createStyles(colors) {
  return StyleSheet.create({
    blockTitle: {
      color: colors.textPrimary,
      fontSize: 15,
      fontWeight: "700",
      marginBottom: 12,
    },
    quickGrid: {
      flexDirection: "row",
      flexWrap: "wrap",
      gap: 12,
    },
    quickCard: {
      width: (width - 48 - 12) / 2,
      height: 96,
      borderRadius: 14,
      borderWidth: 1,
      padding: 14,
      justifyContent: "space-between",
    },
    quickIcon: {
      width: 36,
      height: 36,
      borderRadius: 10,
      alignItems: "center",
      justifyContent: "center",
    },
    quickBadge: {
      position: "absolute",
      top: -6,
      right: -8,
      minWidth: 18,
      height: 18,
      borderRadius: 9,
      borderWidth: 1.5,
      paddingHorizontal: 4,
      alignItems: "center",
      justifyContent: "center",
    },
    quickBadgeText: {
      color: "#FFFFFF",
      fontSize: 10,
      fontWeight: "800",
    },
    quickFooter: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
    },
    quickLabel: {
      color: colors.textPrimary,
      fontSize: 13,
      fontWeight: "700",
      flex: 1,
    },
    quickArrow: {
      width: 26,
      height: 26,
      borderRadius: 13,
      alignItems: "center",
      justifyContent: "center",
    },
    statsRow: {
      flexDirection: "row",
      gap: 10,
      paddingHorizontal: 24,
      marginTop: 16,
    },
    statCard: {
      flex: 1,
      backgroundColor: colors.surface + "CC",
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.primary + "25",
      paddingVertical: 12,
      paddingHorizontal: 10,
      gap: 4,
    },
    statValue: {
      color: colors.textPrimary,
      fontSize: 22,
      fontWeight: "800",
    },
    statLabel: {
      color: colors.textSecondary,
      fontSize: 11,
      lineHeight: 14,
    },
    sectionHeader: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      paddingHorizontal: 24,
      marginBottom: 14,
    },
    sectionTitle: {
      color: colors.textPrimary,
      fontSize: 15,
      fontWeight: "700",
    },
    seeAll: {
      color: colors.accent,
      fontSize: 13,
      fontWeight: "700",
    },
    eventsCarousel: {
      paddingHorizontal: 24,
      gap: 14,
    },
    emptyBox: {
      marginHorizontal: 24,
      paddingVertical: 24,
      alignItems: "center",
      gap: 8,
      backgroundColor: colors.surface + "80",
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.primary + "20",
    },
    emptyText: {
      color: colors.textSecondary,
      fontSize: 13,
    },
    categoryWrap: {
      flexDirection: "row",
      flexWrap: "wrap",
      gap: 8,
      paddingHorizontal: 24,
    },
    categoryPill: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
      backgroundColor: colors.surface + "CC",
      borderRadius: 20,
      borderWidth: 1,
      borderColor: colors.primary + "35",
      paddingLeft: 14,
      paddingRight: 6,
      paddingVertical: 6,
      maxWidth: width - 48,
    },
    categoryPillText: {
      color: colors.textPrimary,
      fontSize: 13,
      fontWeight: "600",
      flexShrink: 1,
    },
    categoryCount: {
      minWidth: 22,
      height: 22,
      borderRadius: 11,
      paddingHorizontal: 6,
      backgroundColor: colors.primary,
      alignItems: "center",
      justifyContent: "center",
    },
    categoryCountText: {
      color: "#FFFFFF",
      fontSize: 11,
      fontWeight: "800",
    },
    stepsCard: {
      backgroundColor: colors.surface + "99",
      borderRadius: 16,
      borderWidth: 1,
      borderColor: colors.primary + "25",
      padding: 16,
    },
    stepRow: {
      flexDirection: "row",
      gap: 14,
    },
    stepIconCol: {
      alignItems: "center",
    },
    stepIcon: {
      width: 36,
      height: 36,
      borderRadius: 18,
      backgroundColor: colors.primary,
      alignItems: "center",
      justifyContent: "center",
    },
    stepLine: {
      width: 2,
      flex: 1,
      minHeight: 14,
      backgroundColor: colors.primary + "40",
      marginVertical: 4,
    },
    stepTextCol: {
      flex: 1,
      paddingBottom: 16,
    },
    stepTitle: {
      color: colors.textPrimary,
      fontSize: 14,
      fontWeight: "700",
      marginBottom: 2,
    },
    stepDesc: {
      color: colors.textSecondary,
      fontSize: 12,
      lineHeight: 17,
    },
  });
}