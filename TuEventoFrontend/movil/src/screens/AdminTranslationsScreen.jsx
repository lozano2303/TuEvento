import { useState, useEffect, useCallback } from "react";
import {
  View, Text, TextInput, FlatList, ScrollView, TouchableOpacity, ActivityIndicator,
  StatusBar, RefreshControl, Modal, KeyboardAvoidingView, Platform,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import { getTranslations, saveTranslation, LANGUAGE_IDS } from "../services/adminTranslationService";
import { getAdminEvents } from "../services/adminService";

// El español es el idioma fuente: no se traduce (igual que la web)
const LANGUAGES = [
  { code: "en", name: "English",   flag: "🇬🇧" },
  { code: "fr", name: "Français",  flag: "🇫🇷" },
  { code: "pt", name: "Português", flag: "🇵🇹" },
];

const FILTERS = [
  { key: "not-translated", label: "Sin traducir",       color: "#f87171" },
  { key: "needs-review",   label: "Necesitan revisión", color: "#fbbf24" },
  { key: "translated",     label: "Traducidos",         color: "#34d399" },
  { key: "all",            label: "Todos",              color: null },
];

const STATUS_BADGE = {
  PENDING_REVIEW: { label: "Necesita revisión", color: "#fbbf24" },
  PUBLISHED:      { label: "Publicado",         color: "#34d399" },
  DRAFT:          { label: "Borrador",          color: "#9ca3af" },
};

/**
 * Configuración por módulo (espejo de las páginas /admin-translations/:lang/* de la web):
 * qué campo identifica la entidad, qué se muestra del original y qué campos se traducen.
 */
const MODULES = {
  events: {
    label: "Eventos", icon: "calendar-outline", idKey: "eventId",
    title: (e) => e.eventName,
    subtitle: (e) => e.categoryName || "Sin categoría",
    fields: [
      { key: "translatedName",        source: "eventName",   label: "Nombre traducido" },
      { key: "translatedDescription", source: "description", label: "Descripción traducida", multiline: true },
    ],
    extra: { source: "MANUAL", status: "PUBLISHED" },
    info: "Los eventos sin traducción mostrarán su contenido original en español a los usuarios.",
  },
  categories: {
    label: "Categorías", icon: "pricetags-outline", idKey: "categoryId",
    title: (c) => c.categoryName,
    subtitle: (c) => (c.parentId ? "Subcategoría" : null),
    fields: [{ key: "translatedName", source: "categoryName", label: "Nombre traducido" }],
    extra: { source: "MANUAL", status: "PUBLISHED" },
    info: "Las categorías sin traducción mostrarán su nombre original en español a los usuarios.",
  },
  sectionTypes: {
    label: "Tipos de sección", icon: "layers-outline", idKey: "sectionTypeId",
    title: (s) => s.sectionTypeName,
    subtitle: () => null,
    fields: [{ key: "translatedName", source: "sectionTypeName", label: "Nombre traducido" }],
    extra: { source: "MANUAL", status: "PUBLISHED" },
    info: "Los tipos de sección sin traducción mostrarán su nombre original en español a los usuarios.",
  },
  comments: {
    label: "Comentarios", icon: "chatbubbles-outline", idKey: "ratingId",
    title: (c) => c.comment,
    subtitle: (c) => (c.rating ? `Rating: ${c.rating}/5` : null),
    fields: [{ key: "translatedComment", source: "comment", label: "Comentario traducido", multiline: true }],
    extra: { status: "PUBLISHED" },
    needsEvent: true,
    info: "Los comentarios sin traducción se muestran en su idioma original.",
  },
  profiles: {
    label: "Perfiles", icon: "person-outline", idKey: "profileId",
    title: (p) => p.fullName,
    subtitle: (p) => p.bio,
    fields: [{ key: "translatedBio", source: "bio", label: "Biografía traducida", multiline: true }],
    extra: { status: "PUBLISHED" },
    info: "Las biografías sin traducción se muestran en su idioma original.",
  },
};
const MODULE_KEYS = Object.keys(MODULES);

const POLL_MS = 3000;
const MAX_POLLS = 40; // 2 minutos, igual que la web

/** Traducciones (admin): mismos módulos, idiomas, filtros y edición manual que la web. */
export default function AdminTranslationsScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();

  const [module, setModule]     = useState("events");
  const [lang, setLang]         = useState("en");
  const [filter, setFilter]     = useState("not-translated");
  const [items, setItems]       = useState([]);
  const [loading, setLoading]   = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError]       = useState(null);

  // Comentarios: se filtran por evento
  const [events, setEvents]           = useState([]);
  const [eventId, setEventId]         = useState(null);
  const [eventPickerOpen, setEventPickerOpen] = useState(false);

  // Edición
  const [editing, setEditing] = useState(null); // entidad en edición
  const [form, setForm]       = useState({});
  const [saving, setSaving]   = useState(false);

  const cfg = MODULES[module];

  const load = useCallback(async (silent = false) => {
    if (cfg.needsEvent && !eventId) {
      setItems([]);
      setLoading(false);
      setRefreshing(false);
      return;
    }
    if (!silent) setLoading(true);
    setError(null);
    try {
      setItems(await getTranslations(module, lang, filter, eventId));
    } catch (e) {
      setError(e.message);
      if (!silent) setItems([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [module, lang, filter, eventId, cfg.needsEvent]);

  useEffect(() => { void load(); }, [load]);

  // Eventos para el selector de comentarios (como la web, se elige el primero por defecto)
  useEffect(() => {
    if (!cfg.needsEvent || events.length) return;
    void (async () => {
      try {
        const list = await getAdminEvents();
        const arr = Array.isArray(list) ? list : (list?.data ?? []);
        setEvents(arr);
        if (arr.length && !eventId) setEventId(arr[0].eventId);
      } catch {
        setError("Error al cargar eventos");
      }
    })();
  }, [cfg.needsEvent, events.length, eventId]);

  // Mientras el backend genera traducciones automáticas se refresca la lista (como la web)
  useEffect(() => {
    if (!items.some((it) => it.translationGenerating)) return;
    let polls = 0;
    const id = setInterval(() => {
      polls += 1;
      if (polls > MAX_POLLS) { clearInterval(id); return; }
      void load(true);
    }, POLL_MS);
    return () => clearInterval(id);
  }, [items, load]);

  const startEdit = (item) => {
    const initial = {};
    cfg.fields.forEach((f) => { initial[f.key] = item[f.key] || item[f.source] || ""; });
    setForm(initial);
    setEditing(item);
  };

  const submitEdit = async () => {
    if (!editing) return;
    setSaving(true);
    try {
      const data = { ...cfg.extra };
      cfg.fields.forEach((f) => { data[f.key] = (form[f.key] || "").trim(); });
      await saveTranslation(module, editing[cfg.idKey], LANGUAGE_IDS[lang], data);
      setEditing(null);
      await load(true);
    } catch (e) {
      setError("Error al guardar traducción: " + e.message);
    } finally {
      setSaving(false);
    }
  };

  const currentLanguage = LANGUAGES.find((l) => l.code === lang);
  const selectedEvent = events.find((e) => e.eventId === eventId);

  const Chip = ({ active, onPress, label, icon, color }) => (
    <TouchableOpacity
      onPress={onPress}
      activeOpacity={0.75}
      style={{
        flexDirection: "row", alignItems: "center", gap: 6,
        paddingHorizontal: 13, paddingVertical: 7, borderRadius: 20,
        backgroundColor: active ? (color ? color + "33" : colors.primary) : colors.surface,
        borderWidth: 1, borderColor: active ? (color ?? colors.primary) : colors.primary + "30",
      }}
    >
      {icon ? <Ionicons name={icon} size={14} color={active ? (color ?? "#fff") : colors.textSecondary} /> : null}
      <Text style={{ color: active ? (color ?? "#FFFFFF") : colors.textSecondary, fontSize: 12, fontWeight: "700" }}>
        {label}
      </Text>
    </TouchableOpacity>
  );

  const renderHeader = () => (
    <View style={{ gap: 12, marginBottom: 8 }}>
      {/* Módulos */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8 }}>
        {MODULE_KEYS.map((key) => (
          <Chip
            key={key}
            active={module === key}
            onPress={() => { setModule(key); setEditing(null); }}
            label={MODULES[key].label}
            icon={MODULES[key].icon}
          />
        ))}
      </ScrollView>

      {/* Idioma */}
      <View style={{ flexDirection: "row", backgroundColor: colors.surface, borderRadius: 12, padding: 4, gap: 4 }}>
        {LANGUAGES.map((l) => {
          const active = lang === l.code;
          return (
            <TouchableOpacity
              key={l.code}
              onPress={() => setLang(l.code)}
              activeOpacity={0.8}
              style={{
                flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 6,
                paddingVertical: 9, borderRadius: 9,
                backgroundColor: active ? colors.primary : "transparent",
              }}
            >
              <Text style={{ fontSize: 15 }}>{l.flag}</Text>
              <Text style={{ color: active ? "#FFFFFF" : colors.textMuted, fontSize: 12, fontWeight: "700" }}>{l.name}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

      <View style={{
        flexDirection: "row", alignItems: "center", gap: 10, padding: 12, borderRadius: 12,
        backgroundColor: colors.primary + "18", borderWidth: 1, borderColor: colors.primary + "35",
      }}>
        <Text style={{ fontSize: 18 }}>{currentLanguage.flag}</Text>
        <Text style={{ color: colors.textPrimary, fontSize: 13, fontWeight: "600", flex: 1 }}>
          Editando traducciones de {cfg.label.toLowerCase()} en{" "}
          <Text style={{ color: colors.accent }}>{currentLanguage.name}</Text>
        </Text>
      </View>

      {/* Evento (solo comentarios) */}
      {cfg.needsEvent && (
        <TouchableOpacity
          onPress={() => setEventPickerOpen(true)}
          activeOpacity={0.8}
          style={{
            flexDirection: "row", alignItems: "center", gap: 10, padding: 12, borderRadius: 12,
            backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.primary + "30",
          }}
        >
          <Ionicons name="calendar-outline" size={18} color={colors.accent} />
          <View style={{ flex: 1 }}>
            <Text style={{ color: colors.textMuted, fontSize: 10, fontWeight: "700", textTransform: "uppercase" }}>Evento</Text>
            <Text style={{ color: colors.textPrimary, fontSize: 14, fontWeight: "600" }} numberOfLines={1}>
              {selectedEvent?.eventName || "Selecciona un evento"}
            </Text>
          </View>
          <Ionicons name="chevron-down" size={18} color={colors.textMuted} />
        </TouchableOpacity>
      )}

      {/* Filtros */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 8 }}>
        {FILTERS.map((f) => (
          <Chip key={f.key} active={filter === f.key} onPress={() => setFilter(f.key)} label={f.label} color={f.color} />
        ))}
      </ScrollView>

      {error ? (
        <View style={{
          flexDirection: "row", alignItems: "center", gap: 8, padding: 12, borderRadius: 12,
          backgroundColor: "#f8717118", borderWidth: 1, borderColor: "#f8717140",
        }}>
          <Ionicons name="alert-circle-outline" size={16} color="#f87171" />
          <Text style={{ color: "#f87171", fontSize: 12, flex: 1 }}>{error}</Text>
          <TouchableOpacity onPress={() => setError(null)} hitSlop={10}>
            <Ionicons name="close" size={16} color="#f87171" />
          </TouchableOpacity>
        </View>
      ) : null}
    </View>
  );

  const renderItem = ({ item }) => {
    const hasTranslation = cfg.fields.some((f) => item[f.key]);
    const badge = item.translationStatus ? STATUS_BADGE[item.translationStatus] : null;
    const subtitle = cfg.subtitle(item);
    return (
      <View style={{
        backgroundColor: colors.surface, borderRadius: 16, padding: 14, marginBottom: 10, gap: 10,
        borderWidth: 1, borderColor: colors.primary + "25",
      }}>
        <View style={{ flexDirection: "row", alignItems: "flex-start", gap: 10 }}>
          <View style={{ flex: 1 }}>
            <Text style={{ color: colors.textMuted, fontSize: 11, fontFamily: Platform.OS === "ios" ? "Menlo" : "monospace" }}>
              #{item[cfg.idKey]} · Original (ES)
            </Text>
            <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700", marginTop: 2 }} numberOfLines={3}>
              {cfg.title(item) || "—"}
            </Text>
            {subtitle ? (
              <Text style={{ color: colors.textMuted, fontSize: 12, marginTop: 2 }} numberOfLines={2}>{subtitle}</Text>
            ) : null}
          </View>
          {badge ? (
            <View style={{
              paddingHorizontal: 8, paddingVertical: 3, borderRadius: 8,
              backgroundColor: badge.color + "22", borderWidth: 1, borderColor: badge.color + "55",
            }}>
              <Text style={{ color: badge.color, fontSize: 10, fontWeight: "800" }}>{badge.label.toUpperCase()}</Text>
            </View>
          ) : null}
        </View>

        <View style={{ borderTopWidth: 1, borderTopColor: colors.primary + "18", paddingTop: 10, gap: 4 }}>
          <Text style={{ color: colors.textMuted, fontSize: 10, fontWeight: "700", textTransform: "uppercase" }}>
            Traducción ({lang.toUpperCase()})
          </Text>
          {item.translationGenerating && !hasTranslation ? (
            <View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
              <ActivityIndicator size="small" color="#fbbf24" />
              <Text style={{ color: "#fbbf24", fontSize: 12 }}>Generando traducción...</Text>
            </View>
          ) : hasTranslation ? (
            cfg.fields.map((f) => (item[f.key] ? (
              <Text key={f.key} style={{ color: colors.textSecondary, fontSize: 13 }} numberOfLines={3}>{item[f.key]}</Text>
            ) : null))
          ) : (
            <Text style={{ color: colors.textMuted, fontSize: 12, fontStyle: "italic" }}>Sin traducir</Text>
          )}
        </View>

        <TouchableOpacity
          onPress={() => startEdit(item)}
          disabled={!!item.translationGenerating}
          activeOpacity={0.8}
          style={{
            flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 6,
            paddingVertical: 10, borderRadius: 11,
            backgroundColor: colors.primary + "22", borderWidth: 1, borderColor: colors.primary + "45",
            opacity: item.translationGenerating ? 0.5 : 1,
          }}
        >
          <Ionicons name="create-outline" size={16} color={colors.accent} />
          <Text style={{ color: colors.accent, fontSize: 13, fontWeight: "700" }}>Editar traducción</Text>
        </TouchableOpacity>
      </View>
    );
  };

  const emptyText = cfg.needsEvent && !eventId
    ? "Selecciona un evento para ver sus comentarios"
    : `No hay ${cfg.label.toLowerCase()} con este filtro`;

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <StatusBar barStyle="light-content" backgroundColor={colors.surface} />

      {/* Header */}
      <View style={{
        flexDirection: "row", alignItems: "center", gap: 12,
        paddingTop: insets.top + 16, paddingBottom: 16, paddingHorizontal: 20,
        backgroundColor: colors.surface,
        borderBottomWidth: 1, borderBottomColor: colors.primary + "30",
      }}>
        <TouchableOpacity
          onPress={() => navigation.goBack()} activeOpacity={0.75}
          style={{
            width: 38, height: 38, borderRadius: 12,
            backgroundColor: colors.primary + "28",
            borderWidth: 1, borderColor: colors.primary + "40",
            alignItems: "center", justifyContent: "center",
          }}
        >
          <Ionicons name="arrow-back" size={20} color={colors.accent} />
        </TouchableOpacity>
        <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "800", flex: 1 }}>Traducciones</Text>
      </View>

      <FlatList
        data={loading ? [] : items}
        keyExtractor={(it) => String(it[cfg.idKey])}
        renderItem={renderItem}
        ListHeaderComponent={renderHeader()}
        contentContainerStyle={{ padding: 16, paddingBottom: insets.bottom + 24 }}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); void load(true); }} tintColor={colors.primary} />}
        ListEmptyComponent={
          loading ? (
            <ActivityIndicator size="large" color={colors.primary} style={{ marginTop: 40 }} />
          ) : (
            <View style={{ alignItems: "center", paddingTop: 40, gap: 10 }}>
              <Ionicons name="language-outline" size={44} color={colors.textMuted} />
              <Text style={{ color: colors.textSecondary, fontSize: 14, textAlign: "center" }}>{emptyText}</Text>
            </View>
          )
        }
        ListFooterComponent={
          <View style={{
            marginTop: 6, padding: 12, borderRadius: 12,
            backgroundColor: "#3b82f611", borderWidth: 1, borderColor: "#3b82f633",
          }}>
            <Text style={{ color: "#93c5fd", fontSize: 12 }}>
              <Text style={{ fontWeight: "800" }}>Info: </Text>{cfg.info}
            </Text>
          </View>
        }
      />

      {/* Modal: elegir evento (comentarios) */}
      <Modal visible={eventPickerOpen} transparent animationType="slide" onRequestClose={() => setEventPickerOpen(false)}>
        <View style={{ flex: 1, backgroundColor: "rgba(0,0,0,0.7)", justifyContent: "flex-end" }}>
          <View style={{
            backgroundColor: colors.background, borderTopLeftRadius: 24, borderTopRightRadius: 24,
            padding: 20, paddingBottom: insets.bottom + 20, maxHeight: "75%",
            borderWidth: 1, borderColor: colors.primary + "30",
          }}>
            <View style={{ flexDirection: "row", alignItems: "center", marginBottom: 12 }}>
              <Text style={{ color: colors.textPrimary, fontSize: 17, fontWeight: "800", flex: 1 }}>Selecciona un evento</Text>
              <TouchableOpacity onPress={() => setEventPickerOpen(false)} hitSlop={10}>
                <Ionicons name="close" size={22} color={colors.textMuted} />
              </TouchableOpacity>
            </View>
            <ScrollView>
              {events.length === 0 ? (
                <Text style={{ color: colors.textMuted, fontSize: 13 }}>No hay eventos</Text>
              ) : events.map((e) => (
                <TouchableOpacity
                  key={e.eventId}
                  onPress={() => { setEventId(e.eventId); setEventPickerOpen(false); }}
                  activeOpacity={0.75}
                  style={{
                    paddingVertical: 12, paddingHorizontal: 10, borderRadius: 10,
                    backgroundColor: e.eventId === eventId ? colors.primary + "22" : "transparent",
                  }}
                >
                  <Text style={{ color: colors.textPrimary, fontWeight: "600" }} numberOfLines={1}>{e.eventName}</Text>
                  <Text style={{ color: colors.textMuted, fontSize: 11 }}>#{e.eventId}</Text>
                </TouchableOpacity>
              ))}
            </ScrollView>
          </View>
        </View>
      </Modal>

      {/* Modal: editar traducción */}
      <Modal visible={!!editing} transparent animationType="slide" onRequestClose={() => !saving && setEditing(null)}>
        <KeyboardAvoidingView
          behavior={Platform.OS === "ios" ? "padding" : undefined}
          style={{ flex: 1, backgroundColor: "rgba(0,0,0,0.7)", justifyContent: "flex-end" }}
        >
          <View style={{
            backgroundColor: colors.background, borderTopLeftRadius: 24, borderTopRightRadius: 24,
            padding: 20, paddingBottom: insets.bottom + 20, gap: 10, maxHeight: "90%",
            borderWidth: 1, borderColor: colors.primary + "30",
          }}>
            <Text style={{ color: colors.textPrimary, fontSize: 17, fontWeight: "800" }}>
              Editar traducción ({lang.toUpperCase()})
            </Text>
            <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ gap: 10 }}>
              {editing && cfg.fields.map((f) => (
                <View key={f.key} style={{ gap: 6 }}>
                  <Text style={{ color: colors.textMuted, fontSize: 11, fontWeight: "700", textTransform: "uppercase" }}>
                    Original (ES)
                  </Text>
                  <Text style={{ color: colors.textSecondary, fontSize: 13 }}>{editing[f.source] || "—"}</Text>
                  <Text style={{ color: colors.textMuted, fontSize: 11, fontWeight: "700", textTransform: "uppercase", marginTop: 4 }}>
                    {f.label}
                  </Text>
                  <TextInput
                    value={form[f.key]}
                    onChangeText={(v) => setForm((prev) => ({ ...prev, [f.key]: v }))}
                    multiline={!!f.multiline}
                    placeholder={`${f.label}...`}
                    placeholderTextColor={colors.textMuted}
                    style={{
                      backgroundColor: colors.surface, borderRadius: 12, padding: 12,
                      minHeight: f.multiline ? 100 : undefined,
                      color: colors.textPrimary, fontSize: 14, textAlignVertical: f.multiline ? "top" : "center",
                      borderWidth: 1, borderColor: colors.primary + "30",
                    }}
                  />
                </View>
              ))}
            </ScrollView>
            <View style={{ flexDirection: "row", gap: 10, marginTop: 4 }}>
              <TouchableOpacity
                onPress={() => setEditing(null)}
                disabled={saving}
                activeOpacity={0.8}
                style={{
                  flex: 1, paddingVertical: 14, borderRadius: 13, alignItems: "center",
                  borderWidth: 1, borderColor: colors.primary + "30",
                }}
              >
                <Text style={{ color: colors.textSecondary, fontWeight: "700" }}>Cancelar</Text>
              </TouchableOpacity>
              <TouchableOpacity
                onPress={submitEdit}
                disabled={saving || cfg.fields.every((f) => !(form[f.key] || "").trim())}
                activeOpacity={0.8}
                style={{ flex: 1, paddingVertical: 14, borderRadius: 13, alignItems: "center", backgroundColor: colors.primary }}
              >
                {saving
                  ? <ActivityIndicator size="small" color="#fff" />
                  : <Text style={{ color: "#fff", fontWeight: "700" }}>Guardar</Text>}
              </TouchableOpacity>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>
    </View>
  );
}
