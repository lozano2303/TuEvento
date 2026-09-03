import { useState, useRef } from "react";
import { View, StyleSheet, TouchableWithoutFeedback } from "react-native";
import { Canvas, Path, Circle, Text, rect, Skia } from "@shopify/react-native-skia";
import {
  migrateElement,
  distributeSeats,
  computeTotalAABB,
  flattenPolygonForFill,
} from "../utils/layoutUtils";
import { findSeatAt } from "../utils/seatHitTesting";

/**
 * Canvas de Skia que renderiza el mapa de sillas de un evento.
 * 
 * Props:
 * - layoutData: objeto con { canvasWidth, canvasHeight, elements }
 * - containerWidth: ancho del contenedor (del onLayout)
 * - containerHeight: alto del contenedor (del onLayout)
 * - focusedSectionId: ID de la sección enfocada (null = vista general)
 * - sections: Array de EventSectionResponse con datos del backend (nombre, precio)
 * - seats: objeto { [seatId]: SeatResponse } con estado de todas las sillas
 * - onSeatPress: callback (seatId) => void para manejar taps en sillas
 * - currentUserId: ID del usuario actual (para colorear "mis reservas")
 * 
 * Por ahora: SOLO dibujo estático + tap en sillas (sin pan/zoom).
 * - Vista general: secciones visibles, sillas NO visibles, sin interacción
 * - Vista filtrada: solo sección seleccionada, sillas visibles y clickeables
 */
export default function SeatMapCanvas({ 
  layoutData, 
  containerWidth, 
  containerHeight, 
  focusedSectionId = null,
  currentSubSectionIndex = 0,
  currentRowPage = 0,
  sections = [],
  seats = {},
  onSeatPress,
  currentUserId = null,
  onRowPagesChange,
}) {
  if (!layoutData || !layoutData.elements || layoutData.elements.length === 0) {
    return null;
  }

  // Migrar elementos (asegurar que polygonPoints estén en formato nuevo)
  const elements = layoutData.elements.map(migrateElement);

  // Agrupar elementos por backendSectionId
  const groupedSections = {};
  elements.forEach((el) => {
    if (el.type === 'section' && el.backendSectionId) {
      const key = el.backendSectionId;
      if (!groupedSections[key]) groupedSections[key] = [];
      groupedSections[key].push(el);
    }
  });

  // Determinar elementos visibles según filtro y sub-sección
  let visibleElements;
  let orderedLayoutElements = []; // Para calcular offsets
  
  if (focusedSectionId) {
    const subSections = groupedSections[focusedSectionId] || [];
    
    // CRÍTICO: ordenar por índice original (posición en elements array)
    // para mantener consistencia con generateContinuousSeatsForSection
    const elementIndexMap = {};
    elements.forEach((el, idx) => {
      elementIndexMap[el.id] = idx;
    });
    
    subSections.sort((a, b) => elementIndexMap[a.id] - elementIndexMap[b.id]);
    orderedLayoutElements = subSections;
    
    if (subSections.length > 1) {
      // Si hay múltiples sub-secciones, mostrar solo la del índice actual
      visibleElements = [subSections[currentSubSectionIndex]];
    } else {
      // Si solo hay una, mostrarla
      visibleElements = subSections;
    }
  } else {
    // Vista general: mostrar todos los elementos
    visibleElements = elements.filter(el => el.type === 'section');
  }

  // Calcular AABB total para determinar zoom inicial (de los elementos visibles)
  const totalAABB = computeTotalAABB(visibleElements);
  const contentWidth = totalAABB.maxX - totalAABB.minX;
  const contentHeight = totalAABB.maxY - totalAABB.minY;

  // Fix auditoría: validar dimensiones del contenedor
  if (!containerWidth || !containerHeight || containerWidth <= 0 || containerHeight <= 0) {
    return null; // No renderizar si no hay dimensiones válidas
  }

  // Fix auditoría: validar dimensiones del contenido
  if (!contentWidth || !contentHeight || contentWidth <= 0 || contentHeight <= 0) {
    return null; // No renderizar si el contenido no tiene dimensiones
  }

  // Calcular zoom para que todo el contenido quepa con un pequeño padding
  const PADDING = 40;
  const MIN_SEAT_TOUCH_RADIUS_PX = 22; // Tamaño táctil mínimo deseado en píxeles
  const availWidth = containerWidth - PADDING * 2;
  const availHeight = containerHeight - PADDING * 2;

  const scaleX = availWidth / contentWidth;
  const scaleY = availHeight / contentHeight;
  let scaleToFit = Math.min(scaleX, scaleY);

  // Calcular zoom mínimo táctil SOLO si NO estamos en overview (sillas no son clickeables en overview)
  let scaleMinTactil = 1;
  if (!inOverviewMode) {
    const sectionsWithSeats = visibleElements.filter(el => el.type === 'section' && el.seatLayout);
    
    if (sectionsWithSeats.length > 0) {
      // Encontrar el seatRadius más pequeño del grupo (ser conservador)
      const minSeatRadius = Math.min(...sectionsWithSeats.map(el => el.seatLayout.seatRadius || 7));
      scaleMinTactil = MIN_SEAT_TOUCH_RADIUS_PX / minSeatRadius;
    }
  }

  // El scale final es el mayor entre ajustar todo y táctil mínimo (si aplica)
  let scale = Math.max(scaleToFit, scaleMinTactil);

  // Validar que el scale no sea NaN o Infinity
  if (!isFinite(scale) || scale <= 0) {
    scale = 1;
  }

  // Verificar si necesitamos paginación por filas
  const scaledWidth = contentWidth * scale;
  const scaledHeight = contentHeight * scale;
  
  // Calcular total de páginas de filas (solo en modo enfocado, no en overview)
  let totalRowPages = 1;
  if (!inOverviewMode && scaledHeight > availHeight) {
    const sectionsWithSeats = visibleElements.filter(el => el.type === 'section' && el.seatLayout);
    
    if (sectionsWithSeats.length > 0) {
      const pageHeight = availHeight / scale;
      totalRowPages = Math.ceil(contentHeight / pageHeight);
    }
  }

  // Notificar cambio en totalRowPages al padre
  if (onRowPagesChange && typeof onRowPagesChange === 'function') {
    onRowPagesChange(totalRowPages);
  }
  
  let effectiveContentCenterY = (totalAABB.minY + totalAABB.maxY) / 2;
  
  if (!inOverviewMode && scaledHeight > availHeight && currentRowPage > 0) {
    const sectionsWithSeats = visibleElements.filter(el => el.type === 'section' && el.seatLayout);
    
    if (sectionsWithSeats.length > 0) {
      // Aplicar paginación por filas - ajustar el centro Y para la página actual
      const pageHeight = availHeight / scale;
      const pageStartY = totalAABB.minY + (currentRowPage * pageHeight);
      const pageEndY = Math.min(totalAABB.maxY, pageStartY + pageHeight);
      effectiveContentCenterY = (pageStartY + pageEndY) / 2;
    }
  }

  // Calcular offset para centrar el contenido
  const effectiveScaledWidth = contentWidth * scale;
  const effectiveScaledHeight = Math.min(scaledHeight, availHeight);
  const offsetX = (containerWidth - effectiveScaledWidth) / 2 - totalAABB.minX * scale;
  const offsetY = (containerHeight - effectiveScaledHeight) / 2 - effectiveContentCenterY * scale;

  // Determinar si estamos en vista general o filtrada
  const inOverviewMode = focusedSectionId === null;

  // Handler para detectar tap en sillas (solo en modo filtrado)
  const handlePress = (event) => {
    console.log('[SeatMapCanvas] Press event received');
    
    if (inOverviewMode || !onSeatPress) {
      console.log('[SeatMapCanvas] Press ignored - inOverviewMode:', inOverviewMode, 'onSeatPress:', !!onSeatPress);
      return;
    }

    // Obtener posición del tap relativa al canvas
    const { locationX, locationY } = event.nativeEvent;
    console.log('[SeatMapCanvas] Press at:', locationX, locationY);

    // Buscar en cada elemento visible si se tocó una silla
    for (const element of visibleElements) {
      const seatResult = distributeSeats(element);
      const seatPositions = Array.isArray(seatResult) ? seatResult : seatResult.positions;
      
      // ORDENAR sillas de esta sección por código para consistencia
      const allSectionSeats = Object.values(seats)
        .filter((s) => s.eventSectionId === element.backendSectionId)
        .sort((a, b) => a.code.localeCompare(b.code));

      // Calcular offset de este elemento
      const elementOffset = calculateSeatOffset(element, orderedLayoutElements);

      // Crear array de sillas para este elemento específico aplicando offset
      const elementSeats = [];
      for (let i = 0; i < seatPositions.length; i++) {
        const seatIndex = elementOffset + i;
        const seat = allSectionSeats[seatIndex];
        if (seat) elementSeats.push(seat);
      }

      const clickedSeat = findSeatAt(
        locationX, 
        locationY, 
        seatPositions, 
        elementSeats, // Usar el array con offset aplicado
        element, 
        scale, 
        offsetX, 
        offsetY
      );

      if (clickedSeat) {
        console.log('[SeatMapCanvas] Seat clicked:', clickedSeat.seatId, clickedSeat.code);
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
          />
        ))}
      </Canvas>
        </View>
      </TouchableWithoutFeedback>
    </View>
  );
}

/**
 * Determina el color de una silla según su estado y propietario.
 * 
 * @param {Object} seat - SeatResponse con status, reservedBy, etc.
 * @param {number | null} currentUserId - ID del usuario actual
 * @returns {string} Color hexadecimal para la silla
 */
function getSeatColor(seat, currentUserId) {
  if (!seat) return '#6B7280'; // Gris por defecto
  
  if (seat.status === 'AVAILABLE') return '#FFFFFF';  // Blanco - disponible
  if (seat.status === 'SOLD') return '#EF4444';       // Rojo - vendida
  if (seat.status === 'COURTESY') return '#8B5CF6';   // Morado - cortesía
  
  if (seat.status === 'RESERVED') {
    if (seat.reservedBy === currentUserId) {
      return '#10B981';  // Verde - reservada por mí
    } else {
      return '#F59E0B';  // Amarillo/naranja - reservada por otro
    }
  }
  
  return '#6B7280';  // Gris por defecto
}

/**
 * Renderiza una sección individual (rect o polygon) con sus sillas.
 * 
 * En modo overview (vista general):
 * - Secciones con mayor opacidad
 * - Sillas NO visibles
 * - Muestra nombre de la sección del backend
 * 
 * En modo filtrado (sección seleccionada):
 * - Sillas visibles y coloreadas según estado
 * - Muestra nombre y precio de la sección
 */
function SectionRenderer({ element, scale, offsetX, offsetY, inOverviewMode, sections, seats, currentUserId, orderedLayoutElements = [] }) {
  const shapeMode = element.shapeMode ?? "rect";
  const color = element.color ?? "#3B82F6";
  const label = element.label ?? "";

  // Buscar datos de la sección del backend
  const sectionData = sections.find((s) => s.eventSectionId === element.backendSectionId);
  const sectionName = sectionData?.sectionTypeName || label;
  const sectionPrice = sectionData?.price ?? 0;

  // Transformar coordenadas al espacio del viewport
  const transformX = (x) => x * scale + offsetX;
  const transformY = (y) => y * scale + offsetY;

  // Calcular posiciones de sillas
  const seatResult = distributeSeats(element);
  const seatPositions = Array.isArray(seatResult) ? seatResult : seatResult.positions;

  // FILTRAR y ORDENAR sillas de esta sección por código para orden consistente
  const allSectionSeats = Object.values(seats)
    .filter((s) => s.eventSectionId === element.backendSectionId)
    .sort((a, b) => a.code.localeCompare(b.code));

  // Calcular offset de este elemento específico
  const elementOffset = calculateSeatOffset(element, orderedLayoutElements);

  // Path para la forma (rect o polygon)
  const shapePath = Skia.Path.Make();

  if (shapeMode === "polygon" && element.polygonPoints) {
    // Aplanar el polígono (manejar curvas Bézier)
    const flatPoints = flattenPolygonForFill(element.polygonPoints);
    
    if (flatPoints.length > 0) {
      const firstPoint = flatPoints[0];
      shapePath.moveTo(
        transformX(element.x + firstPoint.x),
        transformY(element.y + firstPoint.y)
      );

      for (let i = 1; i < flatPoints.length; i++) {
        const pt = flatPoints[i];
        shapePath.lineTo(
          transformX(element.x + pt.x),
          transformY(element.y + pt.y)
        );
      }
      shapePath.close();
    }
  } else {
    // Rectángulo
    shapePath.addRect(
      rect(
        transformX(element.x),
        transformY(element.y),
        element.width * scale,
        element.height * scale
      )
    );
  }

  // Opacidad según modo
  const fillOpacity = inOverviewMode ? "80" : "40"; // 50% vs 25%
  const fillColor = color + fillOpacity;
  const strokeColor = color;
  const strokeWidth = inOverviewMode ? 3 * scale : 2 * scale;

  // Calcular centro para el texto
  const centerX = transformX(element.x + element.width / 2);
  const centerY = transformY(element.y + element.height / 2);

  return (
    <>
      {/* Fondo de la sección */}
      <Path path={shapePath} color={fillColor} style="fill" />
      
      {/* Borde de la sección */}
      <Path path={shapePath} color={strokeColor} style="stroke" strokeWidth={strokeWidth} />

      {/* Sillas - SOLO visibles en modo filtrado */}
      {!inOverviewMode && seatPositions.map((pos, index) => {
        // Aplicar offset: este elemento muestra sillas desde elementOffset
        const seatIndex = elementOffset + index;
        const seat = allSectionSeats[seatIndex];
        
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

      {/* Nombre de la sección */}
      <Text
        x={centerX}
        y={centerY - (inOverviewMode ? 10 * scale : 15 * scale)}
        text={sectionName}
        color="#FFFFFF"
        size={inOverviewMode ? 16 * scale : 14 * scale}
      />

      {/* Precio - SOLO en modo filtrado */}
      {!inOverviewMode && (
        <Text
          x={centerX}
          y={centerY + (5 * scale)}
          text={`$${sectionPrice.toLocaleString()}`}
          color="rgba(196,181,253,0.8)"
          size={12 * scale}
        />
      )}
    </>
  );
}

/**
 * Calcula el offset (índice de inicio) de un elemento específico dentro del array
 * ordenado de sillas de toda la sección lógica.
 * 
 * CRÍTICO: debe usar el mismo orden que generateContinuousSeatsForSection()
 * (por originalIndex en el backend).
 */
function calculateSeatOffset(currentElement, orderedLayoutElements) {
  // Encontrar el índice de este elemento en el array ordenado
  const elementIndex = orderedLayoutElements.findIndex(el => el.id === currentElement.id);
  
  if (elementIndex === -1) return 0;
  
  // Sumar targetSeats de todos los elementos anteriores
  let offset = 0;
  for (let i = 0; i < elementIndex; i++) {
    const element = orderedLayoutElements[i];
    offset += element.seatLayout?.targetSeats ?? 0;
  }
  
  return offset;
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#1A1A1A",
  },
});
