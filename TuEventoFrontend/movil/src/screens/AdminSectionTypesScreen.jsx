import { useState, useEffect, useCallback } from "react";
import {
  View, Text, ScrollView, TouchableOpacity, ActivityIndicator,
  StatusBar, RefreshControl, Modal, TextInput,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTheme } from "../context/ThemeContext";
import {
  getAllSectionTypes, createSectionType, updateSectionType, toggleSectionTypeActive,
} from "../services/catalogService";
import AppModal from "../components/AppModal";

const FILTERS = [
  { key: "all",      label: "Todos" },
  { key: "active",   label: "Activos" },
  { key: "inactive", label: "Inactivos" },
];

export default function AdminSectionTypesScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();

  const [sectionTypes, setSectionTypes] = useState([]);
  const [loading, setLoading]           = useState(true);
  const [refreshing, setRefreshing]     = useState(false);
  const [error, setError]               = useState(null);
  const [filter, setFilter]             = useState("all");

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing]   = useState(null);
  const [name, setName]         = useState("");
  const [saving, setSaving]     = useState(false);

  const [feedback, setFeedback] = useState({ visible: false, type: "info", title: "", message: "" });

  const load = useCallback(async () => {
    setError(null);
    try {
      setSectionTypes(await getAllSectionTypes());
    } catch {
      setError("No se pudieron cargar los tipos de sección.");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);
  const onRefresh = () => { setRefreshing(true); load(); };

  const openCreate = () => { setEditing(null); setName(""); setFormOpen(true); };
  const openEdit = (st) => { setEditing(st); setName(st.name || ""); setFormOpen(true); };
  const closeForm = () => { if (!saving) setFormOpen(false); };

  const handleSave = async () => {
    if (!name.trim()) return;
    setSaving(true);
    try {
      if (editing) await updateSectionType(editing.sectionTypeId, name);
      else await createSectionType(name);
      setFormOpen(false);
      await load();
      setFeedback({ visible: true, type: "success", title: editing ? "Tipo actualizado" : "Tipo creado", message: "Los cambios se guardaron correctamente." });
    } catch (e) {
      setFeedback({ visible: true, type: "error", title: "Error", message: e.message });
    } finally {
      setSaving(false);
    }
  };

  const handleToggle = async (st) => {
    try {
      await toggleSectionTypeActive(st.sectionTypeId, st.active);
      await load();
    } catch (e) {
      setFeedback({ visible: true, type: "error", title: "Error", message: e.message });
    }
  };

  const filtered = sectionTypes.filter((st) => {
    if (filter === "active")   return st.active;
    if (filter === "inactive") return !st.active;
    return true;
  });

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
          style={{ width: 38, height: 38, borderRadius: 12, backgroundColor: colors.primary + "28", borderWidth: 1, borderColor: colors.primary + "40", alignItems: "center", justifyContent: "center" }}
        >
          <Ionicons name="arrow-back" size={20} color={colors.accent} />
        </TouchableOpacity>
        <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "800", flex: 1 }}>Tipos de sección</Text>
        <TouchableOpacity
          onPress={openCreate} activeOpacity={0.8}
          style={{ flexDirection: "row", alignItems: "center", gap: 5, backgroundColor: colors.primary, borderRadius: 11, paddingHorizontal: 12, paddingVertical: 8 }}
        >
          <Ionicons name="add" size={16} color={colors.textPrimary} />
          <Text style={{ color: colors.textPrimary, fontSize: 13, fontWeight: "700" }}>Nuevo</Text>
        </TouchableOpacity>
      </View>

      {/* Filtros */}
      <View style={{ flexDirection: "row", paddingHorizontal: 20, paddingVertical: 12, gap: 8 }}>
        {FILTERS.map(({ key, label }) => {
          const active = filter === key;
          return (
            <TouchableOpacity
              key={key} onPress={() => setFilter(key)} activeOpacity={0.8}
              style={{
                flex: 1, alignItems: "center", paddingVertical: 8, borderRadius: 11, borderWidth: 1,
                backgroundColor: active ? colors.primary : colors.primary + "15",
                borderColor: active ? colors.primary : colors.primary + "30",
              }}
            >
              <Text style={{ color: active ? colors.textPrimary : colors.textSecondary, fontSize: 12, fontWeight: "700" }}>{label}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

      {/* Lista */}
      {loading ? (
        <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={filtered.length === 0 ? { flex: 1 } : { paddingHorizontal: 20, paddingBottom: insets.bottom + 32, gap: 12 }}
          showsVerticalScrollIndicator={false}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}
        >
          {error && (
            <View style={{ flexDirection: "row", alignItems: "center", gap: 8, backgroundColor: colors.error + "15", borderRadius: 12, padding: 14, borderWidth: 1, borderColor: colors.error + "40", marginBottom: 4 }}>
              <Ionicons name="alert-circle-outline" size={18} color={colors.error} />
              <Text style={{ color: colors.error, fontSize: 13, flex: 1 }}>{error}</Text>
            </View>
          )}

          {filtered.length === 0 ? (
            <View style={{ flex: 1, alignItems: "center", justifyContent: "center", gap: 12 }}>
              <Ionicons name="layers-outline" size={52} color={colors.textMuted} />
              <Text style={{ color: colors.textSecondary, fontSize: 14 }}>No hay tipos de sección</Text>
            </View>
          ) : (
            filtered.map((st) => (
              <View key={st.sectionTypeId} style={{
                backgroundColor: colors.surface + "CC", borderRadius: 14,
                borderWidth: 1, borderColor: colors.primary + "20", padding: 14,
              }}>
                <View style={{ flexDirection: "row", alignItems: "center", gap: 12 }}>
                  <View style={{ width: 40, height: 40, borderRadius: 11, backgroundColor: colors.accent + "22", alignItems: "center", justifyContent: "center" }}>
                    <Ionicons name="layers-outline" size={19} color={colors.accent} />
                  </View>
                  <View style={{ flex: 1 }}>
                    <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700" }}>{st.name}</Text>
                    <Text style={{ color: colors.textMuted, fontSize: 11, marginTop: 2 }}>ID: {st.sectionTypeId}</Text>
                  </View>
                  <View style={{
                    flexDirection: "row", alignItems: "center", gap: 4,
                    backgroundColor: (st.active ? "#34d399" : "#64748b") + "22",
                    borderRadius: 8, paddingHorizontal: 8, paddingVertical: 3,
                  }}>
                    <View style={{ width: 6, height: 6, borderRadius: 3, backgroundColor: st.active ? "#34d399" : "#64748b" }} />
                    <Text style={{ color: st.active ? "#34d399" : "#94a3b8", fontSize: 10, fontWeight: "700" }}>
                      {st.active ? "Activo" : "Inactivo"}
                    </Text>
                  </View>
                </View>

                <View style={{ flexDirection: "row", gap: 8, marginTop: 12, paddingTop: 12, borderTopWidth: 1, borderTopColor: colors.primary + "18" }}>
                  <TouchableOpacity
                    onPress={() => openEdit(st)} activeOpacity={0.8}
                    style={{ flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 6, paddingVertical: 9, borderRadius: 10, backgroundColor: colors.primary + "18", borderWidth: 1, borderColor: colors.primary + "35" }}
                  >
                    <Ionicons name="create-outline" size={15} color={colors.accent} />
                    <Text style={{ color: colors.accent, fontSize: 12, fontWeight: "700" }}>Editar</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={() => handleToggle(st)} activeOpacity={0.8}
                    style={{
                      flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 6, paddingVertical: 9, borderRadius: 10,
                      backgroundColor: (st.active ? "#f87171" : "#34d399") + "18",
                      borderWidth: 1, borderColor: (st.active ? "#f87171" : "#34d399") + "40",
                    }}
                  >
                    <Ionicons name={st.active ? "pause-circle-outline" : "play-circle-outline"} size={15} color={st.active ? "#f87171" : "#34d399"} />
                    <Text style={{ color: st.active ? "#f87171" : "#34d399", fontSize: 12, fontWeight: "700" }}>
                      {st.active ? "Desactivar" : "Activar"}
                    </Text>
                  </TouchableOpacity>
                </View>
              </View>
            ))
          )}
        </ScrollView>
      )}

      {/* ── Modal formulario ── */}
      <Modal visible={formOpen} transparent animationType="slide" onRequestClose={closeForm}>
        <View style={{ flex: 1, backgroundColor: "rgba(0,0,0,0.7)", justifyContent: "flex-end" }}>
          <View style={{ backgroundColor: colors.background, borderTopLeftRadius: 24, borderTopRightRadius: 24, borderWidth: 1, borderColor: colors.primary + "30" }}>
            <View style={{ flexDirection: "row", alignItems: "center", padding: 16, borderBottomWidth: 1, borderBottomColor: colors.primary + "20" }}>
              <Text style={{ flex: 1, color: colors.textPrimary, fontSize: 17, fontWeight: "800" }}>
                {editing ? "Editar tipo de sección" : "Nuevo tipo de sección"}
              </Text>
              <TouchableOpacity onPress={closeForm} activeOpacity={0.75} style={{ width: 34, height: 34, borderRadius: 17, backgroundColor: colors.surface, alignItems: "center", justifyContent: "center" }}>
                <Ionicons name="close" size={18} color={colors.textSecondary} />
              </TouchableOpacity>
            </View>

            <View style={{ padding: 20, gap: 16, paddingBottom: insets.bottom + 24 }}>
              <View>
                <Text style={{ color: colors.textMuted, fontSize: 11, fontWeight: "700", textTransform: "uppercase", letterSpacing: 0.8, marginBottom: 8 }}>Nombre *</Text>
                <TextInput
                  value={name} onChangeText={setName} autoFocus
                  placeholder="Ej: General, VIP, Palco" placeholderTextColor={colors.textMuted}
                  style={{ backgroundColor: colors.surface, borderRadius: 12, paddingHorizontal: 14, paddingVertical: 12, color: colors.textPrimary, fontSize: 14, borderWidth: 1, borderColor: colors.primary + "30" }}
                />
              </View>

              {!editing && (
                <View style={{ flexDirection: "row", alignItems: "flex-start", gap: 10, backgroundColor: colors.primary + "0D", borderRadius: 12, padding: 12, borderWidth: 1, borderColor: colors.primary + "25" }}>
                  <Ionicons name="information-circle-outline" size={16} color={colors.accent} style={{ marginTop: 1 }} />
                  <Text style={{ color: colors.textSecondary, fontSize: 12, lineHeight: 18, flex: 1 }}>
                    Los tipos de sección definen categorías como General, VIP, Palco o Tribuna para organizar los eventos.
                  </Text>
                </View>
              )}

              <TouchableOpacity
                onPress={handleSave}
                disabled={!name.trim() || saving}
                activeOpacity={0.8}
                style={{ backgroundColor: colors.primary, borderRadius: 13, paddingVertical: 15, alignItems: "center", opacity: (!name.trim() || saving) ? 0.5 : 1 }}
              >
                {saving
                  ? <ActivityIndicator size="small" color={colors.textPrimary} />
                  : <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700" }}>{editing ? "Guardar cambios" : "Crear tipo"}</Text>
                }
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      <AppModal
        visible={feedback.visible}
        type={feedback.type}
        title={feedback.title}
        message={feedback.message}
        confirmText="Entendido"
        onConfirm={() => setFeedback((p) => ({ ...p, visible: false }))}
      />
    </View>
  );
}
