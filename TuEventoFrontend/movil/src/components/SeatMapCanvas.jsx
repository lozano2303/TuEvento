import { useCallback, useEffect, useMemo } from "react";
import { View, StyleSheet } from "react-native";
import { Canvas, Path, Text, rect, Skia } from "@shopify/react-native-skia";
import { Gesture, GestureDetector } from "react-native-gesture-handler";
import {
  migrateElement,
  distributeSeats,
  computeTotalAABB,
  flattenPolygonForFill,
  filterSeatsByPage,
} from "../utils/layoutUtils";
import { findSeatAt, findSectionAt } from "../utils/seatHitTesting";

const PAGE_SIZE = 10;
const ZOOM_MARGIN_BASE = 60;       // Margen base (igual que web)
const SEAT_VIEW_MARGIN_BASE = 24;  // Margen base para vista de sillas (igual que web: 24px)
const MIN_SEAT_TOUCH_RADIUS_PX = 16; // Mismo valor que la web
const EMPTY_PAGE = [];

/**
 * Canvas de Skia que renderiza el mapa de sillas de un evento.
 *
 * Rendimiento: la geometría (posiciones de las sillas, escala, página visible) se calcula
 * una vez por elemento/página y no depende del estado de las sillas; en cada update solo
 * se leen del store las ≤100 sillas de la página actual.
 *
 * Props:
 * - layoutData: objeto con { canvasWidth, canvasHeight, elements }
 * - containerWidth / containerHeight: tamaño del contenedor (del onLayout)
 * - focusedSectionId: ID de la seccion enfocada (null = vista general)
 * - currentSubSectionIndex: indice de la sub-seccion activa
 * - currentRowPage / currentColPage: pagina actual de filas / columnas (0-indexed)
 * - sections: Array de EventSectionResponse con datos del backend
 * - seatStore: SeatDataStore con las sillas cargadas
 * - storeVersion: cambia con cada modificación del store (fuerza el re-render)
 * - onSeatPress: callback (seatId) => void
 * - onSectionPress: callback (eventSectionId) => void, al tocar una sección en la vista general
 * - currentUserId: ID del usuario actual
 * - onRowPagesChange / onColPagesChange: callback (total) => void
 */
export default function SeatMapCanvas({
  layoutData,
  containerWidth,
  containerHeight,
  focusedSectionId = null,
  currentSubSectionIndex = 0,
  currentRowPage = 0,
  currentColPage = 0,
  sections = [],
  seatStore,
  storeVersion = 0,
  onSeatPress,
  onSectionPress,
  currentUserId = null,
  onRowPagesChange,
  onColPagesChange,
}) {
  const inOverviewMode = focusedSectionId === null;

  // Elementos migrados y agrupados: solo cambian con el layout
  const { elements, groupedSections, elementIndex } = useMemo(() => {
    const migrated = (layoutData?.elements ?? []).map(migrateElement);
    const groups = {};
    const index = {};
    migrated.forEach((el, idx) => {
      index[el.id] = idx;
      if (el.type === "section" && el.backendSectionId) {
        (groups[el.backendSectionId] ??= []).push(el);
      }
    });
    return { elements: migrated, groupedSections: groups, elementIndex: index };
  }, [layoutData]);

  // distributeSeats es costoso (sobre todo en polígonos): se calcula una vez por elemento
  // Cache nueva cuando cambia el layout (la dependencia en `elements` es intencional)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const distributionCache = useMemo(() => new Map(), [elements]);
  const getDistribution = useCallback((el) => {
    let dist = distributionCache.get(el.id);
    if (!dist) {
      const result = distributeSeats(el);
      dist = Array.isArray(result)
        ? { positions: result, rowStructure: [] }
        : { positions: result.positions, rowStructure: result.rowStructure || [] };
      distributionCache.set(el.id, dist);
    }
    return dist;
  }, [distributionCache]);

  const sectionsById = useMemo(
    () => new Map(sections.map((s) => [s.eventSectionId, s])),
    [sections]
  );

  // Geometría de la vista: no depende del estado de las sillas
  const view = useMemo(() => computeView({
    elements, groupedSections, elementIndex, focusedSectionId, currentSubSectionIndex,
    currentRowPage, currentColPage, containerWidth, containerHeight, getDistribution,
  }), [
    elements, groupedSections, elementIndex, focusedSectionId, currentSubSectionIndex,
    currentRowPage, currentColPage, containerWidth, containerHeight, getDistribution,
  ]);

  const totalRowPages = view?.totalRowPages ?? 1;
  const totalColPages = view?.totalColPages ?? 1;

  // Notificar paginas al padre (fuera del render via useEffect)
  useEffect(() => {
    if (typeof onRowPagesChange === "function") onRowPagesChange(totalRowPages);
  }, [totalRowPages, onRowPagesChange]);

  useEffect(() => {
    if (typeof onColPagesChange === "function") onColPagesChange(totalColPages);
  }, [totalColPages, onColPagesChange]);

  // Sillas de la página actual con su estado: ≤100 lecturas del store por update
  const visibleSeatsByElement = useMemo(() => {
    const result = new Map();
    if (!view || inOverviewMode || !seatStore) return result;
    for (const element of view.visibleElements) {
      const pageSeats = view.pageSeatsByElement.get(element.id) ?? EMPTY_PAGE;
      const sorted = seatStore.getSortedSectionIndices(element.backendSectionId);
      const elementOffset = calculateSeatOffset(element, view.orderedLayoutElements);
      const seats = [];
      for (const { pos, realIndex } of pageSeats) {
        const idx = sorted[elementOffset + realIndex];
        if (idx === undefined) continue;
        seats.push({
          pos,
          seatId: seatStore.seatIds[idx],
          color: seatStore.colorHexAt(idx),
          mine: seatStore.isReservedBy(idx, currentUserId),
        });
      }
      result.set(element.id, seats);
    }
    return result;
    // storeVersion: el store es mutable, la versión indica que cambió su contenido
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view, inOverviewMode, seatStore, storeVersion, currentUserId]);

  // Vista general: tocar una sección la selecciona. Vista enfocada: tocar una silla visible.
  const tapGesture = useMemo(() => Gesture.Tap()
    .runOnJS(true)
    .onEnd((event) => {
      if (!view) return;
      const { scale, offsetX, offsetY } = view;
      if (inOverviewMode) {
        const section = findSectionAt(event.x, event.y, view.visibleElements, scale, offsetX, offsetY);
        if (section && onSectionPress) onSectionPress(section.backendSectionId);
        return;
      }
      if (!onSeatPress) return;
      for (const element of view.visibleElements) {
        const seats = visibleSeatsByElement.get(element.id) ?? EMPTY_PAGE;
        const seatId = findSeatAt(event.x, event.y, seats, element, scale, offsetX, offsetY);
        if (seatId !== null) {
          onSeatPress(seatId);
          break;
        }
      }
    }), [view, inOverviewMode, onSectionPress, onSeatPress, visibleSeatsByElement]);

  if (!view) return null;

  return (
    <View style={styles.container}>
      <GestureDetector gesture={tapGesture}>
        <View style={{ width: containerWidth, height: containerHeight }}>
          <Canvas style={{ width: containerWidth, height: containerHeight }}>
            {view.visibleElements.map((element) => (
              <SectionRenderer
                key={element.id}
                element={element}
                view={view}
                inOverviewMode={inOverviewMode}
                sectionData={sectionsById.get(element.backendSectionId)}
                seats={visibleSeatsByElement.get(element.id) ?? EMPTY_PAGE}
              />
            ))}
          </Canvas>
        </View>
      </GestureDetector>
    </View>
  );
}

// ---------------------------------------------------------------------------
// Geometría: elementos visibles, escala, centrado y sillas de la página actual
// ---------------------------------------------------------------------------
function computeView({
  elements, groupedSections, elementIndex, focusedSectionId, currentSubSectionIndex,
  currentRowPage, currentColPage, containerWidth, containerHeight, getDistribution,
}) {
  if (elements.length === 0) return null;
  if (!containerWidth || !containerHeight || containerWidth <= 0 || containerHeight <= 0) return null;

  const inOverviewMode = focusedSectionId === null;

  // Determinar elementos visibles segun filtro y sub-seccion
  let visibleElements;
  let orderedLayoutElements = [];
  if (!inOverviewMode) {
    const subSections = [...(groupedSections[focusedSectionId] || [])]
      .sort((a, b) => elementIndex[a.id] - elementIndex[b.id]);
    orderedLayoutElements = subSections;
    visibleElements = subSections.length > 1
      ? [subSections[currentSubSectionIndex]].filter(Boolean)
      : subSections;
  } else {
    visibleElements = elements.filter((el) => el.type === "section");
  }

  // AABB del contenido visible
  const totalAABB = computeTotalAABB(visibleElements);
  const contentWidth  = totalAABB.maxX - totalAABB.minX;
  const contentHeight = totalAABB.maxY - totalAABB.minY;
  if (!contentWidth || !contentHeight || contentWidth <= 0 || contentHeight <= 0) return null;

  // Márgenes adaptativos para pantallas pequeñas
  const minScreenDimension = Math.min(containerWidth, containerHeight);
  let marginFactor = 1;
  if (minScreenDimension < 400) marginFactor = 0.5;       // Pantalla muy pequeña
  else if (minScreenDimension < 500) marginFactor = 0.7;  // Pantalla pequeña
  const ZOOM_MARGIN = ZOOM_MARGIN_BASE * marginFactor;
  const SEAT_VIEW_MARGIN = SEAT_VIEW_MARGIN_BASE * marginFactor;

  const availWidth  = containerWidth  - ZOOM_MARGIN * 2;
  const availHeight = containerHeight - ZOOM_MARGIN * 2;

  const fitAll = () => {
    let s = Math.min(availWidth / contentWidth, availHeight / contentHeight);
    if (!isFinite(s) || s <= 0) s = 1;
    return {
      scale: s,
      offsetX: (containerWidth  - contentWidth  * s) / 2 - totalAABB.minX * s,
      offsetY: (containerHeight - contentHeight * s) / 2 - totalAABB.minY * s,
    };
  };

  let fit;
  let totalRowPages = 1;
  let totalColPages = 1;
  const pageSeatsByElement = new Map();
  const backgroundRects = new Map();

  if (inOverviewMode) {
    fit = fitAll();
  } else {
    // Sillas de la página actual (ventana 10x10) por elemento
    let pagesCounted = false;
    for (const el of visibleElements) {
      if (!el.seatLayout) continue;
      const { positions, rowStructure } = getDistribution(el);
      if (rowStructure.length === 0 || positions.length === 0) continue;
      const rowStart = currentRowPage * PAGE_SIZE;
      const rowEnd = Math.min(rowStructure.length, rowStart + PAGE_SIZE);
      const maxColsInVisibleRows = Math.max(1, ...rowStructure.slice(rowStart, rowEnd));
      const gridInfo = { totalRows: rowStructure.length, rowStructure, maxColsInVisibleRows };
      pageSeatsByElement.set(el.id, filterSeatsByPage(positions, gridInfo, currentColPage, currentRowPage, false));
      // Las páginas las define la primera sección con sillas (igual que antes)
      if (!pagesCounted) {
        totalRowPages = Math.ceil(rowStructure.length / PAGE_SIZE);
        totalColPages = Math.ceil(maxColsInVisibleRows / PAGE_SIZE);
        pagesCounted = true;
      }
    }

    const el = visibleElements.find((e) => e.type === "section" && e.seatLayout);
    const pageSeats = el ? pageSeatsByElement.get(el.id) : undefined;

    if (el && pageSeats && pageSeats.length > 0) {
      // Bounding box real de las sillas visibles (incluye el radio), como en la web
      let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
      for (const { pos } of pageSeats) {
        minX = Math.min(minX, pos.x - pos.r);
        maxX = Math.max(maxX, pos.x + pos.r);
        minY = Math.min(minY, pos.y - pos.r);
        maxY = Math.max(maxY, pos.y + pos.r);
      }
      const globalMinX = minX - SEAT_VIEW_MARGIN + el.x;
      const globalMaxX = maxX + SEAT_VIEW_MARGIN + el.x;
      const globalMinY = minY - SEAT_VIEW_MARGIN + el.y;
      const globalMaxY = maxY + SEAT_VIEW_MARGIN + el.y;
      const windowWidth  = globalMaxX - globalMinX;
      const windowHeight = globalMaxY - globalMinY;

      const effectiveViewportWidth  = containerWidth  - ZOOM_MARGIN * 2 - SEAT_VIEW_MARGIN * 2;
      const effectiveViewportHeight = containerHeight - ZOOM_MARGIN * 2 - SEAT_VIEW_MARGIN * 2;
      let scale = Math.min(effectiveViewportWidth / windowWidth, effectiveViewportHeight / windowHeight);

      // Mínimo táctil (16px) limitado a lo que cabe en pantalla, para no cortar en pantallas pequeñas
      const minTouchScale = MIN_SEAT_TOUCH_RADIUS_PX / (el.seatLayout.seatRadius || 7);
      const maxScaleForViewport = Math.min(availWidth / windowWidth, availHeight / windowHeight);
      scale = Math.max(scale, Math.min(minTouchScale, maxScaleForViewport));
      if (!isFinite(scale) || scale <= 0) scale = 1;

      const centerX = (globalMinX + globalMaxX) / 2;
      const centerY = (globalMinY + globalMaxY) / 2;
      fit = {
        scale,
        offsetX: ZOOM_MARGIN + SEAT_VIEW_MARGIN + effectiveViewportWidth / 2 - centerX * scale,
        offsetY: ZOOM_MARGIN + SEAT_VIEW_MARGIN + effectiveViewportHeight / 2 - centerY * scale,
      };
    } else if (el && pageSeats) {
      // Hay filas pero la página quedó vacía: fit clasico
      fit = fitAll();
    } else if (el) {
      // Sin estructura de filas: fit con mínimo táctil igual que web
      const base = fitAll();
      const scale = Math.max(base.scale, MIN_SEAT_TOUCH_RADIUS_PX / (el.seatLayout.seatRadius || 7));
      fit = {
        scale,
        offsetX: (containerWidth  - contentWidth  * scale) / 2 - totalAABB.minX * scale,
        offsetY: (containerHeight - contentHeight * scale) / 2 - totalAABB.minY * scale,
      };
    } else {
      // Seccion sin sillas: fit clasico
      fit = fitAll();
    }

    // Fondo de cada sección ajustado a las sillas de la página (igual que web)
    for (const element of visibleElements) {
      const pageSeats = pageSeatsByElement.get(element.id);
      if (!pageSeats || pageSeats.length === 0) continue;
      let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
      const seatRadius = pageSeats[0].pos.r || 7;
      for (const { pos } of pageSeats) {
        minX = Math.min(minX, pos.x);
        maxX = Math.max(maxX, pos.x);
        minY = Math.min(minY, pos.y);
        maxY = Math.max(maxY, pos.y);
      }
      backgroundRects.set(element.id, {
        x: minX - seatRadius - SEAT_VIEW_MARGIN,
        y: minY - seatRadius - SEAT_VIEW_MARGIN,
        width: maxX - minX + seatRadius * 2 + SEAT_VIEW_MARGIN * 2,
        height: maxY - minY + seatRadius * 2 + SEAT_VIEW_MARGIN * 2,
      });
    }
  }

  // Radio adaptativo de las sillas en pantallas pequeñas
  let seatRadiusFactor = 1;
  if (minScreenDimension < 400) seatRadiusFactor = 0.75;
  else if (minScreenDimension < 500) seatRadiusFactor = 0.85;

  return {
    ...fit,
    visibleElements,
    orderedLayoutElements,
    pageSeatsByElement,
    backgroundRects,
    totalRowPages,
    totalColPages,
    seatRadiusFactor,
  };
}

// ---------------------------------------------------------------------------
// Renderer de una seccion individual
// ---------------------------------------------------------------------------
function SectionRenderer({ element, view, inOverviewMode, sectionData, seats }) {
  const { scale, offsetX, offsetY, seatRadiusFactor } = view;
  const transformX = (x) => x * scale + offsetX;
  const transformY = (y) => y * scale + offsetY;

  const shapeMode = element.shapeMode ?? "rect";
  const color = element.color ?? "#3B82F6";
  const sectionName = sectionData?.sectionTypeName || element.label || "";
  const sectionPrice = sectionData?.price ?? 0;

  const backgroundRect = (!inOverviewMode && view.backgroundRects.get(element.id))
    || { x: 0, y: 0, width: element.width, height: element.height };

  // Path de la forma - usar backgroundRect para vista enfocada
  const shapePath = useMemo(() => {
    const path = Skia.Path.Make();
    if (shapeMode === "polygon" && element.polygonPoints) {
      const flatPoints = flattenPolygonForFill(element.polygonPoints);
      if (flatPoints.length > 0) {
        path.moveTo(transformX(element.x + flatPoints[0].x), transformY(element.y + flatPoints[0].y));
        for (let i = 1; i < flatPoints.length; i++) {
          path.lineTo(transformX(element.x + flatPoints[i].x), transformY(element.y + flatPoints[i].y));
        }
        path.close();
      }
    } else {
      path.addRect(rect(
        transformX(element.x + backgroundRect.x),
        transformY(element.y + backgroundRect.y),
        backgroundRect.width * scale,
        backgroundRect.height * scale
      ));
    }
    return path;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [element, scale, offsetX, offsetY, backgroundRect.x, backgroundRect.y, backgroundRect.width, backgroundRect.height]);

  // Sillas agrupadas por color: un Path por color en lugar de un Circle por silla
  const seatPaths = useMemo(() => {
    if (inOverviewMode || seats.length === 0) return null;
    const byColor = new Map();
    const mine = Skia.Path.Make();
    let hasMine = false;
    for (const { pos, color: seatColor, mine: isMine } of seats) {
      const x = transformX(element.x + pos.x);
      const y = transformY(element.y + pos.y);
      const r = Math.max(pos.r * scale * seatRadiusFactor, 8);
      let path = byColor.get(seatColor);
      if (!path) {
        path = Skia.Path.Make();
        byColor.set(seatColor, path);
      }
      path.addCircle(x, y, r);
      if (isMine) {
        mine.addCircle(x, y, r);
        hasMine = true;
      }
    }
    return { byColor: [...byColor.entries()], mine: hasMine ? mine : null };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [seats, inOverviewMode, element, scale, offsetX, offsetY, seatRadiusFactor]);

  const fillColor   = color + (inOverviewMode ? "80" : "40");
  const strokeWidth = inOverviewMode ? 3 * scale : 2 * scale;

  // Centro del texto basado en backgroundRect para vista enfocada
  const centerX = transformX(element.x + backgroundRect.x + backgroundRect.width / 2);
  const centerY = transformY(element.y + backgroundRect.y + backgroundRect.height / 2);

  return (
    <>
      <Path path={shapePath} color={fillColor} style="fill" />
      <Path path={shapePath} color={color} style="stroke" strokeWidth={strokeWidth} />

      {seatPaths?.byColor.map(([seatColor, path]) => (
        <Path key={seatColor} path={path} color={seatColor} style="fill" />
      ))}

      {/* Bordes blancos para mis reservas */}
      {seatPaths?.mine && (
        <Path
          path={seatPaths.mine}
          color="#FFFFFF"
          style="stroke"
          strokeWidth={Math.max(1, 2 * scale * 0.8)}
        />
      )}

      <Text
        x={centerX}
        y={centerY - (inOverviewMode ? 10 * scale : 15 * scale)}
        text={sectionName}
        color="#FFFFFF"
        size={inOverviewMode ? 16 * scale : 14 * scale}
      />

      {!inOverviewMode && (
        <Text
          x={centerX}
          y={centerY + 5 * scale}
          text={`$${sectionPrice.toLocaleString()}`}
          color="rgba(196,181,253,0.8)"
          size={12 * scale}
        />
      )}
    </>
  );
}

// ---------------------------------------------------------------------------
// Offset de inicio de sillas para un sub-elemento dentro de su seccion logica
// ---------------------------------------------------------------------------
function calculateSeatOffset(currentElement, orderedLayoutElements) {
  const idx = orderedLayoutElements.findIndex((el) => el.id === currentElement.id);
  if (idx === -1) return 0;
  let offset = 0;
  for (let i = 0; i < idx; i++) {
    offset += orderedLayoutElements[i].seatLayout?.targetSeats ?? 0;
  }
  return offset;
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#1A1A1A",
  },
});
