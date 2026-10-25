import { View, Text, TouchableOpacity, Image, StyleSheet } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useTranslation } from "react-i18next";
import { useTheme } from "../context/ThemeContext";
import { useLanguage } from "../context/LanguageContext";
import { displayEventName, formatEventDate } from "../utils/eventFormat";

/**
 * Tarjeta de evento (equivalente a EventCard de la web).
 * Imagen 16:9 a todo el ancho para no recortar la portada, badges de
 * disponibilidad, chip de fecha y botón "Ver detalles".
 *
 * Props:
 *   event     — EventSummaryResponse (coverUrl ya resuelto con resolveCoverUrl)
 *   onPress   — callback al tocar la tarjeta
 *   width     — ancho fijo (carruseles); por defecto ocupa todo el ancho
 *   category  — nombre de la categoría ya traducido (opcional)
 */
export default function EventCard({ event, onPress, width, category }) {
  const { t } = useTranslation();
  const { language } = useLanguage();
  const { colors } = useTheme();
  const styles = createStyles(colors);

  const seats      = event.availableSeats ?? 0;
  const soldOut    = seats === 0;
  const almostGone = seats > 0 && seats <= 10;
  const dateLabel  = formatEventDate(event.startDate, event.finishDate, language, true);

  return (
    <TouchableOpacity
      activeOpacity={0.85}
      onPress={onPress}
      style={[styles.card, width ? { width } : null]}
    >
      {/* Imagen */}
      <View style={styles.imageWrap}>
        {event.coverUrl ? (
          <Image source={{ uri: event.coverUrl }} style={styles.image} resizeMode="cover" />
        ) : (
          <View style={[styles.image, styles.placeholder]}>
            <Ionicons name="musical-notes" size={40} color={colors.textMuted} />
          </View>
        )}
        <LinearGradient
          colors={["transparent", colors.background + "CC"]}
          style={StyleSheet.absoluteFill}
          pointerEvents="none"
        />

        {/* Badges de disponibilidad */}
        <View style={styles.badges}>
          {soldOut && (
            <View style={[styles.badge, { backgroundColor: colors.error }]}>
              <Text style={styles.badgeText}>{t("events.soldOut")}</Text>
            </View>
          )}
          {almostGone && (
            <View style={[styles.badge, { backgroundColor: "#F59E0B" }]}>
              <Text style={styles.badgeText}>{t("events.almostGone")}</Text>
            </View>
          )}
        </View>

        {/* Fecha flotante */}
        {dateLabel && (
          <View style={styles.dateChip}>
            <Ionicons name="calendar-outline" size={12} color="#FFFFFF" />
            <Text style={styles.dateChipText} numberOfLines={1}>{dateLabel}</Text>
          </View>
        )}

        {category ? (
          <View style={styles.categoryChip}>
            <Text style={styles.categoryChipText} numberOfLines={1}>{category}</Text>
          </View>
        ) : null}
      </View>

      {/* Cuerpo */}
      <View style={styles.body}>
        <Text style={styles.title} numberOfLines={2}>{displayEventName(event, language)}</Text>
        {event.siteName ? (
          <View style={styles.metaRow}>
            <Ionicons name="location-outline" size={14} color={colors.textSecondary} />
            <Text style={styles.metaText} numberOfLines={1}>{event.siteName}</Text>
          </View>
        ) : null}
        {seats > 0 && (
          <View style={styles.metaRow}>
            <Ionicons name="people-outline" size={14} color={colors.textSecondary} />
            <Text style={styles.metaText}>{t("events.availableSeats", { count: seats })}</Text>
          </View>
        )}
      </View>

      {/* Footer */}
      <View style={styles.footer}>
        <Text style={styles.footerText}>{t("events.viewDetails")}</Text>
        <Ionicons name="arrow-forward" size={14} color={colors.primary} />
      </View>
    </TouchableOpacity>
  );
}

function createStyles(colors) {
  return StyleSheet.create({
    card: {
      backgroundColor: colors.surface,
      borderRadius: 18,
      overflow: "hidden",
      borderWidth: 1,
      borderColor: colors.primary + "25",
    },
    imageWrap: {
      width: "100%",
      aspectRatio: 16 / 9,
      backgroundColor: colors.surfaceAlt,
    },
    image: {
      width: "100%",
      height: "100%",
    },
    placeholder: {
      alignItems: "center",
      justifyContent: "center",
    },
    badges: {
      position: "absolute",
      top: 10,
      left: 10,
      flexDirection: "row",
      gap: 6,
    },
    badge: {
      borderRadius: 8,
      paddingHorizontal: 8,
      paddingVertical: 3,
    },
    badgeText: {
      color: "#FFFFFF",
      fontSize: 10,
      fontWeight: "800",
      textTransform: "uppercase",
    },
    dateChip: {
      position: "absolute",
      top: 10,
      right: 10,
      flexDirection: "row",
      alignItems: "center",
      gap: 4,
      backgroundColor: "rgba(0,0,0,0.55)",
      borderRadius: 10,
      paddingHorizontal: 8,
      paddingVertical: 4,
      maxWidth: "60%",
    },
    dateChipText: {
      color: "#FFFFFF",
      fontSize: 11,
      fontWeight: "700",
    },
    categoryChip: {
      position: "absolute",
      bottom: 10,
      left: 10,
      backgroundColor: colors.primary + "E6",
      borderRadius: 10,
      paddingHorizontal: 10,
      paddingVertical: 3,
      maxWidth: "70%",
    },
    categoryChipText: {
      color: "#FFFFFF",
      fontSize: 11,
      fontWeight: "700",
    },
    body: {
      paddingHorizontal: 14,
      paddingTop: 12,
      gap: 6,
    },
    title: {
      color: colors.textPrimary,
      fontSize: 16,
      fontWeight: "800",
      lineHeight: 21,
    },
    metaRow: {
      flexDirection: "row",
      alignItems: "center",
      gap: 6,
    },
    metaText: {
      color: colors.textSecondary,
      fontSize: 13,
      flex: 1,
    },
    footer: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "flex-end",
      gap: 6,
      paddingHorizontal: 14,
      paddingVertical: 12,
    },
    footerText: {
      color: colors.primary,
      fontSize: 13,
      fontWeight: "700",
    },
  });
}
