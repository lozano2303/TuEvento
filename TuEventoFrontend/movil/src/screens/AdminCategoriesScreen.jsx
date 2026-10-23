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
  getAllCategories, getRootCategories,
  createCategory, updateCategory, toggleCategoryActive,
} from "../services/catalogService";
import AppModal from "../components/AppModal";

const FILTERS = [
  { key: "all",           label: "Todas" },
  { key: "active",        label: "Activas" },
  { key: "inactive",      label: "Inactivas" },
  { key: "parents",       label: "Principales" },
  { key: "subcategories", label: "Subcategorías" },
];

export default function AdminCategoriesScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();

  const [categories, setCategories]     = useState([]);
  const [rootCategories, setRootCats]   = useState([]);
  const [loading, setLoading]           = useState(true);
  const [refreshing, setRefreshing]     = useState(false);
  const [error, setError]               = useState(null);
  const [filter, setFilter]             = useState("all");

  // Modal de formulario (crear / editar)
  const [formOpen, setFormOpen]         = useState(false);
  const [editing, setEditing]           = useState(null); // categoría en edición o null
  const [name, setName]                 = useState("");
  const [description, setDescription]   = useState("");
  const [dadId, setDadId]               = useState(null);
  const [parentPickerOpen, setParentPickerOpen] = useState(false);
  const [saving, setSaving]             = useState(false);

  // Modal de feedback
  const [feedback, setFeedback] = useState({ visible: false, type: "info", title: "", message: "" });

  const load = useCallback(async () => {
    setError(null);
    try {
      const [cats, roots] = await Promise.allSettled([getAllCategories("es"), getRootCategories("es")]);
      if (cats.status === "fulfilled") setCategories(cats.value);
      else setError("No se pudieron cargar las categorías.");
      if (roots.status === "fulfilled") setRootCats(roots.value);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);
  const onRefresh = () => { setRefreshing(true); load(); };

  const openCreate = () => {
    setEditing(null); setName(""); setDescription(""); setDadId(null);
    setFormOpen(true);
  };
  const openEdit = (cat) => {
    setEditing(cat); setName(cat.name || ""); setDescription(cat.description || "");
    setDadId(cat.dadId || null);
    setFormOpen(true);
  };
  const closeForm = () => { if (!saving) setFormOpen(false); };

  const handleSave = async () => {
    if (!name.trim()) return;
    setSaving(true);
    try {
      if (editing) {
        await updateCategory(editing.categoryId, { name, description, dadId });
      } else {
        await createCategory({ name, description, dadId });
      }
      setFormOpen(false);
      await load();
      setFeedback({ visible: true, type: "success", title: editing ? "Categoría actualizada" : "Categoría creada", message: "Los cambios se guardaron correctamente." });
    } catch (e) {
      setFeedback({ visible: true, type: "error", title: "Error", message: e.message });
    } finally {
      setSaving(false);
    }
  };

  const handleToggle = async (cat) => {
    try {
      await toggleCategoryActive(cat.categoryId, cat.active);
      await load();
    } catch (e) {
      setFeedback({ visible: true, type: "error", title: "Error", message: e.message });
    }
  };

  const parentName = (id) => categories.find((c) => c.categoryId === id)?.name ?? "—";

  const filtered = categories.filter((c) => {
    if (filter === "active")        return c.active;
    if (filter === "inactive")      return !c.active;
    if (filter === "parents")       return !c.dadId;
    if (filter === "subcategories") return !!c.dadId;
    return true;
  });

  // Opciones del selector de padre (solo categorías raíz, excluyendo la propia en edición)
  const parentOptions = rootCategories.filter((r) => !editing || r.categoryId !== editing.categoryId);

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
        <Text style={{ color: colors.textPrimary, fontSize: 18, fontWeight: "800", flex: 1 }}>Categorías</Text>
        <TouchableOpacity
          onPress={openCreate} activeOpacity={0.8}
          style={{ flexDirection: "row", alignItems: "center", gap: 5, backgroundColor: colors.primary, borderRadius: 11, paddingHorizontal: 12, paddingVertical: 8 }}
        >
          <Ionicons name="add" size={16} color={colors.textPrimary} />
          <Text style={{ color: colors.textPrimary, fontSize: 13, fontWeight: "700" }}>Nueva</Text>
        </TouchableOpacity>
      </View>

      {/* Filtros */}
      <View style={{ flexGrow: 0 }}>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={{ paddingHorizontal: 20, paddingVertical: 12, gap: 8, alignItems: "center" }}
        >
          {FILTERS.map(({ key, label }) => {
            const active = filter === key;
            return (
              <TouchableOpacity
                key={key}
                onPress={() => setFilter(key)}
                activeOpacity={0.8}
                style={{
                  height: 34, justifyContent: "center",
                  paddingHorizontal: 16, borderRadius: 17, borderWidth: 1,
                  backgroundColor: active ? colors.primary : colors.primary + "15",
                  borderColor: active ? colors.primary : colors.primary + "30",
                }}
              >
                <Text style={{ color: active ? colors.textPrimary : colors.textSecondary, fontSize: 13, fontWeight: "700" }}>
                  {label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
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
              <Ionicons name="pricetags-outline" size={52} color={colors.textMuted} />
              <Text style={{ color: colors.textSecondary, fontSize: 14 }}>No hay categorías para mostrar</Text>
            </View>
          ) : (
            filtered.map((cat) => (
              <View key={cat.categoryId} style={{
                backgroundColor: colors.surface + "CC", borderRadius: 14,
                borderWidth: 1, borderColor: colors.primary + "20", padding: 14,
              }}>
                <View style={{ flexDirection: "row", alignItems: "flex-start", gap: 12 }}>
                  <View style={{ width: 40, height: 40, borderRadius: 11, backgroundColor: colors.accent + "22", alignItems: "center", justifyContent: "center" }}>
                    <Ionicons name="pricetag-outline" size={19} color={colors.accent} />
                  </View>

                  <View style={{ flex: 1, minWidth: 0 }}>
                    {/* Nombre + estado en la misma fila, nombre truncado */}
                    <View style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
                      <Text
                        style={{ flex: 1, color: colors.textPrimary, fontSize: 15, fontWeight: "700" }}
                        numberOfLines={1}
                      >
                        {cat.name}
                      </Text>
                      <View style={{
                        flexDirection: "row", alignItems: "center", gap: 4,
                        backgroundColor: (cat.active ? "#34d399" : "#64748b") + "22",
                        borderRadius: 8, paddingHorizontal: 8, paddingVertical: 3,
                      }}>
                        <View style={{ width: 6, height: 6, borderRadius: 3, backgroundColor: cat.active ? "#34d399" : "#64748b" }} />
                        <Text style={{ color: cat.active ? "#34d399" : "#94a3b8", fontSize: 10, fontWeight: "700" }}>
                          {cat.active ? "Activa" : "Inactiva"}
                        </Text>
                      </View>
                    </View>

                    {/* Tag de tipo (principal / subcategoría) en su propia línea */}
                    <View style={{ flexDirection: "row", marginTop: 6 }}>
                      <View style={{
                        flexDirection: "row", alignItems: "center", gap: 3,
                        backgroundColor: cat.dadId ? "#f59e0b22" : colors.accent + "20",
                        borderRadius: 7, paddingHorizontal: 8, paddingVertical: 3,
                      }}>
                        {cat.dadId && <Ionicons name="chevron-forward" size={10} color="#f59e0b" />}
                        <Text style={{ color: cat.dadId ? "#f59e0b" : colors.accent, fontSize: 9, fontWeight: "800", letterSpacing: 0.4 }}>
                          {cat.dadId ? "SUBCATEGORÍA" : "PRINCIPAL"}
                        </Text>
                      </View>
                    </View>

                    {cat.dadId && (
                      <Text style={{ color: colors.textMuted, fontSize: 11, marginTop: 6 }}>
                        Padre: {parentName(cat.dadId)}
                      </Text>
                    )}
                    {!!cat.description && (
                      <Text style={{ color: colors.textSecondary, fontSize: 12, marginTop: 6 }} numberOfLines={2}>
                        {cat.description}
                      </Text>
                    )}
                  </View>
                </View>

                {/* Acciones */}
                <View style={{ flexDirection: "row", gap: 8, marginTop: 12, paddingTop: 12, borderTopWidth: 1, borderTopColor: colors.primary + "18" }}>
                  <TouchableOpacity
                    onPress={() => openEdit(cat)} activeOpacity={0.8}
                    style={{ flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 6, paddingVertical: 9, borderRadius: 10, backgroundColor: colors.primary + "18", borderWidth: 1, borderColor: colors.primary + "35" }}
                  >
                    <Ionicons name="create-outline" size={15} color={colors.accent} />
                    <Text style={{ color: colors.accent, fontSize: 12, fontWeight: "700" }}>Editar</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={() => handleToggle(cat)} activeOpacity={0.8}
                    style={{
                      flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 6,
                      paddingVertical: 9, borderRadius: 10,
                      backgroundColor: (cat.active ? "#f87171" : "#34d399") + "18",
                      borderWidth: 1, borderColor: (cat.active ? "#f87171" : "#34d399") + "40",
                    }}
                  >
                    <Ionicons name={cat.active ? "pause-circle-outline" : "play-circle-outline"} size={15} color={cat.active ? "#f87171" : "#34d399"} />
                    <Text style={{ color: cat.active ? "#f87171" : "#34d399", fontSize: 12, fontWeight: "700" }}>
                      {cat.active ? "Desactivar" : "Activar"}
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
          <View style={{
            backgroundColor: colors.background, borderTopLeftRadius: 24, borderTopRightRadius: 24,
            maxHeight: "88%", borderWidth: 1, borderColor: colors.primary + "30",
          }}>
            <View style={{ flexDirection: "row", alignItems: "center", padding: 16, borderBottomWidth: 1, borderBottomColor: colors.primary + "20" }}>
              <Text style={{ flex: 1, color: colors.textPrimary, fontSize: 17, fontWeight: "800" }}>
                {editing ? "Editar categoría" : "Nueva categoría"}
              </Text>
              <TouchableOpacity onPress={closeForm} activeOpacity={0.75} style={{ width: 34, height: 34, borderRadius: 17, backgroundColor: colors.surface, alignItems: "center", justifyContent: "center" }}>
                <Ionicons name="close" size={18} color={colors.textSecondary} />
              </TouchableOpacity>
            </View>

            <ScrollView contentContainerStyle={{ padding: 20, gap: 16, paddingBottom: insets.bottom + 24 }} showsVerticalScrollIndicator={false}>
              {/* Nombre */}
              <View>
                <Text style={{ color: colors.textMuted, fontSize: 11, fontWeight: "700", textTransform: "uppercase", letterSpacing: 0.8, marginBottom: 8 }}>Nombre *</Text>
                <TextInput
                  value={name} onChangeText={setName}
                  placeholder="Nombre de la categoría" placeholderTextColor={colors.textMuted}
                  style={{ backgroundColor: colors.surface, borderRadius: 12, paddingHorizontal: 14, paddingVertical: 12, color: colors.textPrimary, fontSize: 14, borderWidth: 1, borderColor: colors.primary + "30" }}
                />
              </View>

              {/* Descripción */}
              <View>
                <Text style={{ color: colors.textMuted, fontSize: 11, fontWeight: "700", textTransform: "uppercase", letterSpacing: 0.8, marginBottom: 8 }}>Descripción</Text>
                <TextInput
                  value={description} onChangeText={setDescription}
                  placeholder="Descripción (opcional)" placeholderTextColor={colors.textMuted}
                  multiline numberOfLines={3}
                  style={{ backgroundColor: colors.surface, borderRadius: 12, paddingHorizontal: 14, paddingVertical: 12, color: colors.textPrimary, fontSize: 14, borderWidth: 1, borderColor: colors.primary + "30", minHeight: 80, textAlignVertical: "top" }}
                />
              </View>

              {/* Categoría padre */}
              <View>
                <Text style={{ color: colors.textMuted, fontSize: 11, fontWeight: "700", textTransform: "uppercase", letterSpacing: 0.8, marginBottom: 8 }}>Categoría padre</Text>
                <TouchableOpacity
                  onPress={() => setParentPickerOpen((v) => !v)} activeOpacity={0.8}
                  style={{ flexDirection: "row", alignItems: "center", backgroundColor: colors.surface, borderRadius: 12, paddingHorizontal: 14, paddingVertical: 13, borderWidth: 1, borderColor: colors.primary + "30" }}
                >
                  <Text style={{ flex: 1, color: dadId ? colors.textPrimary : colors.textMuted, fontSize: 14 }}>
                    {dadId ? parentName(dadId) : "Ninguna (categoría principal)"}
                  </Text>
                  <Ionicons name={parentPickerOpen ? "chevron-up" : "chevron-down"} size={16} color={colors.textMuted} />
                </TouchableOpacity>

                {parentPickerOpen && (
                  <View style={{ marginTop: 6, backgroundColor: colors.surface, borderRadius: 12, borderWidth: 1, borderColor: colors.primary + "25", overflow: "hidden" }}>
                    <TouchableOpacity
                      onPress={() => { setDadId(null); setParentPickerOpen(false); }}
                      activeOpacity={0.75}
                      style={{ paddingHorizontal: 14, paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: colors.primary + "12" }}
                    >
                      <Text style={{ color: !dadId ? colors.accent : colors.textSecondary, fontSize: 13, fontWeight: !dadId ? "700" : "500" }}>
                        Ninguna (categoría principal)
                      </Text>
                    </TouchableOpacity>
                    {parentOptions.map((p, idx) => (
                      <TouchableOpacity
                        key={p.categoryId}
                        onPress={() => { setDadId(p.categoryId); setParentPickerOpen(false); }}
                        activeOpacity={0.75}
                        style={{ paddingHorizontal: 14, paddingVertical: 12, borderBottomWidth: idx === parentOptions.length - 1 ? 0 : 1, borderBottomColor: colors.primary + "12" }}
                      >
                        <Text style={{ color: dadId === p.categoryId ? colors.accent : colors.textSecondary, fontSize: 13, fontWeight: dadId === p.categoryId ? "700" : "500" }}>
                          {p.name}
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </View>
                )}
              </View>

              {/* Guardar */}
              <TouchableOpacity
                onPress={handleSave}
                disabled={!name.trim() || saving}
                activeOpacity={0.8}
                style={{ backgroundColor: colors.primary, borderRadius: 13, paddingVertical: 15, alignItems: "center", opacity: (!name.trim() || saving) ? 0.5 : 1, marginTop: 4 }}
              >
                {saving
                  ? <ActivityIndicator size="small" color={colors.textPrimary} />
                  : <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700" }}>{editing ? "Guardar cambios" : "Crear categoría"}</Text>
                }
              </TouchableOpacity>
            </ScrollView>
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
