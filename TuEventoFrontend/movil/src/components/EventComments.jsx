import { useState, useEffect, useMemo, useCallback } from "react";
import {
  View, Text, TextInput, TouchableOpacity, ActivityIndicator, Alert, StyleSheet,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { useTranslation } from "react-i18next";
import { useTheme } from "../context/ThemeContext";
import { useAuth } from "../context/AuthContext";
import { useLanguage } from "../context/LanguageContext";
import {
  getEventComments, addEventComment, editEventComment, deleteEventComment,
} from "../services/commentService";
import { localeFor } from "../utils/eventFormat";

const MAX_LENGTH = 500;

/** Clave i18n (mobileComments.errors.*) para un error del backend o de red. */
function errorKey(err) {
  const code = err?.code || "";
  const msg = err?.message || "";
  if (msg.includes("SESSION_EXPIRED") || msg.includes("401")) return "sessionExpired";
  if (code === "COMMENT_RATE_LIMITED" || msg.includes("COMMENT_RATE_LIMITED")) return "rateLimited";
  if (code === "COMMENT_RATING_REQUIRED" || code === "COMMENT_RATING_INVALID") return "ratingRequired";
  if (code === "COMMENT_PARENT_INVALID") return "parentInvalid";
  if (code === "COMMENT_EDIT_WINDOW_EXPIRED") return "editExpired";
  if (msg.includes("403")) return "forbidden";
  return "generic";
}

/** Estrellas de solo lectura o seleccionables (onChange). */
function Stars({ value, size = 14, onChange, color }) {
  return (
    <View style={{ flexDirection: "row", gap: onChange ? 6 : 2 }}>
      {[1, 2, 3, 4, 5].map((n) => {
        const icon = <Ionicons name={n <= value ? "star" : "star-outline"} size={size} color={color} />;
        return onChange ? (
          <TouchableOpacity key={n} onPress={() => onChange(n)} hitSlop={6}>{icon}</TouchableOpacity>
        ) : (
          <View key={n}>{icon}</View>
        );
      })}
    </View>
  );
}

/**
 * Comentarios, calificaciones y respuestas de un evento (equivalente a la sección
 * de comentarios de EventDetail en la web).
 *
 * Props:
 *   eventId          — id del evento
 *   organizerUserId  — userId del organizador (no lleva calificación)
 */
/**
 * @param {Function} [props.onInputFocus] recibe el campo de texto que obtuvo el foco, para que la
 *   pantalla lo deje a la vista por encima del teclado
 */
export default function EventComments({ eventId, organizerUserId, onInputFocus }) {
  const { t } = useTranslation();
  const { colors } = useTheme();
  const { user } = useAuth();
  const { language } = useLanguage();
  const navigation = useNavigation();
  const styles = createStyles(colors);

  const currentUserId = user?.userId ?? null;
  const isOrganizer = currentUserId != null && Number(currentUserId) === Number(organizerUserId);
  // Igual que la web: solo se publica con la interfaz en español (las traducciones parten de ahí)
  const canWrite = currentUserId != null && language === "es";

  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);

  const [text, setText] = useState("");
  const [rating, setRating] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);

  // Respuesta en curso: { parentId, toUserId, toUserName }
  const [replyTarget, setReplyTarget] = useState(null);
  const [replyText, setReplyText] = useState("");
  const [replyError, setReplyError] = useState(null);
  const [replySubmitting, setReplySubmitting] = useState(false);

  const [editingId, setEditingId] = useState(null);
  const [editText, setEditText] = useState("");
  const [editError, setEditError] = useState(null);
  const [editSubmitting, setEditSubmitting] = useState(false);

  const loadComments = useCallback(async () => {
    try {
      setComments(await getEventComments(eventId, language));
    } catch (err) {
      console.warn("[EventComments] Error loading comments:", err?.message);
    } finally {
      setLoading(false);
    }
  }, [eventId, language]);

  useEffect(() => {
    setLoading(true);
    void loadComments();
  }, [loadComments]);

  const roots = useMemo(
    () => comments
      .filter((c) => c.parentRatingId == null)
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)),
    [comments]
  );

  const repliesOf = (rootId) => comments
    .filter((c) => c.parentRatingId === rootId)
    .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));

  const average = useMemo(() => {
    const rated = roots.filter((c) => c.rating != null);
    if (rated.length === 0) return null;
    return { value: rated.reduce((s, c) => s + c.rating, 0) / rated.length, count: rated.length };
  }, [roots]);

  // R3/R4: solo la primera reseña principal lleva calificación, y nunca la del organizador
  const needsRating = !isOrganizer && !roots.some(
    (c) => Number(c.userId) === Number(currentUserId) && c.rating != null
  );

  const canEdit = (c) =>
    Number(c.userId) === Number(currentUserId) && !c._editExpired &&
    !!c.editableUntil && new Date(c.editableUntil).getTime() > Date.now();

  const formatWhen = (value) => {
    if (!value) return "";
    return new Date(value).toLocaleString(localeFor(language), {
      day: "numeric", month: "short", hour: "2-digit", minute: "2-digit",
    });
  };

  const commentText = (c) => (c.translatedComment && c.languageCode ? c.translatedComment : c.comment);

  // ── Publicar comentario principal ──
  const submitComment = async () => {
    const trimmed = text.trim();
    if (!trimmed || submitting) return;
    if (needsRating && rating === 0) {
      setFormError(t("mobileComments.errors.ratingRequired"));
      return;
    }
    setSubmitting(true);
    setFormError(null);
    try {
      const saved = await addEventComment(eventId, {
        comment: trimmed,
        ...(needsRating ? { rating } : {}),
      });
      setComments((prev) => (prev.some((c) => c.ratingId === saved.ratingId) ? prev : [saved, ...prev]));
      setText("");
      setRating(0);
    } catch (err) {
      setFormError(t(`mobileComments.errors.${errorKey(err)}`));
    } finally {
      setSubmitting(false);
    }
  };

  // ── Responder (un solo nivel: siempre cuelga del comentario raíz) ──
  const startReply = (root, target) => {
    setEditingId(null);
    setReplyError(null);
    setReplyText("");
    setReplyTarget({ parentId: root.ratingId, toUserId: target.userId, toUserName: target.authorName });
  };

  const submitReply = async () => {
    const trimmed = replyText.trim();
    if (!trimmed || !replyTarget || replySubmitting) return;
    setReplySubmitting(true);
    setReplyError(null);
    try {
      const payload = { comment: trimmed, parentRatingId: replyTarget.parentId };
      if (replyTarget.toUserId && Number(replyTarget.toUserId) !== Number(currentUserId)) {
        payload.replyToUserId = replyTarget.toUserId;
      }
      const saved = await addEventComment(eventId, payload);
      setComments((prev) => (prev.some((c) => c.ratingId === saved.ratingId) ? prev : [...prev, saved]));
      setReplyTarget(null);
      setReplyText("");
    } catch (err) {
      setReplyError(t(`mobileComments.errors.${errorKey(err)}`));
    } finally {
      setReplySubmitting(false);
    }
  };

  // ── Editar (solo texto, dentro de la ventana de 2 h) ──
  const startEdit = (c) => {
    setReplyTarget(null);
    setEditError(null);
    setEditingId(c.ratingId);
    setEditText(c.comment);
  };

  const submitEdit = async (ratingId) => {
    const trimmed = editText.trim();
    if (!trimmed || editSubmitting) return;
    setEditSubmitting(true);
    setEditError(null);
    try {
      const updated = await editEventComment(eventId, ratingId, trimmed);
      setComments((prev) => prev.map((c) => (c.ratingId === ratingId
        ? { ...c, comment: updated.comment, editedAt: updated.editedAt, translatedComment: null }
        : c)));
      setEditingId(null);
    } catch (err) {
      const key = errorKey(err);
      if (key === "editExpired") {
        setComments((prev) => prev.map((c) => (c.ratingId === ratingId ? { ...c, _editExpired: true } : c)));
        setEditingId(null);
      }
      setEditError(t(`mobileComments.errors.${key}`));
    } finally {
      setEditSubmitting(false);
    }
  };

  // ── Borrar (al borrar un principal se van también sus respuestas) ──
  const confirmDelete = (c) => {
    Alert.alert(t("eventDetail.deleteCommentTitle"), t("eventDetail.deleteCommentMessage"), [
      { text: t("eventDetail.noCancel"), style: "cancel" },
      {
        text: t("eventDetail.yesDelete"),
        style: "destructive",
        onPress: async () => {
          try {
            await deleteEventComment(eventId, c.ratingId);
            setComments((prev) => prev.filter(
              (x) => x.ratingId !== c.ratingId && x.parentRatingId !== c.ratingId
            ));
          } catch (err) {
            Alert.alert(t("common.error"), t(`mobileComments.errors.${errorKey(err)}`));
          }
        },
      },
    ]);
  };

  const renderComposer = ({ value, onChange, onSubmit, busy, placeholder, error, onCancel }) => (
    <View>
      <View style={styles.composer}>
        <TextInput
          value={value}
          onChangeText={onChange}
          placeholder={placeholder}
          placeholderTextColor={colors.textMuted}
          style={styles.composerInput}
          multiline
          maxLength={MAX_LENGTH}
          onFocus={(e) => onInputFocus?.(e.currentTarget)}
        />
        <TouchableOpacity
          onPress={onSubmit}
          disabled={busy || !value.trim()}
          style={[styles.sendButton, (busy || !value.trim()) && { opacity: 0.4 }]}
          activeOpacity={0.75}
        >
          {busy ? <ActivityIndicator size="small" color="#FFFFFF" /> : <Ionicons name="send" size={16} color="#FFFFFF" />}
        </TouchableOpacity>
      </View>
      <View style={styles.composerFooter}>
        <Text style={styles.counter}>{value.length}/{MAX_LENGTH}</Text>
        {onCancel ? (
          <TouchableOpacity onPress={onCancel} hitSlop={8}>
            <Text style={styles.cancelText}>{t("comments.cancel")}</Text>
          </TouchableOpacity>
        ) : null}
      </View>
      {error ? <Text style={styles.errorText}>{error}</Text> : null}
    </View>
  );

  const renderComment = (c, root, isReply = false) => {
    const mine = Number(c.userId) === Number(currentUserId);
    return (
      <View key={c.ratingId} style={[styles.comment, isReply && styles.reply]}>
        <View style={styles.commentHeader}>
          <View style={[styles.avatar, c.isOrganizer && { backgroundColor: colors.accent }]}>
            <Text style={styles.avatarText}>{(c.authorName || "?").charAt(0).toUpperCase()}</Text>
          </View>
          <View style={{ flex: 1 }}>
            <View style={styles.nameRow}>
              <Text style={styles.author} numberOfLines={1}>
                {mine ? t("eventDetail.you") : c.authorName}
              </Text>
              {c.isOrganizer && (
                <View style={styles.organizerBadge}>
                  <Text style={styles.organizerText}>{t("comments.organizer")}</Text>
                </View>
              )}
            </View>
            <Text style={styles.when}>
              {formatWhen(c.createdAt)}{c.editedAt ? ` · ${t("eventDetail.edited")}` : ""}
            </Text>
          </View>
          {c.rating != null && <Stars value={c.rating} color="#FBBF24" />}
        </View>

        {editingId === c.ratingId ? (
          renderComposer({
            value: editText,
            onChange: setEditText,
            onSubmit: () => submitEdit(c.ratingId),
            busy: editSubmitting,
            placeholder: t("comments.writeComment"),
            error: editError,
            onCancel: () => setEditingId(null),
          })
        ) : (
          <>
            <Text style={styles.body}>
              {c.replyToUserName ? <Text style={styles.mention}>@{c.replyToUserName} </Text> : null}
              {commentText(c)}
            </Text>
            {c.translatedComment && c.languageCode ? (
              <Text style={styles.translated}>{t("comments.translated")}</Text>
            ) : c.translationGenerating && language !== "es" ? (
              <Text style={styles.translated}>{t("comments.translating")}</Text>
            ) : null}
          </>
        )}

        {editingId !== c.ratingId && (
          <View style={styles.actions}>
            {canWrite && (
              <TouchableOpacity onPress={() => startReply(root, c)} style={styles.action} hitSlop={6}>
                <Ionicons name="return-down-forward-outline" size={14} color={colors.accent} />
                <Text style={styles.actionText}>{t("comments.reply")}</Text>
              </TouchableOpacity>
            )}
            {canEdit(c) && (
              <TouchableOpacity onPress={() => startEdit(c)} style={styles.action} hitSlop={6}>
                <Ionicons name="create-outline" size={14} color={colors.textSecondary} />
                <Text style={[styles.actionText, { color: colors.textSecondary }]}>{t("comments.edit")}</Text>
              </TouchableOpacity>
            )}
            {mine && (
              <TouchableOpacity onPress={() => confirmDelete(c)} style={styles.action} hitSlop={6}>
                <Ionicons name="trash-outline" size={14} color={colors.error} />
                <Text style={[styles.actionText, { color: colors.error }]}>{t("comments.delete")}</Text>
              </TouchableOpacity>
            )}
          </View>
        )}
      </View>
    );
  };

  return (
    <View style={styles.section}>
      {/* Encabezado con promedio */}
      <View style={styles.headerRow}>
        <Text style={styles.title}>
          {t("eventDetail.commentsCount")} ({comments.length})
        </Text>
        {average && (
          <View style={styles.averageBox}>
            <Ionicons name="star" size={14} color="#FBBF24" />
            <Text style={styles.averageText}>{average.value.toFixed(1)}</Text>
            <Text style={styles.averageCount}>({average.count})</Text>
          </View>
        )}
      </View>

      {/* Formulario */}
      {currentUserId == null ? (
        <TouchableOpacity onPress={() => navigation.navigate("Login")} activeOpacity={0.75} style={styles.notice}>
          <Ionicons name="log-in-outline" size={18} color={colors.accent} />
          <Text style={styles.noticeText}>
            <Text style={{ color: colors.accent, fontWeight: "700" }}>{t("mobileComments.login")}</Text>
            {" "}{t("eventDetail.loginToComment")}
          </Text>
        </TouchableOpacity>
      ) : !canWrite ? (
        <TouchableOpacity onPress={() => navigation.navigate("EditProfile")} activeOpacity={0.75} style={[styles.notice, styles.warning]}>
          <Ionicons name="alert-circle-outline" size={18} color="#F59E0B" />
          <Text style={[styles.noticeText, { color: "#F59E0B" }]}>{t("comments.languageRestriction")}</Text>
        </TouchableOpacity>
      ) : (
        <View style={styles.form}>
          {needsRating && (
            <View style={styles.ratingRow}>
              <Text style={styles.ratingLabel}>{t("mobileComments.yourRating")}</Text>
              <Stars value={rating} size={24} onChange={setRating} color="#FBBF24" />
            </View>
          )}
          {renderComposer({
            value: text,
            onChange: setText,
            onSubmit: submitComment,
            busy: submitting,
            placeholder: t("eventDetail.writeComment"),
            error: formError,
          })}
        </View>
      )}

      {/* Lista */}
      {loading ? (
        <View style={styles.loadingRow}>
          <ActivityIndicator size="small" color={colors.primary} />
          <Text style={styles.muted}>{t("eventDetail.loadingComments")}</Text>
        </View>
      ) : roots.length === 0 ? (
        <Text style={[styles.muted, { textAlign: "center", paddingVertical: 16 }]}>
          {t("eventDetail.noCommentsYet")}
        </Text>
      ) : (
        roots.map((root) => (
          <View key={root.ratingId} style={styles.thread}>
            {renderComment(root, root)}
            {repliesOf(root.ratingId).map((r) => renderComment(r, root, true))}
            {replyTarget?.parentId === root.ratingId && (
              <View style={styles.replyComposer}>
                <Text style={styles.replyingTo}>
                  {t("comments.replyTo")} <Text style={styles.mention}>@{replyTarget.toUserName}</Text>
                </Text>
                {renderComposer({
                  value: replyText,
                  onChange: setReplyText,
                  onSubmit: submitReply,
                  busy: replySubmitting,
                  placeholder: t("comments.writeComment"),
                  error: replyError,
                  onCancel: () => setReplyTarget(null),
                })}
              </View>
            )}
          </View>
        ))
      )}
    </View>
  );
}

function createStyles(colors) {
  return StyleSheet.create({
    section: { marginTop: 28 },
    headerRow: {
      flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginBottom: 14,
    },
    title: { color: colors.textPrimary, fontSize: 18, fontWeight: "800" },
    averageBox: {
      flexDirection: "row", alignItems: "center", gap: 4,
      backgroundColor: colors.surface, borderRadius: 12, paddingHorizontal: 10, paddingVertical: 4,
    },
    averageText: { color: colors.textPrimary, fontSize: 14, fontWeight: "800" },
    averageCount: { color: colors.textMuted, fontSize: 12 },
    notice: {
      flexDirection: "row", alignItems: "center", gap: 10, padding: 14, marginBottom: 16,
      borderRadius: 12, borderWidth: 1, borderColor: colors.primary + "30", backgroundColor: colors.surface + "AA",
    },
    warning: { borderColor: "#F59E0B55", backgroundColor: "#F59E0B14" },
    noticeText: { flex: 1, color: colors.textSecondary, fontSize: 13, lineHeight: 19 },
    form: {
      backgroundColor: colors.primary + "14", borderRadius: 14, borderWidth: 1,
      borderColor: colors.primary + "30", padding: 12, marginBottom: 18, gap: 10,
    },
    ratingRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
    ratingLabel: { color: colors.textSecondary, fontSize: 13, fontWeight: "600" },
    composer: { flexDirection: "row", alignItems: "flex-end", gap: 8 },
    composerInput: {
      flex: 1, minHeight: 44, maxHeight: 120, color: colors.textPrimary, fontSize: 14,
      backgroundColor: colors.background + "AA", borderRadius: 10, borderWidth: 1,
      borderColor: colors.primary + "30", paddingHorizontal: 12, paddingVertical: 10,
      textAlignVertical: "top",
    },
    sendButton: {
      width: 44, height: 44, borderRadius: 10, backgroundColor: colors.primary,
      alignItems: "center", justifyContent: "center",
    },
    composerFooter: { flexDirection: "row", justifyContent: "space-between", marginTop: 4 },
    counter: { color: colors.textMuted, fontSize: 11 },
    cancelText: { color: colors.textSecondary, fontSize: 12, fontWeight: "700" },
    errorText: { color: colors.error, fontSize: 12, marginTop: 4 },
    loadingRow: { flexDirection: "row", alignItems: "center", gap: 8, paddingVertical: 16, justifyContent: "center" },
    muted: { color: colors.textMuted, fontSize: 13 },
    thread: {
      backgroundColor: colors.surface + "CC", borderRadius: 14, borderWidth: 1,
      borderColor: colors.primary + "20", padding: 12, marginBottom: 12,
    },
    comment: { gap: 6 },
    reply: {
      marginTop: 12, marginLeft: 14, paddingLeft: 12, borderLeftWidth: 2, borderLeftColor: colors.primary + "40",
    },
    commentHeader: { flexDirection: "row", alignItems: "center", gap: 10 },
    avatar: {
      width: 32, height: 32, borderRadius: 16, backgroundColor: colors.primary,
      alignItems: "center", justifyContent: "center",
    },
    avatarText: { color: "#FFFFFF", fontSize: 14, fontWeight: "800" },
    nameRow: { flexDirection: "row", alignItems: "center", gap: 6 },
    author: { color: colors.textPrimary, fontSize: 14, fontWeight: "700", flexShrink: 1 },
    organizerBadge: { backgroundColor: colors.accent + "30", borderRadius: 6, paddingHorizontal: 6, paddingVertical: 1 },
    organizerText: { color: colors.accent, fontSize: 10, fontWeight: "800" },
    when: { color: colors.textMuted, fontSize: 11, marginTop: 1 },
    body: { color: colors.textSecondary, fontSize: 14, lineHeight: 20 },
    mention: { color: colors.accent, fontWeight: "700" },
    translated: { color: colors.textMuted, fontSize: 11, fontStyle: "italic" },
    actions: { flexDirection: "row", gap: 16, marginTop: 2 },
    action: { flexDirection: "row", alignItems: "center", gap: 4 },
    actionText: { color: colors.accent, fontSize: 12, fontWeight: "700" },
    replyComposer: { marginTop: 12, marginLeft: 14, gap: 6 },
    replyingTo: { color: colors.textMuted, fontSize: 12 },
  });
}
