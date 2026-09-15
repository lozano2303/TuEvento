import { useEffect } from "react";
import { View, StyleSheet, TouchableWithoutFeedback } from "react-native";
import { Canvas, Path, Circle, Text, rect, Skia } from "@shopify/react-native-skia";
import {
  migrateElement,
  distributeSeats,
  computeTotalAABB,
  flattenPolygonForFill,
  filterSeatsByPage,
} from "../utils/layoutUtils";
import { findSeatAt } from "../utils/seatHitTesting";

/**
 * Canvas de Skia que renderiza el mapa de sillas de un evento.
 *
 * Props:
 * - layoutData: objeto con { canvasWidth, canvasHeight, elements }
 * - containerWidth: ancho del contenedor (del onLayout)
 * - containerHeight: alto del contenedor (del onLayout)
 * - focusedSectionId: ID de la seccion enfocada (null = vista general)
 * - currentSubSectionIndex: indice de la sub-seccion activa
 * - currentRowPage: pagina actual de filas (0-indexed)
 * - currentColPage: pagina actual de columnas (0-indexed)
 * - sections: Array de EventSectionResponse con datos del backend
 * - seats: objeto { [seatId]: SeatResponse }
 * - onSeatPress: callback (seatId) => void
 * - currentUserId: ID del usuario actual
 * - onRowPagesChange: callback (total) => void
 * - onColPagesChange: callback (total) => void
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
  seats = {},
  onSeatPress,
  currentUserId = null,
  onRowPagesChange,
  onColPagesChange,
}) {
  const inOverviewMode = focusedSectionId === null;

  if (!layoutData || !layoutData.elements || layoutData.elements.length === 0) {
    return null;
  }

  // Migrar elementos
  const elements = layoutData.elements.map(migrateElement);

  // Agrupar elementos por backendSectionId
  const groupedSections = {};
  elements.forEach((el) => {
    if (el.type === "section" && el.backendSectionId) {
      const key = el.backendSectionId;
      if (!groupedSections[key]) groupedSections[key] = [];
      groupedSections[key].push(el);
    }
  });

  // Determinar elementos visibles segun filtro y sub-seccion
  let visibleElements;
  let orderedLayoutElements = [];

  if (focusedSectionId) {
    const subSections = groupedSections[focusedSectionId] || [];
    const elementIndexMap = {};
    elements.forEach((el, idx) => { elementIndexMap[el.id] = idx; });
    subSections.sort((a, b) => elementIndexMap[a.id] - elementIndexMap[b.id]);
    orderedLayoutElements = subSections;
    visibleElements = subSections.length > 1
      ? [subSections[currentSubSectionIndex]]
      : subSections;
  } else {
    visibleElements = elements.filter((el) => el.type === "section");
  }

  // AABB del contenido visible
  const totalAABB = computeTotalAABB(visibleElements);
  const contentWidth  = totalAABB.maxX - totalAABB.minX;
  const contentHeight = totalAABB.maxY - totalAABB.minY;

  if (!containerWidth || !containerHeight || containerWidth <= 0 || containerHeight <= 0) return null;
  if (!contentWidth  || !contentHeight  || contentWidth  <= 0 || contentHeight  <= 0) return null;

  // Espacio disponible con padding
  const PADDING     = 20;
  const availWidth  = containerWidth  - PADDING * 2;
  const availHeight = containerHeight - PADDING * 2;

  // --- CALCULO DE SCALE Y CENTRO ---
  let scale;
  let effectiveOffsetX;
  let effectiveOffsetY;
  let totalRowPages = 1;
  let totalColPages = 1;

  if (inOverviewMode) {
    // Overview: ajustar todo el contenido al viewport
    scale = Math.min(availWidth / contentWidth, availHeight / contentHeight);
    if (!isFinite(scale) || scale <= 0) scale = 1;
    effectiveOffsetX = (containerWidth  - contentWidth  * scale) / 2 - totalAABB.minX * scale;
    effectiveOffsetY = (containerHeight - contentHeight * scale) / 2 - totalAABB.minY * scale;
  } else {
    // Vista enfocada: calcular scale sobre la ventana 10x10 real de sillas
    const sectionsWithSeats = visibleElements.filter(
      (el) => el.type === "section" && el.seatLayout
    );

    if (sectionsWithSeats.length > 0) {
      const el         = sectionsWithSeats[0];
      const seatLayout = el.seatLayout;
      const seatResult = distributeSeats(el);
      const allPos     = Array.isArray(seatResult) ? seatResult : seatResult.positions;
      const rowStruct  = Array.isArray(seatResult) ? [] : (seatResult.rowStructure || []);

      if (rowStruct.length > 0 && allPos.length > 0) {
        // Calcular paginas
        const rowStart        = currentRowPage * 10;
        const rowEnd          = Math.min(rowStruct.length, rowStart + 10);
        const maxColsInPage   = Math.max(1, ...rowStruct.slice(rowStart, rowEnd));
        totalRowPages         = Math.ceil(rowStruct.length / 10);
        totalColPages         = Math.ceil(maxColsInPage / 10);

        // Filtrar posiciones de la pagina actual
        const gridInfo = {
          totalRows: rowStruct.length,
          rowStructure: rowStruct,
          maxColsInVisibleRows: maxColsInPage,
        };
        const pageSeats = filterSeatsByPage(allPos, gridInfo, currentColPage, currentRowPage, false);

        if (pageSeats.length > 0) {
          // Bounding box real de las sillas visibles en coordenadas locales al elemento
          const positions = pageSeats.map((s) => s.pos);
          const seatR     = positions[0].r || seatLayout.seatRadius || 7;
          const localMinX = Math.min(...positions.map((p) => p.x)) - seatR;
          const localMaxX = Math.max(...positions.map((p) => p.x)) + seatR;
          const localMinY = Math.min(...positions.map((p) => p.y)) - seatR;
          const localMaxY = Math.max(...positions.map((p) => p.y)) + seatR;

          // Convertir a coordenadas globales
          const globalMinX = localMinX + el.x;
          const globalMaxX = localMaxX + el.x;
          const globalMinY = localMinY + el.y;
          const globalMaxY = localMaxY + el.y;

          const windowWidth  = (globalMaxX - globalMinX) + PADDING * 2;
          const windowHeight = (globalMaxY - globalMinY) + PADDING * 2;

          // Scale para que la ventana quepa en el viewport
          scale = Math.min(availWidth / windowWidth, availHeight / windowHeight);

          // Aplicar minimo tactil (16px como en web)
          const minTouchScale = 16 / (seatLayout.seatRadius || 7);
          scale = Math.max(scale, minTouchScale);

          if (!isFinite(scale) || scale <= 0) scale = 1;

          // Centrar sobre el bounding box real de la pagina
          const centerX = (globalMinX + globalMaxX) / 2;
          const centerY = (globalMinY + globalMaxY) / 2;
          effectiveOffsetX = containerWidth  / 2 - centerX * scale;
          effectiveOffsetY = containerHeight / 2 - centerY * scale;
        } else {
          // Sin sillas filtradas: fit clasico
          scale = Math.min(availWidth / contentWidth, availHeight / contentHeight);
          if (!isFinite(scale) || scale <= 0) scale = 1;
          effectiveOffsetX = (containerWidth  - contentWidth  * scale) / 2 - totalAABB.minX * scale;
          effectiveOffsetY = (containerHeight - contentHeight * scale) / 2 - totalAABB.minY * scale;
        }
      } else {
        // Sin rowStructure: fit con minimo tactil
        scale = Math.max(
          Math.min(availWidth / contentWidth, availHeight / contentHeight),
          16 / (seatLayout.seatRadius || 7)
        );
        if (!isFinite(scale) || scale <= 0) scale = 1;
        effectiveOffsetX = (containerWidth  - contentWidth  * scale) / 2 - totalAABB.minX * scale;
        effectiveOffsetY = (containerHeight - contentHeight * scale) / 2 - totalAABB.minY * scale;
      }
    } else {
      // Seccion sin sillas: fit clasico
      scale = Math.min(availWidth / contentWidth, availHeight / contentHeight);
      if (!isFinite(scale) || scale <= 0) scale = 1;
      effectiveOffsetX = (containerWidth  - contentWidth  * scale) / 2 - totalAABB.minX * scale;
      effectiveOffsetY = (containerHeight - contentHeight * scale) / 2 - totalAABB.minY * scale;
    }
  }

  const offsetX = effectiveOffsetX;
  const offsetY = effectiveOffsetY;

  // Notificar paginas al padre (fuera del render via useEffect)
  useEffect(() => {
    if (typeof onRowPagesChange === "function") onRowPagesChange(totalRowPages);
  }, [totalRowPages, onRowPagesChange]);

  useEffect(() => {
    if (typeof onColPagesChange === "function") onColPagesChange(totalColPages);
  }, [totalColPages, onColPagesChange]);

  // Handler de tap en sillas
  const handlePress = (event) => {
    if (inOverviewMode || !onSeatPress) return;
    const { locationX, locationY } = event.nativeEvent;

    for (const element of visibleElements) {
      const seatResult  = distributeSeats(element);
      const seatPositions = Array.isArray(seatResult) ? seatResult : seatResult.positions;
      const allSectionSeats = Object.values(seats)
        .filter((s) => s.eventSectionId === element.backendSectionId)
        .sort((a, b) => a.code.localeCompare(b.code));
      const elementOffset = calculateSeatOffset(element, orderedLayoutElements);
      const elementSeats  = seatPositions.map((_, i) => allSectionSeats[elementOffset + i]).filter(Boolean);

      const clickedSeat = findSeatAt(locationX, locationY, seatPositions, elementSeats, element, scale, offsetX, offsetY);
      if (clickedSeat) {
        onSeatPress(clickedSeat.seatId);
        break;
      }
    }
  };

  return (
    <View style={styles.container}>
      <TouchableWithoutFeedback onPress={handlePress}>
        <View style={{ width: containerWidth, height: containerHeight }}>
          <Canvas style={{ width: containerWidth, height: containerHeight }}>
            {visibleElements.map((element) => (
              <SectionRenderer
                key={element.id}
                element={element}
                scale={scale}
                offsetX={offsetX}
                offsetY={offsetY}
                inOverviewMode={inOverviewMode}
                sections={sections}
                seats={seats}
                currentUserId={currentUserId}
                orderedLayoutElements={orderedLayoutElements}
                currentRowPage={currentRowPage}
                currentColPage={currentColPage}
              />
            ))}
          </Canvas>
        </View>
      </TouchableWithoutFeedback>
    </View>
  );
}

// ---------------------------------------------------------------------------
// Color de silla segun estado
// ---------------------------------------------------------------------------
function getSeatColor(seat, currentUserId) {
  if (!seat) return "#6B7280";
  if (seat.status === "AVAILABLE") return "#FFFFFF";
  if (seat.status === "SOLD")      return "#EF4444";
  if (seat.status === "COURTESY")  return "#8B5CF6";
  if (seat.status === "RESERVED") {
    return seat.reservedBy === currentUserId ? "#10B981" : "#F59E0B";
  }
  return "#6B7280";
}

// ---------------------------------------------------------------------------
// Renderer de una seccion individual
// ---------------------------------------------------------------------------
function SectionRenderer({
  element,
  scale,
  offsetX,
  offsetY,
  inOverviewMode,
  sections,
  seats,
  currentUserId,
  orderedLayoutElements = [],
  currentRowPage = 0,
  currentColPage = 0,
}) {
  const shapeMode   = element.shapeMode ?? "rect";
  const color       = element.color     ?? "#3B82F6";
  const label       = element.label     ?? "";
  const sectionData = sections.find((s) => s.eventSectionId === element.backendSectionId);
  const sectionName = sectionData?.sectionTypeName || label;
  const sectionPrice = sectionData?.price ?? 0;

  const transformX = (x) => x * scale + offsetX;
  const transformY = (y) => y * scale + offsetY;

  // Distribuir sillas con estructura de filas
  const seatResult   = distributeSeats(element);
  const seatPositions = Array.isArray(seatResult) ? seatResult : seatResult.positions;
  const rowStructure  = Array.isArray(seatResult) ? [] : (seatResult.rowStructure || []);

  // Construir gridInfo y filtrar sillas de la pagina actual
  const gridInfo = (() => {
    if (inOverviewMode || rowStructure.length === 0) {
      return { totalRows: 0, rowStructure: [], maxColsInVisibleRows: 0 };
    }
    const rowStart = currentRowPage * 10;
    const rowEnd   = Math.min(rowStructure.length, rowStart + 10);
    const maxColsInVisibleRows = Math.max(1, ...rowStructure.slice(rowStart, rowEnd));
    return { totalRows: rowStructure.length, rowStructure, maxColsInVisibleRows };
  })();

  const pageFilteredSeats = filterSeatsByPage(
    seatPositions, gridInfo, currentColPage, currentRowPage, inOverviewMode
  );

  // Sillas de esta seccion ordenadas por codigo
  const allSectionSeats = Object.values(seats)
    .filter((s) => s.eventSectionId === element.backendSectionId)
    .sort((a, b) => a.code.localeCompare(b.code));

  const elementOffset = calculateSeatOffset(element, orderedLayoutElements);

  // Path de la forma
  const shapePath = Skia.Path.Make();
  if (shapeMode === "polygon" && element.polygonPoints) {
    const flatPoints = flattenPolygonForFill(element.polygonPoints);
    if (flatPoints.length > 0) {
      shapePath.moveTo(transformX(element.x + flatPoints[0].x), transformY(element.y + flatPoints[0].y));
      for (let i = 1; i < flatPoints.length; i++) {
        shapePath.lineTo(transformX(element.x + flatPoints[i].x), transformY(element.y + flatPoints[i].y));
      }
      shapePath.close();
    }
  } else {
    shapePath.addRect(rect(transformX(element.x), transformY(element.y), element.width * scale, element.height * scale));
  }

  const fillOpacity  = inOverviewMode ? "80" : "40";
  const fillColor    = color + fillOpacity;
  const strokeColor  = color;
  const strokeWidth  = inOverviewMode ? 3 * scale : 2 * scale;
  const centerX      = transformX(element.x + element.width  / 2);
  const centerY      = transformY(element.y + element.height / 2);

  return (
    <>
      <Path path={shapePath} color={fillColor}   style="fill" />
      <Path path={shapePath} color={strokeColor} style="stroke" strokeWidth={strokeWidth} />

      {/* Sillas: solo las de la pagina actual (ventana 10x10) */}
      {!inOverviewMode && pageFilteredSeats.map(({ pos, realIndex }) => {
        const seat = allSectionSeats[elementOffset + realIndex];
        if (!seat) return null;
        return (
          <Circle
            key={seat.seatId}
            cx={transformX(element.x + pos.x)}
            cy={transformY(element.y + pos.y)}
            r={pos.r * scale}
            color={getSeatColor(seat, currentUserId)}
          />
        );
      })}

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
