import { useState, useEffect, useCallback } from "react";
import {
  View, Text, ScrollView, TouchableOpacity, ActivityIndicator,
  StatusBar, StyleSheet, RefreshControl, TextInput, Modal, Image,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import { getAdminEvents, adminChangeEventStatus } from "../services/adminService";
import { getEventDetail, getEventMedia } from "../services/eventService";
import AppModal from "../components/AppModal";

// ─── Helpers ──────────────────────────────────────────────────────────────────

function fmtDate(d) {
  if (!d) return "—";
  const date = new Date(d.includes("T") ? d : d + "T00:00:00");
  return date.toLocaleDateString("es-CO", { day: "2-digit", month: "short", year: "numeric" });
}

// ─── Config de estados ─────────────────────────────────────────────────────────

const STATUS_CONFIG = {
  DRAFT:          { label: "Borrador",    color: "#64748b", bg: "#64748b22", icon: "document-outline" },
  PENDING_REVIEW: { label: "En revisión", color: "#f59e0b", bg: "#f59e0b22", icon: "time-outline" },
  PUBLISHED:      { label: "Activo",      color: "#34d399", bg: "#34d39922", icon: "globe-outline" },
  REJECTED:       { label: "Rechazado",   color: "#f87171", bg: "#f8717122", icon: "close-circle-outline" },
  CANCELLED:      { label: "Cancelado",   color: "#f87171", bg: "#f8717122", icon: "ban-outline" },
  COMPLETED:      { label: "Finalizado",  color: "#a78bfa", bg: "#a78bfa22", icon: "flag-outline" },
};

function getCfg(status) {
  return STATUS_CONFIG[status] ?? { label: status, color: "#94a3b8", bg: "#94a3b822", icon: "ellipse-outline" };
}

function getAllowedTransitions(status) {
  if (status === "PENDING_REVIEW") return [
    { value: "PUBLISHED", label: "Publicar",  icon: "checkmark-circle-outline",   color: "#34d399" },
    { value: "REJECTED",  label: "Rechazar",  icon: "chatbubble-ellipses-outline", color: "#f87171" },
  ];
  if (status === "PUBLISHED") return [
    { value: "CANCELLED", label: "Cancelar",  icon: "close-circle-outline", color: "#f87171" },
    { value: "COMPLETED", label: "Finalizar", icon: "flag-outline",          color: "#a78bfa" },
  ];
  return [];
}

const FILTERS = [
  { key: "all",            label: "Todos",        icon: "list-outline" },
  { key: "PENDING_REVIEW", label: "En revisión",  icon: "time-outline" },
  { key: "PUBLISHED",      label: "Activos",      icon: "globe-outline" },
  { key: "DRAFT",          label: "Borradores",   icon: "document-outline" },
  { key: "REJECTED",       label: "Rechazados",   icon: "close-circle-outline" },
  { key: "CANCELLED",      label: "Cancelados",   icon: "ban-outline" },
  { key: "COMPLETED",      label: "Finalizados",  icon: "flag-outline" },
];

// ─── Dropdown de filtros (se superpone, no desplaza) ─────────────────────────

function FilterDropdown({ filter, onSelect, onClose, colors, insets }) {
  const currentLabel = FILTERS.find((f) => f.key === filter)?.label ?? "Filtrar";
  return (
    <Modal visible transparent animationType="fade" onRequestClose={onClose}>
      {/* Overlay que cierra al tocar fuera */}
      <TouchableOpacity
        style={{ flex: 1, backgroundColor: "rgba(0,0,0,0.5)" }}
        activeOpacity={1}
        onPress={onClose}
      >
        {/* Panel del dropdown — posicionado debajo del header */}
        <TouchableOpacity
          activeOpacity={1}
          onPress={() => {}} // evitar que tap en el panel cierre el modal
          style={{
            position: "absolute",
            top: insets.top + 64,
            left: 20, right: 20,
            backgroundColor: colors.surface,
            borderRadius: 16,
            borderWidth: 1, borderColor: colors.primary + "40",
            overflow: "hidden",
            shadowColor: "#000",
            shadowOffset: { width: 0, height: 8 },
            shadowOpacity: 0.35, shadowRadius: 20, elevation: 12,
          }}
        >
          {/* Título del dropdown */}
          <View style={{
            flexDirection: "row", alignItems: "center", justifyContent: "space-between",
            paddingHorizontal: 16, paddingVertical: 12,
            borderBottomWidth: 1, borderBottomColor: colors.primary + "20",
          }}>
            <Text style={{ color: colors.textSecondary, fontSize: 11, fontWeight: "700", textTransform: "uppercase", letterSpacing: 1 }}>
              Filtrar por estado
            </Text>
            <TouchableOpacity onPress={onClose} activeOpacity={0.75}>
              <Ionicons name="close" size={18} color={colors.textMuted} />
            </TouchableOpacity>
          </View>

          {/* Opciones */}
          {FILTERS.map(({ key, label, icon }, idx) => {
            const active = filter === key;
            const cfg = key !== "all" ? getCfg(key) : { color: colors.accent, bg: colors.accent + "22", icon };
            const isLast = idx === FILTERS.length - 1;
            return (
              <TouchableOpacity
                key={key}
                onPress={() => { onSelect(key); onClose(); }}
                activeOpacity={0.75}
                style={{
                  flexDirection: "row", alignItems: "center", gap: 12,
                  paddingHorizontal: 16, paddingVertical: 13,
                  backgroundColor: active ? colors.primary + "18" : "transparent",
                  borderBottomWidth: isLast ? 0 : 1,
                  borderBottomColor: colors.primary + "12",
                }}
              >
                <View style={{
                  width: 32, height: 32, borderRadius: 9,
                  backgroundColor: active ? colors.primary + "30" : cfg.bg,
                  alignItems: "center", justifyContent: "center",
                }}>
                  <Ionicons name={icon} size={16} color={active ? colors.primary : cfg.color} />
                </View>
                <Text style={{
                  flex: 1,
                  color: active ? colors.textPrimary : colors.textSecondary,
                  fontSize: 14, fontWeight: active ? "700" : "500",
                }}>
                  {label}
                </Text>
                {active && (
                  <Ionicons name="checkmark" size={16} color={colors.primary} />
                )}
              </TouchableOpacity>
            );
          })}
        </TouchableOpacity>
      </TouchableOpacity>
    </Modal>
  );
}

// ─── Modal de detalle del evento ──────────────────────────────────────────────

function EventDetailModal({ event, detail, media, loading, onClose, onAction, actionLoading, colors, insets }) {
  const transitions = getAllowedTransitions(event.status);
  const [rejectReason, setRejectReason] = useState("");
  const [showRejectInput, setShowRejectInput] = useState(false);
  const cfg = getCfg(event.status);
  const coverUrl = media?.[0] ?? null;

  const handleTransition = (value) => {
    if (value === "REJECTED") { setShowRejectInput(true); return; }
    onAction(event.eventId, value, null);
  };

  const confirmReject = () => {
    if (!rejectReason.trim()) return;
    onAction(event.eventId, "REJECTED", rejectReason.trim());
  };

  return (
    <Modal visible transparent animationType="slide" onRequestClose={onClose}>
      <View style={{ flex: 1, backgroundColor: "rgba(0,0,0,0.75)", justifyContent: "flex-end" }}>
        <View style={{
          backgroundColor: colors.background,
          borderTopLeftRadius: 24, borderTopRightRadius: 24,
          maxHeight: "92%",
          borderWidth: 1, borderColor: colors.primary + "30",
        }}>
          {/* Handle + botón X */}
          <View style={{
            flexDirection: "row", alignItems: "center",
            paddingTop: 12, paddingHorizontal: 16, paddingBottom: 4,
          }}>
            <View style={{ flex: 1, alignItems: "center" }}>
              <View style={{ width: 40, height: 4, borderRadius: 2, backgroundColor: colors.textMuted + "50" }} />
            </View>
            <TouchableOpacity
              onPress={onClose}
              activeOpacity={0.75}
              style={{
                width: 34, height: 34, borderRadius: 17,
                backgroundColor: colors.surface,
                borderWidth: 1, borderColor: colors.primary + "30",
                alignItems: "center", justifyContent: "center",
              }}
            >
              <Ionicons name="close" size={18} color={colors.textSecondary} />
            </TouchableOpacity>
          </View>

          <ScrollView
            showsVerticalScrollIndicator={false}
            contentContainerStyle={{ paddingHorizontal: 20, paddingBottom: insets.bottom + 24, gap: 16 }}
          >
            {/* ── Imagen de portada prominente ── */}
            <View style={{ borderRadius: 16, overflow: "hidden", height: 190 }}>
              {coverUrl ? (
                <Image
                  source={{ uri: coverUrl.replace("localhost", process.env.EXPO_PUBLIC_MINIO_HOST ?? "localhost") }}
                  style={{ width: "100%", height: "100%" }}
                  resizeMode="cover"
                />
              ) : (
                <View style={{
                  flex: 1, backgroundColor: colors.surface,
                  alignItems: "center", justifyContent: "center", gap: 8,
                }}>
                  <Ionicons name="calendar" size={48} color={colors.textMuted} />
                  <Text style={{ color: colors.textMuted, fontSize: 12 }}>Sin imagen</Text>
                </View>
              )}
              {/* Badge de estado sobre la imagen */}
              <View style={{
                position: "absolute", top: 12, right: 12,
                backgroundColor: colors.background + "E0",
                borderRadius: 10, paddingHorizontal: 10, paddingVertical: 5,
                borderWidth: 1, borderColor: cfg.color + "60",
                flexDirection: "row", alignItems: "center", gap: 5,
              }}>
                <View style={{ width: 7, height: 7, borderRadius: 4, backgroundColor: cfg.color }} />
                <Text style={{ color: cfg.color, fontSize: 11, fontWeight: "700" }}>{cfg.label}</Text>
              </View>
            </View>

            {/* Nombre + ID */}
            <View>
              <Text style={{ color: colors.textPrimary, fontSize: 20, fontWeight: "800", lineHeight: 26 }}>
                {event.eventName}
              </Text>
              <Text style={{ color: colors.textMuted, fontSize: 12, marginTop: 3 }}>#{event.eventId}</Text>
            </View>

            {/* Grid de datos */}
            <View style={{ gap: 8 }}>
              {[
                { icon: "person-outline",   label: "Organizador", value: event.organizerName ?? "—" },
                { icon: "calendar-outline", label: "Fechas",      value: `${fmtDate(event.startDate)} → ${fmtDate(event.finishDate)}` },
                { icon: "pricetag-outline", label: "Categoría",   value: event.categoryName ?? "—" },
                { icon: "people-outline",   label: "Boletas",     value: event.availableSeats?.toLocaleString("es-CO") ?? "—" },
              ].map(({ icon, label, value }) => (
                <View key={label} style={{
                  flexDirection: "row", alignItems: "center", gap: 12,
                  backgroundColor: colors.surface + "CC", borderRadius: 12,
                  padding: 12, borderWidth: 1, borderColor: colors.primary + "20",
                }}>
                  <View style={{
                    width: 34, height: 34, borderRadius: 9,
                    backgroundColor: colors.primary + "22",
                    alignItems: "center", justifyContent: "center",
                  }}>
                    <Ionicons name={icon} size={16} color={colors.accent} />
                  </View>
                  <View style={{ flex: 1 }}>
                    <Text style={{ color: colors.textMuted, fontSize: 10, fontWeight: "700", textTransform: "uppercase", letterSpacing: 0.8 }}>
                      {label}
                    </Text>
                    <Text style={{ color: colors.textPrimary, fontSize: 13, fontWeight: "600", marginTop: 2 }}>
                      {value}
                    </Text>
                  </View>
                </View>
              ))}
            </View>

            {/* Descripción */}
            {loading && <ActivityIndicator size="small" color={colors.primary} />}
            {!loading && detail?.description && (
              <View style={{
                backgroundColor: colors.surface + "CC", borderRadius: 12,
                padding: 14, borderWidth: 1, borderColor: colors.primary + "20",
              }}>
                <Text style={{ color: colors.textMuted, fontSize: 10, fontWeight: "700", textTransform: "uppercase", letterSpacing: 0.8, marginBottom: 6 }}>
                  Descripción
                </Text>
                <Text style={{ color: colors.textSecondary, fontSize: 13, lineHeight: 20 }}>
                  {detail.description}
                </Text>
              </View>
            )}

            {/* Input motivo de rechazo */}
            {showRejectInput && (
              <View style={{ gap: 10 }}>
                <Text style={{ color: colors.textPrimary, fontSize: 14, fontWeight: "700" }}>
                  Motivo de rechazo
                </Text>
                <TextInput
                  value={rejectReason}
                  onChangeText={setRejectReason}
                  placeholder="Escribe el motivo del rechazo..."
                  placeholderTextColor={colors.textMuted}
                  multiline numberOfLines={3}
                  style={{
                    backgroundColor: colors.surface, borderRadius: 12, padding: 14,
                    color: colors.textPrimary, fontSize: 13,
                    borderWidth: 1, borderColor: colors.primary + "30",
                    minHeight: 90, textAlignVertical: "top",
                  }}
                />
                <View style={{ flexDirection: "row", gap: 10 }}>
                  <TouchableOpacity
                    onPress={() => setShowRejectInput(false)} activeOpacity={0.8}
                    style={{
                      flex: 1, paddingVertical: 14, borderRadius: 13,
                      borderWidth: 1, borderColor: colors.primary + "30",
                      alignItems: "center",
                    }}
                  >
                    <Text style={{ color: colors.textSecondary, fontWeight: "700" }}>Cancelar</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={confirmReject}
                    disabled={!rejectReason.trim() || actionLoading}
                    activeOpacity={0.8}
                    style={{
                      flex: 1, paddingVertical: 14, borderRadius: 13,
                      backgroundColor: "#f87171", alignItems: "center",
                      opacity: !rejectReason.trim() ? 0.5 : 1,
                    }}
                  >
                    {actionLoading
                      ? <ActivityIndicator size="small" color="#fff" />
                      : <Text style={{ color: "#fff", fontWeight: "700" }}>Confirmar rechazo</Text>
                    }
                  </TouchableOpacity>
                </View>
              </View>
            )}

            {/* Botones de transición */}
            {!showRejectInput && transitions.length > 0 && (
              <View style={{ flexDirection: "row", gap: 10 }}>
                {transitions.map(({ value, label, icon, color }) => (
                  <TouchableOpacity
                    key={value}
                    onPress={() => handleTransition(value)}
                    disabled={actionLoading}
                    activeOpacity={0.8}
                    style={{
                      flex: 1, flexDirection: "row", alignItems: "center",
                      justifyContent: "center", gap: 6,
                      paddingVertical: 15, borderRadius: 13,
                      backgroundColor: color + "22",
                      borderWidth: 1, borderColor: color + "55",
                      opacity: actionLoading ? 0.5 : 1,
                    }}
                  >
                    {actionLoading
                      ? <ActivityIndicator size="small" color={color} />
                      : <>
                          <Ionicons name={icon} size={18} color={color} />
                          <Text style={{ color, fontWeight: "700", fontSize: 14 }}>{label}</Text>
                        </>
                    }
                  </TouchableOpacity>
                ))}
              </View>
            )}

            {!showRejectInput && transitions.length === 0 && (
              <View style={{
                backgroundColor: colors.surface + "CC", borderRadius: 12, padding: 16,
                borderWidth: 1, borderColor: colors.primary + "15", alignItems: "center",
              }}>
                <Text style={{ color: colors.textMuted, fontSize: 13 }}>
                  No hay acciones disponibles para este estado
                </Text>
              </View>
            )}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

// ─── Componente principal ─────────────────────────────────────────────────────

export default function AdminEventsScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();

  const [events, setEvents]           = useState([]);
  const [loading, setLoading]         = useState(true);
  const [refreshing, setRefreshing]   = useState(false);
  const [filter, setFilter]           = useState("PENDING_REVIEW");
  const [showFilterDropdown, setShowFilterDropdown] = useState(false);
  const [error, setError]             = useState(null);

  const [selected, setSelected]           = useState(null);
  const [detailData, setDetailData]       = useState(null);
  const [mediaUrls, setMediaUrls]         = useState([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [feedbackModal, setFeedbackModal] = useState({ visible: false, type: "info", title: "", message: "" });

  const currentFilterLabel = FILTERS.find((f) => f.key === filter)?.label ?? "Todos";

  const fetchEvents = useCallback(async () => {
    setError(null);
    try {
      const data = await getAdminEvents(filter === "all" ? null : filter);
      setEvents(data);
    } catch (e) {
      setError(e.message || "Error al cargar eventos");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [filter]);

  useEffect(() => { setLoading(true); fetchEvents(); }, [fetchEvents]);

  const onRefresh = () => { setRefreshing(true); fetchEvents(); };

  const openDetail = async (ev) => {
    setSelected(ev);
    setDetailData(null);
    setMediaUrls([]);
    setDetailLoading(true);
    try {
      const [dr, mr] = await Promise.allSettled([getEventDetail(ev.eventId), getEventMedia(ev.eventId)]);
      if (dr.status === "fulfilled") setDetailData(dr.value?.data ?? dr.value ?? null);
      if (mr.status === "fulfilled") {
        const urls = (mr.value?.data ?? []).sort((a, b) => a.mediaId - b.mediaId).map((m) => m.imgUrl).filter(Boolean);
        setMediaUrls(urls);
      }
    } catch { /* silencioso */ }
    finally { setDetailLoading(false); }
  };

  const closeDetail = () => { setSelected(null); setDetailData(null); setMediaUrls([]); };

  const handleAction = async (eventId, newStatus, reason) => {
    setActionLoading(true);
    try {
      await adminChangeEventStatus(eventId, newStatus, reason);
      closeDetail();
      await fetchEvents();
      setFeedbackModal({ visible: true, type: "success", title: "Estado actualizado", message: `El evento fue ${getCfg(newStatus).label.toLowerCase()} correctamente.` });
    } catch (e) {
      let msg = e.message || "Error al cambiar el estado";
      if (msg.includes("EVENT_START_DATE_IN_PAST"))        msg = "No se puede publicar: la fecha de inicio ya pasó.";
      if (msg.includes("EVENT_PUBLISH_MEDIA_COUNT_INVALID")) msg = "Requiere entre 3 y 9 imágenes para publicarse.";
      if (msg.includes("EVENT_SECTIONS_REQUIRED"))          msg = "Debe tener al menos una sección configurada.";
      if (msg.includes("EVENT_REJECTION_REASON_REQUIRED"))  msg = "El motivo de rechazo es obligatorio.";
      setFeedbackModal({ visible: true, type: "error", title: "Error", message: msg });
    } finally {
      setActionLoading(false);
    }
  };

  const pendingCount = events.filter((e) => e.status === "PENDING_REVIEW").length;

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <StatusBar barStyle="light-content" backgroundColor={colors.surface} />

      {/* ── Header ── */}
      <View style={{
        flexDirection: "row", alignItems: "center",
        paddingTop: insets.top + 16, paddingBottom: 16,
        paddingHorizontal: 20,
        backgroundColor: colors.surface,
        borderBottomWidth: 1, borderBottomColor: colors.primary + "30",
        gap: 12,
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

        <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "800", flex: 1 }}>
          Gestión de eventos
        </Text>

        {pendingCount > 0 && (
          <View style={{
            backgroundColor: "#f59e0b", borderRadius: 10,
            minWidth: 22, height: 22, alignItems: "center",
            justifyContent: "center", paddingHorizontal: 5,
          }}>
            <Text style={{ color: "#fff", fontSize: 11, fontWeight: "800" }}>{pendingCount}</Text>
          </View>
        )}

        {/* ── Botón de filtro en header ── */}
        <TouchableOpacity
          onPress={() => setShowFilterDropdown(true)}
          activeOpacity={0.8}
          style={{
            flexDirection: "row", alignItems: "center", gap: 6,
            backgroundColor: colors.primary + "22",
            borderRadius: 10, paddingHorizontal: 10, paddingVertical: 7,
            borderWidth: 1, borderColor: colors.primary + "40",
          }}
        >
          <Ionicons name="filter-outline" size={15} color={colors.accent} />
          <Text style={{ color: colors.accent, fontSize: 12, fontWeight: "700", maxWidth: 80 }} numberOfLines={1}>
            {currentFilterLabel}
          </Text>
          <Ionicons name="chevron-down" size={13} color={colors.accent} />
        </TouchableOpacity>
      </View>

      {/* ── Lista de eventos ── */}
      {loading ? (
        <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      ) : error ? (
        <View style={{ flex: 1, alignItems: "center", justifyContent: "center", padding: 32, gap: 12 }}>
          <Ionicons name="alert-circle-outline" size={48} color={colors.error} />
          <Text style={{ color: colors.error, fontSize: 14, textAlign: "center" }}>{error}</Text>
          <TouchableOpacity
            onPress={fetchEvents} activeOpacity={0.8}
            style={{ backgroundColor: colors.primary, borderRadius: 12, paddingHorizontal: 24, paddingVertical: 10 }}
          >
            <Text style={{ color: colors.textPrimary, fontWeight: "700" }}>Reintentar</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={
            events.length === 0
              ? { flex: 1 }
              : { paddingHorizontal: 20, paddingTop: 16, paddingBottom: insets.bottom + 32, gap: 14 }
          }
          showsVerticalScrollIndicator={false}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}
        >
          {events.length === 0 ? (
            <View style={{ flex: 1, alignItems: "center", justifyContent: "center", gap: 12 }}>
              <Ionicons name="calendar-outline" size={52} color={colors.textMuted} />
              <Text style={{ color: colors.textSecondary, fontSize: 14, textAlign: "center" }}>
                No hay eventos en "{currentFilterLabel}"
              </Text>
            </View>
          ) : (
            events.map((ev) => {
              const cfg = getCfg(ev.status);
              const transitions = getAllowedTransitions(ev.status);
              const minio = process.env.EXPO_PUBLIC_MINIO_HOST ?? "localhost";
              const cover = ev.coverUrl ? ev.coverUrl.replace("localhost", minio) : null;

              return (
                <TouchableOpacity
                  key={ev.eventId}
                  onPress={() => openDetail(ev)}
                  activeOpacity={0.75}
                  style={{
                    backgroundColor: colors.surface + "CC",
                    borderRadius: 16, borderWidth: 1,
                    borderColor: colors.primary + "25",
                    overflow: "hidden",
                  }}
                >
                  {/* ── Imagen de portada ── */}
                  <View style={{ height: 130, backgroundColor: colors.surface }}>
                    {cover ? (
                      <Image source={{ uri: cover }} style={{ width: "100%", height: "100%" }} resizeMode="cover" />
                    ) : (
                      <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
                        <Ionicons name="calendar" size={36} color={colors.textMuted} />
                      </View>
                    )}
                    {/* Badge estado sobre imagen */}
                    <View style={{
                      position: "absolute", top: 10, right: 10,
                      backgroundColor: colors.background + "DD",
                      borderRadius: 8, paddingHorizontal: 9, paddingVertical: 4,
                      borderWidth: 1, borderColor: cfg.color + "55",
                      flexDirection: "row", alignItems: "center", gap: 5,
                    }}>
                      <View style={{ width: 6, height: 6, borderRadius: 3, backgroundColor: cfg.color }} />
                      <Text style={{ color: cfg.color, fontSize: 11, fontWeight: "700" }}>{cfg.label}</Text>
                    </View>
                  </View>

                  {/* ── Contenido inferior de la card ── */}
                  <View style={{ padding: 14 }}>
                    <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700", lineHeight: 20 }} numberOfLines={2}>
                      {ev.eventName}
                    </Text>
                    <Text style={{ color: colors.textMuted, fontSize: 12, marginTop: 3 }} numberOfLines={1}>
                      {ev.organizerName ?? "Organizador desconocido"}
                    </Text>

                    {/* Fechas */}
                    <View style={{ flexDirection: "row", alignItems: "center", gap: 5, marginTop: 6 }}>
                      <Ionicons name="calendar-outline" size={13} color={colors.textMuted} />
                      <Text style={{ color: colors.textSecondary, fontSize: 12 }}>
                        {fmtDate(ev.startDate)} → {fmtDate(ev.finishDate)}
                      </Text>
                    </View>

                    {/* Acciones rápidas */}
                    {transitions.length > 0 && (
                      <View style={{
                        flexDirection: "row", gap: 8, marginTop: 12,
                        paddingTop: 12,
                        borderTopWidth: 1, borderTopColor: colors.primary + "18",
                      }}>
                        {transitions.map(({ value, label, icon, color }) => (
                          <TouchableOpacity
                            key={value}
                            onPress={() => {
                              if (value === "REJECTED") { openDetail(ev); }
                              else { handleAction(ev.eventId, value, null); }
                            }}
                            disabled={actionLoading}
                            activeOpacity={0.8}
                            style={{
                              flex: 1, flexDirection: "row", alignItems: "center",
                              justifyContent: "center", gap: 5,
                              paddingVertical: 9, borderRadius: 10,
                              backgroundColor: color + "22",
                              borderWidth: 1, borderColor: color + "40",
                            }}
                          >
                            <Ionicons name={icon} size={15} color={color} />
                            <Text style={{ color, fontSize: 12, fontWeight: "700" }}>{label}</Text>
                          </TouchableOpacity>
                        ))}
                        {/* Ver detalle */}
                        <TouchableOpacity
                          onPress={() => openDetail(ev)}
                          activeOpacity={0.8}
                          style={{
                            width: 36, height: 36, borderRadius: 10,
                            backgroundColor: colors.primary + "22",
                            alignItems: "center", justifyContent: "center",
                          }}
                        >
                          <Ionicons name="eye-outline" size={17} color={colors.primary} />
                        </TouchableOpacity>
                      </View>
                    )}
                  </View>
                </TouchableOpacity>
              );
            })
          )}
        </ScrollView>
      )}

      {/* ── Dropdown de filtros ── */}
      {showFilterDropdown && (
        <FilterDropdown
          filter={filter}
          onSelect={setFilter}
          onClose={() => setShowFilterDropdown(false)}
          colors={colors}
          insets={insets}
        />
      )}

      {/* ── Modal de detalle ── */}
      {selected && (
        <EventDetailModal
          event={selected}
          detail={detailData}
          media={mediaUrls}
          loading={detailLoading}
          onClose={closeDetail}
          onAction={handleAction}
          actionLoading={actionLoading}
          colors={colors}
          insets={insets}
        />
      )}

      {/* ── Modal de feedback ── */}
      <AppModal
        visible={feedbackModal.visible}
        type={feedbackModal.type}
        title={feedbackModal.title}
        message={feedbackModal.message}
        confirmText="Entendido"
        onConfirm={() => setFeedbackModal((p) => ({ ...p, visible: false }))}
      />
    </View>
  );
}
