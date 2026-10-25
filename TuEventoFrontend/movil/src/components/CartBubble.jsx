import { useState, useEffect, useRef } from "react";
import {
  View, Text, TouchableOpacity, Modal, ScrollView, Pressable, Animated, StyleSheet,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTranslation } from "react-i18next";
import { useTheme } from "../context/ThemeContext";

const formatPrice = (amount) => `$${Number(amount ?? 0).toLocaleString("es-CO")}`;

/** "m:ss" restante hasta `until`, o null si ya venció. */
function remaining(until, now) {
  const diffMs = new Date(until) - now;
  if (diffMs <= 0) return null;
  const minutes = Math.floor(diffMs / 60000);
  const seconds = Math.floor((diffMs / 1000) % 60);
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}

/** Fila de una silla reservada dentro del modal (solo muestra; el vencimiento lo vigila la burbuja). */
function CartItem({ seat, section, now, onRelease, isReleasing, colors, styles }) {
  const { t } = useTranslation();
  const left = seat.reservedUntil ? remaining(seat.reservedUntil, now) : null;
  return (
    <View style={styles.item}>
      <View style={styles.itemIcon}>
        <Ionicons name="ticket-outline" size={18} color={colors.primary} />
      </View>
      <View style={{ flex: 1 }}>
        <Text style={styles.itemCode}>{seat.code}</Text>
        <Text style={styles.itemSection}>{section?.sectionTypeName ?? t("seats.section")}</Text>
        {seat.reservedUntil ? (
          <View style={styles.itemTimer}>
            <Ionicons name="time-outline" size={12} color={colors.textMuted} />
            <Text style={styles.itemTimerText}>{left ?? t("seats.expiredCountdown")}</Text>
          </View>
        ) : null}
      </View>
      <Text style={styles.itemPrice}>{formatPrice(section?.price)}</Text>
      <TouchableOpacity
        onPress={onRelease}
        disabled={isReleasing}
        style={{ padding: 4, opacity: isReleasing ? 0.5 : 1 }}
        activeOpacity={0.7}
        hitSlop={6}
      >
        <Ionicons name="close-circle" size={22} color={isReleasing ? colors.textMuted : colors.error} />
      </TouchableOpacity>
    </View>
  );
}

/**
 * Carrito flotante: burbuja con el número de sillas y la cuenta regresiva de la
 * reserva más próxima a vencer; al tocarla abre un modal con el detalle, el total
 * y el botón de pago.
 *
 * La burbuja está siempre montada mientras haya sillas, así que es ella quien
 * detecta los vencimientos y llama a onExpire (antes lo hacía cada fila del carrito).
 *
 * Props:
 *   cart             — sillas reservadas por el usuario
 *   sections         — secciones del evento (precio y nombre)
 *   selectedQuantity — cantidad elegida en el selector; el pago se habilita al completarla
 *   reserving        — Set de seatId con una operación en curso
 *   onRelease(seatId), onExpire(seatId), onCheckout()
 */
export default function CartBubble({
  cart, sections, selectedQuantity, reserving, onRelease, onExpire, onCheckout,
}) {
  const { t } = useTranslation();
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();
  const styles = createStyles(colors);

  const [open, setOpen] = useState(false);
  const [now, setNow] = useState(() => new Date());
  const expiredRef = useRef(new Set());
  const pulse = useRef(new Animated.Value(1)).current;

  // Reloj de 1 s + vencimiento optimista de las reservas
  useEffect(() => {
    if (cart.length === 0) return undefined;
    const tick = () => {
      const current = new Date();
      setNow(current);
      cart.forEach((seat) => {
        if (seat.reservedUntil && new Date(seat.reservedUntil) <= current
            && !expiredRef.current.has(seat.seatId)) {
          expiredRef.current.add(seat.seatId);
          onExpire?.(seat.seatId);
        }
      });
    };
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [cart, onExpire]);

  // Una silla que se vuelve a reservar debe poder vencer de nuevo
  useEffect(() => {
    const ids = new Set(cart.map((s) => s.seatId));
    expiredRef.current.forEach((id) => { if (!ids.has(id)) expiredRef.current.delete(id); });
  }, [cart]);

  // Pulso al cambiar la cantidad de sillas
  useEffect(() => {
    Animated.sequence([
      Animated.timing(pulse, { toValue: 1.15, duration: 120, useNativeDriver: true }),
      Animated.spring(pulse, { toValue: 1, friction: 4, useNativeDriver: true }),
    ]).start();
  }, [cart.length]);

  // Si el carrito queda vacío, cerrar el modal
  useEffect(() => {
    if (cart.length === 0) setOpen(false);
  }, [cart.length]);

  if (cart.length === 0) return null;

  const sectionOf = (seat) => sections.find((s) => s.eventSectionId === seat.eventSectionId);
  const total = cart.reduce((sum, seat) => sum + (sectionOf(seat)?.price ?? 0), 0);
  const ready = cart.length >= selectedQuantity;
  const soonest = cart
    .map((s) => s.reservedUntil)
    .filter(Boolean)
    .sort((a, b) => new Date(a) - new Date(b))[0];
  const soonestLeft = soonest ? remaining(soonest, now) : null;

  return (
    <>
      {/* Burbuja */}
      <Animated.View
        style={[styles.bubbleWrap, { bottom: insets.bottom + 20, transform: [{ scale: pulse }] }]}
      >
        <TouchableOpacity onPress={() => setOpen(true)} activeOpacity={0.85} style={styles.bubble}>
          <Ionicons name="cart" size={26} color="#FFFFFF" />
          <View style={styles.countBadge}>
            <Text style={styles.countText}>{cart.length}</Text>
          </View>
        </TouchableOpacity>
        {soonestLeft ? (
          <View style={styles.timerPill}>
            <Ionicons name="time-outline" size={11} color="#FFFFFF" />
            <Text style={styles.timerPillText}>{soonestLeft}</Text>
          </View>
        ) : null}
      </Animated.View>

      {/* Modal del carrito */}
      <Modal visible={open} transparent animationType="slide" onRequestClose={() => setOpen(false)}>
        <Pressable style={styles.backdrop} onPress={() => setOpen(false)} />
        <View style={[styles.sheet, { paddingBottom: insets.bottom + 16 }]}>
          <View style={styles.handle} />
          <View style={styles.sheetHeader}>
            <View style={styles.sheetTitleRow}>
              <Ionicons name="cart-outline" size={20} color={colors.primary} />
              <Text style={styles.sheetTitle}>{t("seats.myCart")}</Text>
              <Text style={styles.sheetCount}>({cart.length})</Text>
            </View>
            <TouchableOpacity onPress={() => setOpen(false)} hitSlop={10}>
              <Ionicons name="close" size={24} color={colors.textSecondary} />
            </TouchableOpacity>
          </View>

          <Text style={styles.hint}>{t("mobileCart.reservationHint")}</Text>

          <ScrollView style={styles.list} contentContainerStyle={{ gap: 8 }}>
            {cart.map((seat) => (
              <CartItem
                key={seat.seatId}
                seat={seat}
                section={sectionOf(seat)}
                now={now}
                onRelease={() => onRelease(seat.seatId)}
                isReleasing={reserving.has(seat.seatId)}
                colors={colors}
                styles={styles}
              />
            ))}
          </ScrollView>

          <View style={styles.totalRow}>
            <Text style={styles.totalLabel}>{t("mobileCart.total")}</Text>
            <Text style={styles.totalValue}>{formatPrice(total)}</Text>
          </View>

          {!ready && (
            <Text style={styles.pending}>
              {t("mobileCart.selectMore", { count: selectedQuantity - cart.length })}
            </Text>
          )}

          <TouchableOpacity
            onPress={() => { setOpen(false); onCheckout(); }}
            disabled={!ready}
            activeOpacity={0.85}
            style={[styles.payButton, !ready && { opacity: 0.45 }]}
          >
            <Ionicons name="card-outline" size={20} color="#FFFFFF" />
            <Text style={styles.payText}>
              {t("mobileCart.pay", { count: cart.length })}
            </Text>
          </TouchableOpacity>
        </View>
      </Modal>
    </>
  );
}

function createStyles(colors) {
  return StyleSheet.create({
    bubbleWrap: {
      position: "absolute",
      right: 20,
      alignItems: "center",
    },
    bubble: {
      width: 62,
      height: 62,
      borderRadius: 31,
      backgroundColor: colors.primary,
      alignItems: "center",
      justifyContent: "center",
      shadowColor: colors.primary,
      shadowOffset: { width: 0, height: 6 },
      shadowOpacity: 0.45,
      shadowRadius: 10,
      elevation: 10,
      borderWidth: 2,
      borderColor: "rgba(255,255,255,0.25)",
    },
    countBadge: {
      position: "absolute",
      top: -2,
      right: -2,
      minWidth: 22,
      height: 22,
      borderRadius: 11,
      paddingHorizontal: 5,
      backgroundColor: colors.success,
      borderWidth: 2,
      borderColor: colors.background,
      alignItems: "center",
      justifyContent: "center",
    },
    countText: { color: "#FFFFFF", fontSize: 11, fontWeight: "800" },
    timerPill: {
      marginTop: 6,
      flexDirection: "row",
      alignItems: "center",
      gap: 3,
      backgroundColor: "rgba(0,0,0,0.65)",
      borderRadius: 10,
      paddingHorizontal: 7,
      paddingVertical: 2,
    },
    timerPillText: { color: "#FFFFFF", fontSize: 11, fontWeight: "700" },
    backdrop: {
      flex: 1,
      backgroundColor: "rgba(0,0,0,0.5)",
    },
    sheet: {
      backgroundColor: colors.background,
      borderTopLeftRadius: 24,
      borderTopRightRadius: 24,
      borderTopWidth: 1,
      borderColor: colors.primary + "40",
      paddingHorizontal: 20,
      paddingTop: 10,
      maxHeight: "75%",
    },
    handle: {
      alignSelf: "center",
      width: 44,
      height: 5,
      borderRadius: 3,
      backgroundColor: colors.textMuted + "80",
      marginBottom: 12,
    },
    sheetHeader: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
    },
    sheetTitleRow: { flexDirection: "row", alignItems: "center", gap: 8 },
    sheetTitle: { color: colors.textPrimary, fontSize: 18, fontWeight: "800" },
    sheetCount: { color: colors.textSecondary, fontSize: 14, fontWeight: "600" },
    hint: { color: colors.textMuted, fontSize: 12, marginTop: 6, marginBottom: 12 },
    list: { flexGrow: 0 },
    item: {
      flexDirection: "row",
      alignItems: "center",
      gap: 12,
      padding: 12,
      backgroundColor: colors.surface,
      borderRadius: 12,
      borderWidth: 1,
      borderColor: colors.primary + "25",
    },
    itemIcon: {
      width: 36,
      height: 36,
      borderRadius: 10,
      backgroundColor: colors.primary + "20",
      alignItems: "center",
      justifyContent: "center",
    },
    itemCode: { color: colors.textPrimary, fontSize: 14, fontWeight: "800" },
    itemSection: { color: colors.textSecondary, fontSize: 12, marginTop: 1 },
    itemTimer: { flexDirection: "row", alignItems: "center", gap: 4, marginTop: 3 },
    itemTimerText: { color: colors.textMuted, fontSize: 11 },
    itemPrice: { color: colors.primary, fontSize: 14, fontWeight: "800" },
    totalRow: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      marginTop: 16,
      paddingTop: 14,
      borderTopWidth: 1,
      borderTopColor: colors.primary + "25",
    },
    totalLabel: { color: colors.textSecondary, fontSize: 15, fontWeight: "700" },
    totalValue: { color: colors.textPrimary, fontSize: 22, fontWeight: "800" },
    pending: { color: "#F59E0B", fontSize: 12, marginTop: 8, textAlign: "center" },
    payButton: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "center",
      gap: 8,
      backgroundColor: colors.success,
      borderRadius: 14,
      paddingVertical: 15,
      marginTop: 14,
    },
    payText: { color: "#FFFFFF", fontSize: 16, fontWeight: "800" },
  });
}
