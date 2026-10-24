import { useState, useEffect, useMemo, useRef } from "react";
import {
  View,
  Text,
  TextInput,
  FlatList,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  StyleSheet,
  RefreshControl,
  StatusBar,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation, useRoute } from "@react-navigation/native";
import { useTranslation } from "react-i18next";
import { useTheme } from "../context/ThemeContext";
import { useLanguage } from "../context/LanguageContext";
import { getPublishedEvents } from "../services/eventService";
import { getAllCategories } from "../services/catalogService";
import EventCard from "../components/EventCard";
import { displayEventName, parseLocalDate, resolveCoverUrl } from "../utils/eventFormat";

// Filtros rápidos de fecha (clave i18n en mobileEvents.dates.*)
const DATE_FILTERS = ["all", "today", "weekend", "week", "month"];

/** Rango [desde, hasta) en fecha local para cada filtro rápido. */
function dateRange(filter) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const addDays = (d, n) => { const r = new Date(d); r.setDate(r.getDate() + n); return r; };
  switch (filter) {
    case "today":
      return [today, addDays(today, 1)];
    case "weekend": {
      // Sábado y domingo de esta semana (si hoy es domingo, solo hoy)
      const day = today.getDay();
      const saturday = day === 0 ? addDays(today, -1) : addDays(today, 6 - day);
      return [day === 0 ? today : saturday, addDays(saturday, 2)];
    }
    case "week":
      return [today, addDays(today, 7)];
    case "month":
      return [today, new Date(today.getFullYear(), today.getMonth() + 1, 1)];
    default:
      return null;
  }
}

/** Un evento cae en el rango si su intervalo [inicio, fin] se cruza con él. */
function inRange(event, range) {
  if (!range) return true;
  const start = parseLocalDate(event.startDate);
  if (!start) return false;
  const finish = parseLocalDate(event.finishDate) ?? start;
  return start < range[1] && finish >= range[0];
}

/**
 * Pantalla principal del tab "Eventos".
 * Lista los eventos publicados con búsqueda y filtros (como /events en la web).
 * El filtrado es local para conservar los nombres traducidos del idioma activo.
 * (La validación de QR del organizador vive en "Validar entradas", en el perfil.)
 */
export default function EventsHomeScreen() {
  const { colors } = useTheme();
  const { t } = useTranslation();
  const { language } = useLanguage();
  const navigation = useNavigation();
  const route = useRoute();
  const styles = createStyles(colors);
  const searchRef = useRef(null);

  const [events, setEvents] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const [query, setQuery] = useState("");
  const [dateFilter, setDateFilter] = useState("all");
  const [categoryId, setCategoryId] = useState(null);
  const [onlyAvailable, setOnlyAvailable] = useState(false);

  const loadEvents = async (isRefresh = false) => {
    try {
      if (!isRefresh) setLoading(true);
      setError(null);
      const [data, cats] = await Promise.all([
        getPublishedEvents(language),
        getAllCategories(language).catch(() => []),
      ]);
      setEvents((data || []).map((event) => ({ ...event, coverUrl: resolveCoverUrl(event.coverUrl) })));
      setCategories(Array.isArray(cats) ? cats : []);
    } catch (err) {
      console.error("[EventsHomeScreen] Error loading events:", err);
      setError(err.message || t("events.connectionError"));
      setEvents([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    void loadEvents();
    // Recargar cuando cambia el idioma para traer nombres traducidos.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [language]);

  // Acceso rápido "Buscar eventos" desde Inicio: enfoca el buscador
  useEffect(() => {
    if (route.params?.focusSearch) {
      const id = setTimeout(() => searchRef.current?.focus(), 300);
      navigation.setParams({ focusSearch: undefined });
      return () => clearTimeout(id);
    }
  }, [route.params?.focusSearch]);

  // "Explora por categoría" desde Inicio: aplica el filtro de categoría
  useEffect(() => {
    if (route.params?.categoryId) {
      setCategoryId(route.params.categoryId);
      navigation.setParams({ categoryId: undefined });
    }
  }, [route.params?.categoryId]);

  const handleRefresh = () => {
    setRefreshing(true);
    void loadEvents(true);
  };

  const categoryName = (id) => {
    const cat = categories.find((c) => c.categoryId === id);
    return cat ? cat.translatedName || cat.name : null;
  };

  // Solo se ofrecen categorías que tienen al menos un evento publicado
  const categoryChips = useMemo(() => {
    const used = new Set(events.map((e) => e.categoryId).filter(Boolean));
    return categories.filter((c) => used.has(c.categoryId));
  }, [events, categories]);

  const filteredEvents = useMemo(() => {
    const term = query.trim().toLowerCase();
    const range = dateRange(dateFilter);
    return events.filter((event) => {
      if (term) {
        const haystack = [displayEventName(event, language), event.eventName, event.siteName]
          .filter(Boolean).join(" ").toLowerCase();
        if (!haystack.includes(term)) return false;
      }
      if (categoryId && event.categoryId !== categoryId) return false;
      if (onlyAvailable && !(event.availableSeats > 0)) return false;
      return inRange(event, range);
    });
  }, [events, query, dateFilter, categoryId, onlyAvailable, language]);

  const hasFilters = query.trim() !== "" || dateFilter !== "all" || categoryId !== null || onlyAvailable;

  const clearFilters = () => {
    setQuery("");
    setDateFilter("all");
    setCategoryId(null);
    setOnlyAvailable(false);
  };

  const Chip = ({ label, active, onPress, icon }) => (
    <TouchableOpacity
      onPress={onPress}
      activeOpacity={0.75}
      style={[styles.chip, active && styles.chipActive]}
    >
      {icon ? <Ionicons name={icon} size={13} color={active ? "#FFFFFF" : colors.textSecondary} /> : null}
      <Text style={[styles.chipText, active && styles.chipTextActive]}>{label}</Text>
    </TouchableOpacity>
  );

  const renderFilters = () => (
    <View style={styles.filters}>
      {/* Buscador */}
      <View style={styles.searchBox}>
        <Ionicons name="search" size={18} color={colors.textMuted} />
        <TextInput
          ref={searchRef}
          value={query}
          onChangeText={setQuery}
          placeholder={t("events.searchPlaceholder")}
          placeholderTextColor={colors.textMuted}
          style={styles.searchInput}
          returnKeyType="search"
          autoCorrect={false}
        />
        {query ? (
          <TouchableOpacity onPress={() => setQuery("")} hitSlop={10}>
            <Ionicons name="close-circle" size={18} color={colors.textMuted} />
          </TouchableOpacity>
        ) : null}
      </View>

      {/* Fecha */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipRow}>
        {DATE_FILTERS.map((key) => (
          <Chip
            key={key}
            label={t(`mobileEvents.dates.${key}`)}
            icon={key === "all" ? "calendar-outline" : undefined}
            active={dateFilter === key}
            onPress={() => setDateFilter(key)}
          />
        ))}
        <Chip
          label={t("mobileEvents.onlyAvailable")}
          icon="ticket-outline"
          active={onlyAvailable}
          onPress={() => setOnlyAvailable((v) => !v)}
        />
      </ScrollView>

      {/* Categorías */}
      {categoryChips.length > 0 && (
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipRow}>
          <Chip
            label={t("mobileEvents.allCategories")}
            icon="pricetags-outline"
            active={categoryId === null}
            onPress={() => setCategoryId(null)}
          />
          {categoryChips.map((cat) => (
            <Chip
              key={cat.categoryId}
              label={cat.translatedName || cat.name}
              active={categoryId === cat.categoryId}
              onPress={() => setCategoryId(categoryId === cat.categoryId ? null : cat.categoryId)}
            />
          ))}
        </ScrollView>
      )}

      {/* Resumen */}
      <View style={styles.resultsRow}>
        <Text style={styles.resultsText}>
          {t("mobileEvents.results", { count: filteredEvents.length })}
        </Text>
        {hasFilters && (
          <TouchableOpacity onPress={clearFilters} activeOpacity={0.75} style={styles.clearButton}>
            <Ionicons name="close" size={14} color={colors.accent} />
            <Text style={styles.clearText}>{t("mobileEvents.clearFilters")}</Text>
          </TouchableOpacity>
        )}
      </View>
    </View>
  );

  const renderEmptyState = () => (
    <View style={styles.emptyContainer}>
      <View style={styles.emptyIconContainer}>
        <Ionicons name={hasFilters ? "search-outline" : "calendar-outline"} size={56} color={colors.textMuted} />
      </View>
      <Text style={styles.emptyTitle}>{hasFilters ? t("events.noEventsFound") : t("events.noEvents")}</Text>
      <Text style={styles.emptyMessage}>
        {hasFilters ? t("mobileEvents.noMatches") : t("events.noEventsMessage")}
      </Text>
      <TouchableOpacity
        style={styles.refreshButton}
        onPress={hasFilters ? clearFilters : handleRefresh}
        activeOpacity={0.75}
      >
        <Ionicons name={hasFilters ? "close" : "refresh"} size={18} color="#FFFFFF" />
        <Text style={styles.refreshButtonText}>
          {hasFilters ? t("mobileEvents.clearFilters") : t("common.tryAgain")}
        </Text>
      </TouchableOpacity>
    </View>
  );

  const renderErrorState = () => (
    <View style={styles.emptyContainer}>
      <View style={styles.emptyIconContainer}>
        <Ionicons name="alert-circle-outline" size={56} color={colors.error} />
      </View>
      <Text style={styles.emptyTitle}>{t("events.connectionError")}</Text>
      <Text style={styles.emptyMessage}>{error}</Text>
      <TouchableOpacity style={styles.refreshButton} onPress={handleRefresh} activeOpacity={0.75}>
        <Ionicons name="refresh" size={18} color="#FFFFFF" />
        <Text style={styles.refreshButtonText}>{t("common.tryAgain")}</Text>
      </TouchableOpacity>
    </View>
  );

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <StatusBar barStyle="light-content" backgroundColor={colors.background} />

      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>{t("events.title")}</Text>
        <TouchableOpacity style={styles.headerButton} onPress={handleRefresh} activeOpacity={0.75}>
          <Ionicons name="refresh" size={22} color={colors.accent} />
        </TouchableOpacity>
      </View>

      {loading && !refreshing ? (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={colors.primary} />
          <Text style={styles.loadingText}>{t("events.loadingEvents")}</Text>
        </View>
      ) : (
        <FlatList
          data={filteredEvents}
          keyExtractor={(item) => item.eventId.toString()}
          renderItem={({ item }) => (
            <EventCard
              event={item}
              category={categoryName(item.categoryId)}
              onPress={() => navigation.navigate("EventDetail", { eventId: item.eventId })}
            />
          )}
          ItemSeparatorComponent={() => <View style={{ height: 16 }} />}
          ListHeaderComponent={error ? null : renderFilters()}
          ListEmptyComponent={error ? renderErrorState : renderEmptyState}
          contentContainerStyle={styles.listContent}
          keyboardShouldPersistTaps="handled"
          keyboardDismissMode="on-drag"
          showsVerticalScrollIndicator={false}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={handleRefresh}
              tintColor={colors.primary}
              colors={[colors.primary]}
            />
          }
        />
      )}
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
      paddingBottom: 32,
      flexGrow: 1,
    },
    filters: {
      gap: 12,
      marginBottom: 16,
    },
    searchBox: {
      flexDirection: "row",
      alignItems: "center",
      gap: 10,
      backgroundColor: colors.surface,
      borderRadius: 14,
      borderWidth: 1,
      borderColor: colors.primary + "35",
      paddingHorizontal: 14,
      height: 48,
    },
    searchInput: {
      flex: 1,
      color: colors.textPrimary,
      fontSize: 15,
      paddingVertical: 0,
    },
    chipRow: {
      gap: 8,
      paddingRight: 8,
    },
    chip: {
      flexDirection: "row",
      alignItems: "center",
      gap: 5,
      backgroundColor: colors.surface,
      borderRadius: 18,
      borderWidth: 1,
      borderColor: colors.primary + "30",
      paddingHorizontal: 12,
      paddingVertical: 7,
    },
    chipActive: {
      backgroundColor: colors.primary,
      borderColor: colors.primary,
    },
    chipText: {
      color: colors.textSecondary,
      fontSize: 13,
      fontWeight: "600",
    },
    chipTextActive: {
      color: "#FFFFFF",
    },
    resultsRow: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
    },
    resultsText: {
      color: colors.textMuted,
      fontSize: 12,
      fontWeight: "600",
    },
    clearButton: {
      flexDirection: "row",
      alignItems: "center",
      gap: 4,
    },
    clearText: {
      color: colors.accent,
      fontSize: 12,
      fontWeight: "700",
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
      paddingVertical: 40,
    },
    emptyIconContainer: {
      width: 104,
      height: 104,
      borderRadius: 52,
      backgroundColor: colors.surfaceAlt,
      alignItems: "center",
      justifyContent: "center",
      marginBottom: 20,
    },
    emptyTitle: {
      fontSize: 19,
      fontWeight: "800",
      color: colors.textPrimary,
      marginBottom: 10,
      textAlign: "center",
    },
    emptyMessage: {
      fontSize: 14,
      color: colors.textSecondary,
      textAlign: "center",
      lineHeight: 22,
      marginBottom: 22,
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
      color: "#FFFFFF",
    },
  });
}
