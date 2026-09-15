import React, { useEffect } from "react";
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
 * - reserving: Set de seatIds que están en proceso de reserva
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
  reserving = new Set(),
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

  // Espacio disponible con padding - márgenes adaptativos para pantallas pequeñas
  const ZOOM_MARGIN_BASE = 60;       // Margen base (igual que web)
  const SEAT_VIEW_MARGIN_BASE = 24;  // Margen base para vista de sillas (igual que web: 24px)
  
  // 🔧 NUEVO: Reducir márgenes en pantallas pequeñas para ganar espacio
  const minScreenDimension = Math.min(containerWidth, containerHeight);
  let ZOOM_MARGIN = ZOOM_MARGIN_BASE;
  let SEAT_VIEW_MARGIN = SEAT_VIEW_MARGIN_BASE;
  
  if (minScreenDimension < 400) {
    // Pantalla muy pequeña: reducir márgenes al 50%
    ZOOM_MARGIN *= 0.5;
    SEAT_VIEW_MARGIN *= 0.5;
  } else if (minScreenDimension < 500) {
    // Pantalla pequeña: reducir márgenes al 70%
    ZOOM_MARGIN *= 0.7;
    SEAT_VIEW_MARGIN *= 0.7;
  }
  
  const PADDING = ZOOM_MARGIN;  // Usar ZOOM_MARGIN como padding principal
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
          
          // 🔧 CRÍTICO: Calcular bounding box real desde las posiciones filtradas (como en web)
          // Incluir el radio completo de cada silla para evitar cortes
          const localMinX = Math.min(...positions.map((p) => p.x - p.r)) - SEAT_VIEW_MARGIN;
          const localMaxX = Math.max(...positions.map((p) => p.x + p.r)) + SEAT_VIEW_MARGIN;
          const localMinY = Math.min(...positions.map((p) => p.y - p.r)) - SEAT_VIEW_MARGIN;
          const localMaxY = Math.max(...positions.map((p) => p.y + p.r)) + SEAT_VIEW_MARGIN;

          // Convertir a coordenadas globales (igual que en web)
          const globalMinX = localMinX + el.x;
          const globalMaxX = localMaxX + el.x;
          const globalMinY = localMinY + el.y;
          const globalMaxY = localMaxY + el.y;

          // Usar dimensiones reales de las sillas filtradas (igual que web)
          const windowWidth  = (globalMaxX - globalMinX);
          const windowHeight = (globalMaxY - globalMinY);

          // Aplicar SEAT_VIEW_MARGIN para respiración visual como en web (24px)
          const availableWidth = containerWidth - ZOOM_MARGIN * 2 - SEAT_VIEW_MARGIN * 2;
          const availableHeight = containerHeight - ZOOM_MARGIN * 2 - SEAT_VIEW_MARGIN * 2;
          
          // Scale para que la ventana quepa en el viewport
          scale = Math.min(availableWidth / windowWidth, availableHeight / windowHeight);

          // Aplicar mínimo táctil igual que en web (16px), PERO con límite dinámico para pantallas pequeñas
          const MIN_SEAT_TOUCH_RADIUS_PX = 16; // Mismo valor que la web
          const minTouchScale = MIN_SEAT_TOUCH_RADIUS_PX / (seatLayout.seatRadius || 7);
          
          // 🔧 NUEVO: Limitar el zoom táctil si causa cortes en pantallas pequeñas
          // Calcular el scale máximo que permite que el contenido entre sin cortes
          const maxScaleForViewport = Math.min(
            (containerWidth - ZOOM_MARGIN * 2) / windowWidth,
            (containerHeight - ZOOM_MARGIN * 2) / windowHeight
          );
          
          // Usar el menor entre zoom táctil y lo que cabe en pantalla
          scale = Math.max(scale, Math.min(minTouchScale, maxScaleForViewport));

          if (!isFinite(scale) || scale <= 0) scale = 1;

          // 🔧 CRÍTICO: Centrar sobre el bounding box real de la página (igual que web)
          // Usar efectiveViewport con AMBOS márgenes como en web
          const effectiveViewportWidth = containerWidth - ZOOM_MARGIN * 2 - SEAT_VIEW_MARGIN * 2;
          const effectiveViewportHeight = containerHeight - ZOOM_MARGIN * 2 - SEAT_VIEW_MARGIN * 2;
          const centerX = (globalMinX + globalMaxX) / 2;
          const centerY = (globalMinY + globalMaxY) / 2;
          effectiveOffsetX = ZOOM_MARGIN + SEAT_VIEW_MARGIN + effectiveViewportWidth / 2 - centerX * scale;
          effectiveOffsetY = ZOOM_MARGIN + SEAT_VIEW_MARGIN + effectiveViewportHeight / 2 - centerY * scale;
        } else {
          // Sin sillas filtradas: fit clasico
          scale = Math.min(availWidth / contentWidth, availHeight / contentHeight);
          if (!isFinite(scale) || scale <= 0) scale = 1;
          effectiveOffsetX = (containerWidth  - contentWidth  * scale) / 2 - totalAABB.minX * scale;
          effectiveOffsetY = (containerHeight - contentHeight * scale) / 2 - totalAABB.minY * scale;
        }
      } else {
        // Sin rowStructure: fit con mínimo táctil igual que web
        const MIN_SEAT_TOUCH_RADIUS_PX = 16;
        scale = Math.max(
          Math.min(availWidth / contentWidth, availHeight / contentHeight),
          MIN_SEAT_TOUCH_RADIUS_PX / (seatLayout.seatRadius || 7)
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
                reserving={reserving}
                orderedLayoutElements={orderedLayoutElements}
                currentRowPage={currentRowPage}
                currentColPage={currentColPage}
                containerWidth={containerWidth}
                containerHeight={containerHeight}
              />
            ))}
          </Canvas>
        </View>
      </TouchableWithoutFeedback>
    </View>
  );
}

// ---------------------------------------------------------------------------
// Color de silla según estado - usando exactamente los mismos colores que la web
// ---------------------------------------------------------------------------
function getSeatColor(seat, currentUserId, isSectionFiltered = false, isReserving = false) {
  if (!seat) return "#6B7280";
  
  // Si la sección está filtrada (no es la seleccionada), usar color gris
  if (isSectionFiltered) return "#4B5563";
  
  // Si está en proceso de reserva, usar color gris claro como en web
  if (isReserving) return "#9CA3AF";
  
  const isMyReservation = seat.status === "RESERVED" && seat.reservedBy === currentUserId;
  const isOtherReservation = seat.status === "RESERVED" && seat.reservedBy !== currentUserId;
  
  if (seat.status === "AVAILABLE") return "#10B981";      // Verde para disponibles
  if (isMyReservation) return "#3B82F6";                  // Azul para mis reservas  
  if (isOtherReservation) return "#FBBF24";               // Amarillo para reservas de otros
  if (seat.status === "SOLD") return "#6B7280";           // Gris para vendidas
  if (seat.status === "COURTESY") return "#8B5CF6";       // Púrpura para cortesías
  
  return "#6B7280"; // Gris por defecto
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
  reserving = new Set(),
  orderedLayoutElements = [],
  currentRowPage = 0,
  currentColPage = 0,
  containerWidth = 400,
  containerHeight = 600,
}) {
  // 🔧 Calcular márgenes adaptativos localmente (igual que en el componente principal)
  const minScreenDimension = Math.min(containerWidth, containerHeight);
  let SEAT_VIEW_MARGIN = 24; // Margen base
  
  if (minScreenDimension < 400) {
    SEAT_VIEW_MARGIN *= 0.5; // Pantalla muy pequeña: 50%
  } else if (minScreenDimension < 500) {
    SEAT_VIEW_MARGIN *= 0.7; // Pantalla pequeña: 70%
  }
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

  // 🔧 NUEVO: Calcular background rect basado en sillas filtradas (igual que web)
  const backgroundRect = (() => {
    if (inOverviewMode || pageFilteredSeats.length === 0) {
      // Vista general → usar dimensiones completas de la sección
      return { x: 0, y: 0, width: element.width, height: element.height };
    }

    // Vista enfocada → calcular bounding box de las sillas de la página actual
    const positions = pageFilteredSeats.map(item => item.pos);
    const seatRadius = positions[0]?.r || 7;
    
    const minX = Math.min(...positions.map(p => p.x)) - seatRadius;
    const maxX = Math.max(...positions.map(p => p.x)) + seatRadius;
    const minY = Math.min(...positions.map(p => p.y)) - seatRadius;
    const maxY = Math.max(...positions.map(p => p.y)) + seatRadius;

    // Expandir el bounding box con margen visual simétrico (usar margen dinámico)
    return {
      x: minX - SEAT_VIEW_MARGIN,
      y: minY - SEAT_VIEW_MARGIN,
      width: (maxX - minX) + (SEAT_VIEW_MARGIN * 2),
      height: (maxY - minY) + (SEAT_VIEW_MARGIN * 2)
    };
  })();

  // Sillas de esta seccion ordenadas por codigo
  const allSectionSeats = Object.values(seats)
    .filter((s) => s.eventSectionId === element.backendSectionId)
    .sort((a, b) => a.code.localeCompare(b.code));

  const elementOffset = calculateSeatOffset(element, orderedLayoutElements);

  // Path de la forma - usar backgroundRect para vista enfocada
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
    // Para rectángulos, usar backgroundRect calculado
    shapePath.addRect(rect(
      transformX(element.x + backgroundRect.x), 
      transformY(element.y + backgroundRect.y), 
      backgroundRect.width * scale, 
      backgroundRect.height * scale
    ));
  }

  const fillOpacity  = inOverviewMode ? "80" : "40";
  const fillColor    = color + fillOpacity;
  const strokeColor  = color;
  const strokeWidth  = inOverviewMode ? 3 * scale : 2 * scale;
  
  // Centro del texto basado en backgroundRect para vista enfocada
  const centerX = inOverviewMode 
    ? transformX(element.x + element.width / 2)
    : transformX(element.x + backgroundRect.x + backgroundRect.width / 2);
  const centerY = inOverviewMode
    ? transformY(element.y + element.height / 2)
    : transformY(element.y + backgroundRect.y + backgroundRect.height / 2);

  return (
    <>
      <Path path={shapePath} color={fillColor}   style="fill" />
      <Path path={shapePath} color={strokeColor} style="stroke" strokeWidth={strokeWidth} />

      {/* Sillas: solo las de la pagina actual (ventana 10x10) */}
      {!inOverviewMode && pageFilteredSeats.map(({ pos, realIndex }) => {
        const seat = allSectionSeats[elementOffset + realIndex];
        if (!seat) return null;
        
        const isMyReservation = seat.status === "RESERVED" && seat.reservedBy === currentUserId;
        const isReserving = reserving.has(seat.seatId);
        const seatColor = getSeatColor(seat, currentUserId, false, isReserving);
        const seatX = transformX(element.x + pos.x);
        const seatY = transformY(element.y + pos.y);
        
        // 🔧 NUEVO: Reducir dinámicamente el radio si las sillas se ven cortadas
        let seatRadius = pos.r * scale;
        
        // Si estamos en una pantalla pequeña, reducir el radio para evitar cortes
        // Usar la misma lógica que para los márgenes
        if (minScreenDimension < 400) {
          // Pantalla muy pequeña: reducir radio al 75%
          seatRadius *= 0.75;
        } else if (minScreenDimension < 500) {
          // Pantalla pequeña: reducir radio al 85%
          seatRadius *= 0.85;
        }
        
        // Mínimo absoluto para que siga siendo táctil
        seatRadius = Math.max(seatRadius, 8);
        
        return (
          <React.Fragment key={seat.seatId}>
            {/* Círculo principal de la silla */}
            <Circle
              cx={seatX}
              cy={seatY}
              r={seatRadius}
              color={seatColor}
            />
            {/* Borde blanco para mis reservas como en la web */}
            {isMyReservation && (
              <Circle
                cx={seatX}
                cy={seatY}
                r={seatRadius}
                style="stroke"
                color="#FFFFFF"
                strokeWidth={Math.max(1, 2 * scale * 0.8)} // Reducir grosor del borde también
              />
            )}
          </React.Fragment>
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
