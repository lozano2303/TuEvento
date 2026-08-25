import { useCallback, useMemo } from "react";
import { View, StyleSheet, Text, TouchableOpacity } from "react-native";
import {
  Canvas,
  Path,
  Circle,
  Text as SkiaText,
  Group,
  rect,
  Skia,
} from "@shopify/react-native-skia";
import { Gesture, GestureDetector } from "react-native-gesture-handler";
import {
  useSharedValue,
  useDerivedValue,
  withTiming,
  runOnJS,
  Easing,
} from "react-native-reanimated";
import {
  migrateElement,
  distributeSeats,
  computeTotalAABB,
  getElementAABB,
  flattenPolygonForFill,
} from "../utils/layoutUtils";

// ─── Constantes ───────────────────────────────────────────────────────────────

const PADDING         = 40;    // padding del overview completo
const SECTION_PADDING = 60;    // padding extra al enfocar una sección
const ZOOM_MAX_FACTOR = 4.0;   // zoom máximo = 4× el encuadre inicial de la sección
const PAN_MARGIN      = 40;    // píxeles mínimos de sección visibles al panear

const ANIM_DURATION = 380;
const EASE_IN_OUT   = Easing.inOut(Easing.quad);

// ─── Helpers de encuadre (JS — solo en render) ────────────────────────────────

function overviewCamera(elements, cW, cH) {
  const aabb  = computeTotalAABB(elements);
  const cntW  = aabb.maxX - aabb.minX;
  const cntH  = aabb.maxY - aabb.minY;
  const scale = Math.min((cW - PADDING * 2) / cntW, (cH - PADDING * 2) / cntH, 1);
  const tx    = (cW - cntW * scale) / 2 - aabb.minX * scale;
  const ty    = (cH - cntH * scale) / 2 - aabb.minY * scale;
  return { scale, tx, ty };
}

function sectionCamera(el, cW, cH) {
  const aabb  = getElementAABB(el);
  const cntW  = aabb.maxX - aabb.minX;
  const cntH  = aabb.maxY - aabb.minY;
  const scale = Math.min((cW - SECTION_PADDING * 2) / cntW, (cH - SECTION_PADDING * 2) / cntH);
  const tx    = (cW - cntW * scale) / 2 - aabb.minX * scale;
  const ty    = (cH - cntH * scale) / 2 - aabb.minY * scale;
  return { scale, tx, ty };
}

// ─── SeatMapCanvas ────────────────────────────────────────────────────────────

/**
 * Canvas de Skia con:
 *   - Overview: todas las secciones, sin gestos.
 *   - Tap en sección → anima cámara a encuadre de esa sección.
 *   - Una vez enfocada: pan 1 dedo + pinch 2 dedos dentro de la sección.
 *   - Botón "← Ver todas" para volver al overview.
 *
 * Props:
 *   layoutData       { canvasWidth, canvasHeight, elements }
 *   containerWidth   ancho del contenedor (onLayout)
 *   containerHeight  alto del contenedor (onLayout)
 *   selectedSection  id de la sección enfocada | null
 *   onSectionTap     (sectionId: string) => void
 *   onBackToOverview () => void
 */
export default function SeatMapCanvas({
  layoutData,
  containerWidth,
  containerHeight,
  selectedSection,
  onSectionTap,
  onBackToOverview,
}) {
  if (!layoutData?.elements?.length || containerWidth <= 0 || containerHeight <= 0) {
    return null;
  }

  // ── Datos del layout ──────────────────────────────────────────────────────
  const elements = useMemo(
    () => layoutData.elements.map(migrateElement),
    [layoutData.elements],
  );

  const focusedEl = useMemo(
    () => (selectedSection ? (elements.find((e) => e.id === selectedSection) ?? null) : null),
    [selectedSection, elements],
  );

  // ── Cámara base para el estado actual ────────────────────────────────────
  const baseCamera = useMemo(() => {
    if (focusedEl) return sectionCamera(focusedEl, containerWidth, containerHeight);
    return overviewCamera(elements, containerWidth, containerHeight);
  }, [focusedEl, containerWidth, containerHeight, elements]);

  // ── SharedValues de la cámara ─────────────────────────────────────────────
  const camScale = useSharedValue(baseCamera.scale);
  const camTX    = useSharedValue(baseCamera.tx);
  const camTY    = useSharedValue(baseCamera.ty);

  // Límites de zoom — en SharedValues para que los worklets los lean correctamente
  // (los objetos JS no se capturan de forma segura desde worklets)
  const baseTX      = useSharedValue(baseCamera.tx);
  const baseTY      = useSharedValue(baseCamera.ty);
  const baseScale   = useSharedValue(baseCamera.scale);
  const cW          = useSharedValue(containerWidth);
  const cH          = useSharedValue(containerHeight);

  // Actualizar los SharedValues de base cuando cambia el encuadre
  // (no useEffect — comparamos imperativo en render para no agregar dependencias)
  if (baseTX.value !== baseCamera.tx)    baseTX.value    = baseCamera.tx;
  if (baseTY.value !== baseCamera.ty)    baseTY.value    = baseCamera.ty;
  if (baseScale.value !== baseCamera.scale) baseScale.value = baseCamera.scale;
  if (cW.value !== containerWidth)       cW.value        = containerWidth;
  if (cH.value !== containerHeight)      cH.value        = containerHeight;

  // Snapshots al inicio de cada gesto
  const scaleSnap = useSharedValue(baseCamera.scale);
  const txSnap    = useSharedValue(baseCamera.tx);
  const tySnap    = useSharedValue(baseCamera.ty);

  // Focal del pinch
  const focalX = useSharedValue(0);
  const focalY = useSharedValue(0);

  // ── Bounding box de la sección enfocada (en SharedValues para worklets) ──
  // Extraemos los 4 valores del AABB ahora que estamos en render (hilo JS)
  const focusedAABB = focusedEl ? getElementAABB(focusedEl) : null;
  const aabbMinX = useSharedValue(focusedAABB?.minX ?? 0);
  const aabbMinY = useSharedValue(focusedAABB?.minY ?? 0);
  const aabbMaxX = useSharedValue(focusedAABB?.maxX ?? 0);
  const aabbMaxY = useSharedValue(focusedAABB?.maxY ?? 0);
  const hasFocus  = useSharedValue(!!focusedEl);

  // Sincronizar cuando cambia la sección
  if (hasFocus.value !== !!focusedEl)         hasFocus.value  = !!focusedEl;
  if (focusedAABB && aabbMinX.value !== focusedAABB.minX) aabbMinX.value = focusedAABB.minX;
  if (focusedAABB && aabbMinY.value !== focusedAABB.minY) aabbMinY.value = focusedAABB.minY;
  if (focusedAABB && aabbMaxX.value !== focusedAABB.maxX) aabbMaxX.value = focusedAABB.maxX;
  if (focusedAABB && aabbMaxY.value !== focusedAABB.maxY) aabbMaxY.value = focusedAABB.maxY;

  // ── Animación al cambiar de sección ──────────────────────────────────────
  // Detección imperativa de cambio de sección (evita useEffect)
  const prevSectionId = useSharedValue(selectedSection ?? "__none__");
  const currentId     = selectedSection ?? "__none__";
  if (prevSectionId.value !== currentId) {
    prevSectionId.value = currentId;
    const timing = { duration: ANIM_DURATION, easing: EASE_IN_OUT };
    camScale.value = withTiming(baseCamera.scale, timing);
    camTX.value    = withTiming(baseCamera.tx,    timing);
    camTY.value    = withTiming(baseCamera.ty,    timing);
  }

  // ── Clamp de pan — worklet ────────────────────────────────────────────────
  // Corre en el hilo UI. Lee SharedValues (no closures de JS).
  const clampPan = (tx, ty, scale) => {
    "worklet";
    if (!hasFocus.value) return { tx, ty };

    const sL = aabbMinX.value * scale + tx;
    const sT = aabbMinY.value * scale + ty;
    const sR = aabbMaxX.value * scale + tx;
    const sB = aabbMaxY.value * scale + ty;
    const W  = cW.value;
    const H  = cH.value;

    let cx = tx, cy = ty;
    if (sR < PAN_MARGIN)              cx += PAN_MARGIN - sR;
    if (sL > W - PAN_MARGIN)          cx -= sL - (W - PAN_MARGIN);
    if (sB < PAN_MARGIN)              cy += PAN_MARGIN - sB;
    if (sT > H - PAN_MARGIN)          cy -= sT - (H - PAN_MARGIN);

    return { tx: cx, ty: cy };
  };

  // ── Gestos ────────────────────────────────────────────────────────────────

  const panGesture = Gesture.Pan()
    .enabled(!!focusedEl)
    .onBegin(() => {
      "worklet";
      txSnap.value = camTX.value;
      tySnap.value = camTY.value;
    })
    .onUpdate((e) => {
      "worklet";
      const raw = clampPan(
        txSnap.value + e.translationX,
        tySnap.value + e.translationY,
        camScale.value,
      );
      camTX.value = raw.tx;
      camTY.value = raw.ty;
    });

  const pinchGesture = Gesture.Pinch()
    .enabled(!!focusedEl)
    .onBegin((e) => {
      "worklet";
      scaleSnap.value = camScale.value;
      txSnap.value    = camTX.value;
      tySnap.value    = camTY.value;
      focalX.value    = e.focalX;
      focalY.value    = e.focalY;
    })
    .onUpdate((e) => {
      "worklet";
      const minS   = baseScale.value;                       // no alejarse más que el encuadre
      const maxS   = baseScale.value * ZOOM_MAX_FACTOR;     // tope de 4×
      const newS   = Math.min(Math.max(scaleSnap.value * e.scale, minS), maxS);
      const factor = newS / scaleSnap.value;
      const rawTX  = focalX.value - (focalX.value - txSnap.value) * factor;
      const rawTY  = focalY.value - (focalY.value - tySnap.value) * factor;
      const c      = clampPan(rawTX, rawTY, newS);
      camScale.value = newS;
      camTX.value    = c.tx;
      camTY.value    = c.ty;
    });

  // Tap: solo en overview, dispara selección de sección desde hilo JS
  const handleTap = useCallback(
    (screenX, screenY) => {
      const scale = camScale.value;
      const tx    = camTX.value;
      const ty    = camTY.value;
      const lx    = (screenX - tx) / scale;
      const ly    = (screenY - ty) / scale;
      for (const el of elements) {
        const aabb = getElementAABB(el);
        if (lx >= aabb.minX && lx <= aabb.maxX && ly >= aabb.minY && ly <= aabb.maxY) {
          onSectionTap?.(el.id);
          return;
        }
      }
    },
    [elements, onSectionTap, camScale, camTX, camTY],
  );

  const tapGesture = Gesture.Tap()
    .enabled(!focusedEl)
    .maxDuration(250)
    .onEnd((e) => {
      "worklet";
      runOnJS(handleTap)(e.x, e.y);
    });

  // Tap compite con Pan+Pinch (Race): si hay sección activa, Pan+Pinch ganan
  const composed = Gesture.Race(
    tapGesture,
    Gesture.Simultaneous(panGesture, pinchGesture),
  );

  // ── Transform de cámara para el Group de Skia ────────────────────────────
  // useDerivedValue retorna un SharedValue<Transforms3d> que Skia detecta
  // via _isReanimatedSharedValue y suscribe en el hilo UI.
  // El order importa: translate ANTES de scale para que el origen sea correcto.
  const cameraTransform = useDerivedValue(() => [
    { translateX: camTX.value    },
    { translateY: camTY.value    },
    { scale:      camScale.value },
  ]);

  return (
    <View style={styles.root}>
      <GestureDetector gesture={composed}>
        {/* View con collapsable=false para que RNGH detecte el área táctil correctamente */}
        <View
          style={{ width: containerWidth, height: containerHeight }}
          collapsable={false}
        >
          <Canvas style={{ width: containerWidth, height: containerHeight }}>
            {/* Group aplica la cámara animada a todo el contenido del layout */}
            <Group transform={cameraTransform}>
              {elements.map((el) => (
                <SectionRenderer
                  key={el.id}
                  element={el}
                  isFocused={el.id === selectedSection}
                  hasSelection={!!selectedSection}
                />
              ))}
            </Group>
          </Canvas>
        </View>
      </GestureDetector>

      {/* Botón "Ver todas" — solo visible cuando hay sección enfocada */}
      {selectedSection && (
        <TouchableOpacity
          style={styles.overviewBtn}
          onPress={onBackToOverview}
          activeOpacity={0.8}
        >
          <Text style={styles.overviewBtnText}>← Ver todas</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

// ─── SectionRenderer ─────────────────────────────────────────────────────────
// Renderiza una sección en coordenadas absolutas del layout (sin escalar).
// La transformación la aplica el Group padre con la cámara.

function SectionRenderer({ element, isFocused, hasSelection }) {
  const color     = element.color    ?? "#3B82F6";
  const label     = element.label    ?? "";
  const shapeMode = element.shapeMode ?? "rect";

  // Secciones no enfocadas se atenúan para resaltar la enfocada
  const dimmed      = hasSelection && !isFocused;
  const fillAlpha   = dimmed ? "28" : "55";   // ~16% vs ~33%
  const strokeAlpha = dimmed ? "55" : "FF";
  const seatAlpha   = dimmed ? "40" : "FF";
  const fillColor   = color + fillAlpha;
  const strokeColor = color + strokeAlpha;
  const seatColor   = "#FFFFFF" + seatAlpha;
  const strokeW     = isFocused ? 3 : 2;

  const seatPositions = distributeSeats(element);

  // Path de la forma (rect o polygon)
  const shapePath = Skia.Path.Make();
  if (shapeMode === "polygon" && element.polygonPoints) {
    const flat = flattenPolygonForFill(element.polygonPoints);
    if (flat.length > 0) {
      shapePath.moveTo(element.x + flat[0].x, element.y + flat[0].y);
      for (let i = 1; i < flat.length; i++) {
        shapePath.lineTo(element.x + flat[i].x, element.y + flat[i].y);
      }
      shapePath.close();
    }
  } else {
    shapePath.addRect(rect(element.x, element.y, element.width, element.height));
  }

  return (
    <>
      {/* Fondo de sección */}
      <Path path={shapePath} color={fillColor}   style="fill" />
      {/* Borde de sección */}
      <Path path={shapePath} color={strokeColor} style="stroke" strokeWidth={strokeW} />

      {/* Sillas */}
      {seatPositions.map((seat, idx) => (
        <Circle
          key={idx}
          cx={element.x + seat.x}
          cy={element.y + seat.y}
          r={seat.r}
          color={seatColor}
        />
      ))}

      {/* Label */}
      {label.length > 0 && (
        <SkiaText
          x={element.x + element.width / 2 - label.length * 3.5}
          y={element.y + 16}
          text={label}
          color={strokeColor}
          size={14}
        />
      )}
    </>
  );
}

// ─── Estilos ──────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  root: {
    flex:            1,
    backgroundColor: "#1A1A1A",
    overflow:        "hidden",
  },
  overviewBtn: {
    position:          "absolute",
    bottom:            16,
    alignSelf:         "center",
    backgroundColor:   "rgba(124, 58, 237, 0.92)",
    paddingHorizontal: 20,
    paddingVertical:   10,
    borderRadius:      20,
    shadowColor:       "#000",
    shadowOffset:      { width: 0, height: 2 },
    shadowOpacity:     0.35,
    shadowRadius:      6,
    elevation:         6,
  },
  overviewBtnText: {
    color:      "#FFFFFF",
    fontSize:   14,
    fontWeight: "700",
  },
});
