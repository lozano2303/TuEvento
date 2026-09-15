import React, { useEffect, useMemo } from "react";
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

// 🚀 Cache global para distribución de sillas (evitar recálculos costosos)
const seatDistributionCache = new Map();

// 🚀 Cache de layout para evitar recálculos de posiciones
const layoutCalculationCache = new Map();

// Limpiar cache cuando sea muy grande (prevenir memory leaks)
const MAX_CACHE_SIZE = 50;
const cleanupCache = (cache) => {
  if (cache.size > MAX_CACHE_SIZE) {
    const keys = Array.from(cache.keys());
    const keysToDelete = keys.slice(0, keys.length - MAX_CACHE_SIZE);
    keysToDelete.forEach(key => cache.delete(key));
    console.log(`🧹 [Cache] Limpieza realizada: ${keysToDelete.length} entradas eliminadas`);
  }
};

/**
 * Canvas de Skia que renderiza el mapa de sillas de un evento.
 * OPTIMIZADO con memoización para evitar re-renders costosos.
 */
function SeatMapCanvas({
  layoutData,
  containerWidth,
  containerHeight,
  focusedSectionId = null,
  currentSubSectionIndex = 0,
  currentRowPage = 0,
  currentColPage = 0,
  sections = [],
  seats = {},
  seatsBySection = {}, // 🚀 NUEVO: Índice pre-computado por sección
  onSeatPress,
  currentUserId = null,
  reserving = new Set(),
  onRowPagesChange,
  onColPagesChange,
}) {
  // 🔍 PERFORMANCE LOGGING
  const renderStartTime = performance.now();
  const inOverviewMode = focusedSectionId === null;
  
  console.log(`🚀 [SeatMapCanvas] Render iniciado - Modo: ${inOverviewMode ? 'Overview' : `Sección ${focusedSectionId}`}`);
  console.log(`📊 [SeatMapCanvas] Datos iniciales:`, {
    layoutElements: layoutData?.elements?.length || 0,
    totalSeats: Object.keys(seats).length,
    sections: sections.length,
    containerSize: `${containerWidth}x${containerHeight}`,
    focusedSection: focusedSectionId,
    subSection: currentSubSectionIndex,
    page: `${currentRowPage},${currentColPage}`
  });

  // 🚀 CALCULAR dimensiones de pantalla temprano para usarlas en memoización
  const minScreenDimension = Math.min(containerWidth, containerHeight);

  if (!layoutData || !layoutData.elements || layoutData.elements.length === 0) {
    console.log(`❌ [SeatMapCanvas] Sin datos de layout - tiempo: ${(performance.now() - renderStartTime).toFixed(2)}ms`);
    return null;
  }

  // 🔍 TIMING: Migración de elementos
  const migrationStart = performance.now();
  // Migrar elementos
  const elements = layoutData.elements.map(migrateElement);
  console.log(`⚡ [SeatMapCanvas] Migración elementos: ${(performance.now() - migrationStart).toFixed(2)}ms`);

  // 🔍 TIMING: Agrupación de secciones
  const groupingStart = performance.now();
  // Agrupar elementos por backendSectionId
  const groupedSections = {};
  elements.forEach((el) => {
    if (el.type === "section" && el.backendSectionId) {
      const key = el.backendSectionId;
      if (!groupedSections[key]) groupedSections[key] = [];
      groupedSections[key].push(el);
    }
  });
  console.log(`🔗 [SeatMapCanvas] Agrupación secciones: ${(performance.now() - groupingStart).toFixed(2)}ms`);

  // 🔍 TIMING: Determinación de elementos visibles
  const visibilityStart = performance.now();
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
  
  console.log(`👀 [SeatMapCanvas] Elementos visibles calculados: ${(performance.now() - visibilityStart).toFixed(2)}ms`, {
    visibleCount: visibleElements.length,
    orderedCount: orderedLayoutElements.length
  });

  // 🚀 OPTIMIZACIÓN CRÍTICA: Memoizar distribución de sillas (evita 110ms por render)
  const seatDistributionResults = useMemo(() => {
    const memoStart = performance.now();
    const results = {};
    
    visibleElements.forEach((element) => {
      if (element.type === "section" && element.seatLayout) {
        const cacheKey = `${element.id}-${element.backendSectionId}-${element.seatLayout.targetSeats}`;
        
        if (seatDistributionCache.has(cacheKey)) {
          results[element.id] = seatDistributionCache.get(cacheKey);
          console.log(`💾 [SeatMapCanvas] Cache HIT para elemento ${element.id}`);
        } else {
          const distributionStart = performance.now();
          const seatResult = distributeSeats(element);
          const seatPositions = Array.isArray(seatResult) ? seatResult : seatResult.positions;
          const rowStructure = Array.isArray(seatResult) ? [] : (seatResult.rowStructure || []);
          
          const result = { seatPositions, rowStructure };
          seatDistributionCache.set(cacheKey, result);
          results[element.id] = result;
          
          // Limpiar cache si está muy grande
          cleanupCache(seatDistributionCache);
          
          console.log(`💾 [SeatMapCanvas] Cache MISS para elemento ${element.id}: ${(performance.now() - distributionStart).toFixed(2)}ms`);
        }
      }
    });
    
    console.log(`🚀 [SeatMapCanvas] Distribución memoizada: ${(performance.now() - memoStart).toFixed(2)}ms`);
    return results;
  }, [
    // Dependencias más específicas para evitar recálculos innecesarios
    focusedSectionId,
    visibleElements.length,
    visibleElements.map(el => `${el.id}-${el.backendSectionId}-${el.seatLayout?.targetSeats || 0}`).join(',')
  ]);

  // 🔍 TIMING: Cálculo AABB
  const aabbStart = performance.now();
  // AABB del contenido visible
  const totalAABB = computeTotalAABB(visibleElements);
  const contentWidth  = totalAABB.maxX - totalAABB.minX;
  const contentHeight = totalAABB.maxY - totalAABB.minY;
  console.log(`📐 [SeatMapCanvas] Cálculo AABB: ${(performance.now() - aabbStart).toFixed(2)}ms`, {
    aabb: totalAABB,
    contentSize: `${contentWidth.toFixed(1)}x${contentHeight.toFixed(1)}`
  });

  if (!containerWidth || !containerHeight || containerWidth <= 0 || containerHeight <= 0) {
    console.log(`❌ [SeatMapCanvas] Contenedor inválido: ${containerWidth}x${containerHeight}`);
    return null;
  }
  if (!contentWidth  || !contentHeight  || contentWidth  <= 0 || contentHeight  <= 0) {
    console.log(`❌ [SeatMapCanvas] Contenido inválido: ${contentWidth}x${contentHeight}`);
    return null;
  }

  // 🔍 TIMING: Cálculo de márgenes y escala
  const scaleStart = performance.now();
  // Espacio disponible con padding - márgenes adaptativos para pantallas pequeñas
  const ZOOM_MARGIN_BASE = 60;       // Margen base (igual que web)
  const SEAT_VIEW_MARGIN_BASE = 24;  // Margen base para vista de sillas (igual que web: 24px)
  
  // 🔧 NUEVO: Reducir márgenes en pantallas pequeñas para ganar espacio
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
      // 🔍 TIMING: Distribución de sillas
      const seatDistributionStart = performance.now();
      const el         = sectionsWithSeats[0];
      const seatLayout = el.seatLayout;
      const seatResult = distributeSeats(el);
      const allPos     = Array.isArray(seatResult) ? seatResult : seatResult.positions;
      const rowStruct  = Array.isArray(seatResult) ? [] : (seatResult.rowStructure || []);
      console.log(`🪑 [SeatMapCanvas] Distribución sillas: ${(performance.now() - seatDistributionStart).toFixed(2)}ms`, {
        sectionId: el.backendSectionId,
        totalPositions: allPos.length,
        rows: rowStruct.length,
        targetSeats: seatLayout.targetSeats
      });

      if (rowStruct.length > 0 && allPos.length > 0) {
        // 🔍 TIMING: Cálculo de páginas (CONSISTENTE CON WEB)
        const paginationStart = performance.now();
        // Mantener siempre 10x10 = 100 sillas como en la web
        const SEATS_PER_ROW_PAGE = 10; // Siempre 10 filas como en web
        const SEATS_PER_COL_PAGE = 10; // Siempre 10 columnas como en web
        
        // Calcular paginas con tamaño fijo (igual que web)
        const rowStart        = currentRowPage * SEATS_PER_ROW_PAGE;
        const rowEnd          = Math.min(rowStruct.length, rowStart + SEATS_PER_ROW_PAGE);
        const maxColsInPage   = Math.max(1, ...rowStruct.slice(rowStart, rowEnd));
        totalRowPages         = Math.ceil(rowStruct.length / SEATS_PER_ROW_PAGE);
        totalColPages         = Math.ceil(maxColsInPage / SEATS_PER_COL_PAGE);
        
        console.log(`📄 [SeatMapCanvas] Páginas (web): ${(performance.now() - paginationStart).toFixed(2)}ms`, {
          pageSize: `${SEATS_PER_ROW_PAGE}x${SEATS_PER_COL_PAGE}`,
          totalRowPages,
          totalColPages,
          currentPage: `${currentRowPage},${currentColPage}`,
          screenSize: minScreenDimension
        });

        // 🔍 TIMING: Filtrado de sillas por página (100 SILLAS COMO WEB)
        const filteringStart = performance.now();
        // Filtrar posiciones de la pagina actual con tamaño fijo (como web)
        const gridInfo = {
          totalRows: rowStruct.length,
          rowStructure: rowStruct,
          maxColsInVisibleRows: maxColsInPage,
          // 🚀 FIJO: Siempre 10x10 como la web
          rowPageSize: SEATS_PER_ROW_PAGE,
          colPageSize: SEATS_PER_COL_PAGE,
        };
        const pageSeats = filterSeatsByPage(allPos, gridInfo, currentColPage, currentRowPage, false);
        console.log(`🔍 [SeatMapCanvas] Filtrado web: ${(performance.now() - filteringStart).toFixed(2)}ms`, {
          totalSeats: allPos.length,
          filteredSeats: pageSeats.length,
          reduction: `${((1 - pageSeats.length / allPos.length) * 100).toFixed(1)}%`,
          pageSize: `${SEATS_PER_ROW_PAGE}x${SEATS_PER_COL_PAGE}`
        });

        if (pageSeats.length > 0) {
          // 🔍 TIMING: Cálculo bounding box de página
          const bboxStart = performance.now();
          // Bounding box real de las sillas visibles en coordenadas locales al elemento
          const positions = pageSeats.map((s) => s.pos);
          const seatR     = positions[0].r || seatLayout.seatRadius || 7;
          
          // 🔧 CRÍTICO: Calcular bounding box real desde las posiciones filtradas (como en web)
          // Incluir el radio completo de cada silla para evitar cortes
          // 🚀 OPTIMIZACIÓN HONOR X8A: Usar márgenes adaptativos más pequeños
          let adaptiveMargin = SEAT_VIEW_MARGIN;
          if (minScreenDimension < 400) {
            adaptiveMargin = SEAT_VIEW_MARGIN * 0.5; // Menos margen en pantallas pequeñas
          }
          
          const localMinX = Math.min(...positions.map((p) => p.x - p.r)) - adaptiveMargin;
          const localMaxX = Math.max(...positions.map((p) => p.x + p.r)) + adaptiveMargin;
          const localMinY = Math.min(...positions.map((p) => p.y - p.r)) - adaptiveMargin;
          const localMaxY = Math.max(...positions.map((p) => p.y + p.r)) + adaptiveMargin;

          // Convertir a coordenadas globales (igual que en web)
          const globalMinX = localMinX + el.x;
          const globalMaxX = localMaxX + el.x;
          const globalMinY = localMinY + el.y;
          const globalMaxY = localMaxY + el.y;

          // Usar dimensiones reales de las sillas filtradas (igual que web)
          const windowWidth  = (globalMaxX - globalMinX);
          const windowHeight = (globalMaxY - globalMinY);
          
          console.log(`📦 [SeatMapCanvas] Bounding box página: ${(performance.now() - bboxStart).toFixed(2)}ms`, {
            localBounds: `${localMinX.toFixed(1)},${localMinY.toFixed(1)} → ${localMaxX.toFixed(1)},${localMaxY.toFixed(1)}`,
            globalBounds: `${globalMinX.toFixed(1)},${globalMinY.toFixed(1)} → ${globalMaxX.toFixed(1)},${globalMaxY.toFixed(1)}`,
            windowSize: `${windowWidth.toFixed(1)}x${windowHeight.toFixed(1)}`
          });

          // 🔍 TIMING: Cálculo de escala final
          const scaleCalculationStart = performance.now();

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
          
          console.log(`🔍 [SeatMapCanvas] Escala calculada: ${(performance.now() - scaleCalculationStart).toFixed(2)}ms`, {
            initialScale: scale,
            minTouchScale,
            maxScaleForViewport,
            finalScale: scale
          });

          // 🔍 TIMING: Centrado y offset
          const centeringStart = performance.now();
          // 🔧 CRÍTICO: Centrar sobre el bounding box real de la página (igual que web)
          // Usar efectiveViewport con AMBOS márgenes como en web
          const effectiveViewportWidth = containerWidth - ZOOM_MARGIN * 2 - SEAT_VIEW_MARGIN * 2;
          const effectiveViewportHeight = containerHeight - ZOOM_MARGIN * 2 - SEAT_VIEW_MARGIN * 2;
          const centerX = (globalMinX + globalMaxX) / 2;
          const centerY = (globalMinY + globalMaxY) / 2;
          effectiveOffsetX = ZOOM_MARGIN + SEAT_VIEW_MARGIN + effectiveViewportWidth / 2 - centerX * scale;
          effectiveOffsetY = ZOOM_MARGIN + SEAT_VIEW_MARGIN + effectiveViewportHeight / 2 - centerY * scale;
          console.log(`🎯 [SeatMapCanvas] Centrado: ${(performance.now() - centeringStart).toFixed(2)}ms`, {
            center: `${centerX.toFixed(1)},${centerY.toFixed(1)}`,
            offset: `${effectiveOffsetX.toFixed(1)},${effectiveOffsetY.toFixed(1)}`
          });
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
  
  console.log(`⚡ [SeatMapCanvas] Cálculo escala completado: ${(performance.now() - scaleStart).toFixed(2)}ms`, {
    finalScale: scale,
    finalOffset: `${offsetX.toFixed(1)},${offsetY.toFixed(1)}`,
    totalRowPages,
    totalColPages
  });

  // Notificar paginas al padre (fuera del render via useEffect)
  useEffect(() => {
    if (typeof onRowPagesChange === "function") onRowPagesChange(totalRowPages);
  }, [totalRowPages, onRowPagesChange]);

  useEffect(() => {
    if (typeof onColPagesChange === "function") onColPagesChange(totalColPages);
  }, [totalColPages, onColPagesChange]);

  // Handler de tap en sillas
  const handlePress = (event) => {
    const pressStart = performance.now();
    if (inOverviewMode || !onSeatPress) return;
    const { locationX, locationY } = event.nativeEvent;
    
    console.log(`👆 [SeatMapCanvas] Procesando tap en: ${locationX.toFixed(1)},${locationY.toFixed(1)}`);

    for (const element of visibleElements) {
      const seatResult  = distributeSeats(element);
      const seatPositions = Array.isArray(seatResult) ? seatResult : seatResult.positions;
      
      // 🚀 OPTIMIZACIÓN: Usar índice pre-computado en lugar de filtrar
      const allSectionSeats = seatsBySection[element.backendSectionId] || [];
      const elementOffset = calculateSeatOffset(element, orderedLayoutElements);
      const elementSeats  = seatPositions.map((_, i) => allSectionSeats[elementOffset + i]).filter(Boolean);

      const clickedSeat = findSeatAt(locationX, locationY, seatPositions, elementSeats, element, scale, offsetX, offsetY);
      if (clickedSeat) {
        console.log(`✅ [SeatMapCanvas] Silla encontrada: ${clickedSeat.seatId} - tiempo: ${(performance.now() - pressStart).toFixed(2)}ms`);
        onSeatPress(clickedSeat.seatId);
        break;
      }
    }
    
    console.log(`👆 [SeatMapCanvas] Tap completado: ${(performance.now() - pressStart).toFixed(2)}ms`);
  };

  // 🔍 TIMING: Render final
  const renderingStart = performance.now();
  console.log(`🎨 [SeatMapCanvas] Iniciando renderizado final - tiempo total hasta ahora: ${(renderingStart - renderStartTime).toFixed(2)}ms`);

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
                seatsBySection={seatsBySection} // 🚀 NUEVO: Pasar índice optimizado
                currentUserId={currentUserId}
                reserving={reserving}
                orderedLayoutElements={orderedLayoutElements}
                currentRowPage={currentRowPage}
                currentColPage={currentColPage}
                containerWidth={containerWidth}
                containerHeight={containerHeight}
                renderStartTime={renderStartTime} // Pasar tiempo de inicio para logging
                seatDistributionResults={seatDistributionResults} // 🚀 NUEVO: Pasar cache
              />
            ))}
          </Canvas>
        </View>
      </TouchableWithoutFeedback>
      {/* Log final después de renderizar */}
      {(() => {
        const totalTime = performance.now() - renderStartTime;
        console.log(`🏁 [SeatMapCanvas] RENDER COMPLETO: ${totalTime.toFixed(2)}ms`);
        if (totalTime > 1000) {
          console.warn(`⚠️ [SeatMapCanvas] RENDER LENTO: ${totalTime.toFixed(2)}ms - Considerar optimizaciones`);
        }
        return null;
      })()}
    </View>
  );
}

export default React.memo(SeatMapCanvas);

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
  seatsBySection = {}, // 🚀 NUEVO: Índice optimizado
  currentUserId,
  reserving = new Set(),
  orderedLayoutElements = [],
  currentRowPage = 0,
  currentColPage = 0,
  containerWidth = 400,
  containerHeight = 600,
  renderStartTime = 0, // Para logging de performance
  seatDistributionResults = {}, // 🚀 NUEVO: Distribución memoizada
}) {
  // 🔍 PERFORMANCE LOGGING para SectionRenderer
  const sectionRenderStart = performance.now();
  console.log(`🎭 [SectionRenderer] Iniciando render sección: ${element.backendSectionId || element.id}`);
  
  // 🔧 Calcular márgenes adaptativos localmente (igual que en el componente principal)
  const minScreenDimension = Math.min(containerWidth, containerHeight);
  let SEAT_VIEW_MARGIN = 24; // Margen base
  
  if (minScreenDimension < 400) {
    SEAT_VIEW_MARGIN *= 0.5; // Pantalla muy pequeña: 50%
  } else if (minScreenDimension < 500) {
    SEAT_VIEW_MARGIN *= 0.7; // Pantalla pequeña: 70%
  }
  
  // 🔍 TIMING: Obtener datos de sección
  const sectionDataStart = performance.now();
  const shapeMode   = element.shapeMode ?? "rect";
  const color       = element.color     ?? "#3B82F6";
  const label       = element.label     ?? "";
  const sectionData = sections.find((s) => s.eventSectionId === element.backendSectionId);
  const sectionName = sectionData?.sectionTypeName || label;
  const sectionPrice = sectionData?.price ?? 0;
  console.log(`📊 [SectionRenderer] Datos sección obtenidos: ${(performance.now() - sectionDataStart).toFixed(2)}ms`, {
    sectionId: element.backendSectionId,
    sectionName,
    shapeMode,
    price: sectionPrice
  });

  const transformX = (x) => x * scale + offsetX;
  const transformY = (y) => y * scale + offsetY;

  // 🔍 TIMING: Distribución de sillas (OPTIMIZADA CON CACHE)
  const seatDistributionStart = performance.now();
  let seatPositions, rowStructure;
  
  if (seatDistributionResults[element.id]) {
    // Usar resultado cacheado
    const cached = seatDistributionResults[element.id];
    seatPositions = cached.seatPositions;
    rowStructure = cached.rowStructure;
    console.log(`💾 [SectionRenderer] Usando distribución cacheada: ${(performance.now() - seatDistributionStart).toFixed(2)}ms`);
  } else {
    // Fallback: calcular si no está en cache
    const seatResult = distributeSeats(element);
    seatPositions = Array.isArray(seatResult) ? seatResult : seatResult.positions;
    rowStructure = Array.isArray(seatResult) ? [] : (seatResult.rowStructure || []);
    console.log(`⚠️ [SectionRenderer] Fallback distribución: ${(performance.now() - seatDistributionStart).toFixed(2)}ms`);
  }

  // 🔍 TIMING: Construcción de gridInfo
  const gridInfoStart = performance.now();
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
  console.log(`🔢 [SectionRenderer] GridInfo construido: ${(performance.now() - gridInfoStart).toFixed(2)}ms`);

  // 🔍 TIMING: Filtrado de sillas por página (puede ser muy costoso con muchas sillas)
  const pageFilterStart = performance.now();
  const pageFilteredSeats = filterSeatsByPage(
    seatPositions, gridInfo, currentColPage, currentRowPage, inOverviewMode
  );
  console.log(`🔍 [SectionRenderer] Filtrado página: ${(performance.now() - pageFilterStart).toFixed(2)}ms`, {
    totalSeats: seatPositions.length,
    filteredSeats: pageFilteredSeats.length,
    reduction: `${((1 - pageFilteredSeats.length / seatPositions.length) * 100).toFixed(1)}%`
  });

  // 🔍 TIMING: Cálculo background rect
  const backgroundRectStart = performance.now();
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
  console.log(`📦 [SectionRenderer] Background rect: ${(performance.now() - backgroundRectStart).toFixed(2)}ms`);

  // 🔍 TIMING: Obtener sillas de la sección (OPTIMIZADO con índice pre-computado)
  const sectionSeatsStart = performance.now();
  // 🚀 OPTIMIZACIÓN CRÍTICA: Usar índice pre-computado en lugar de filtrar 100k sillas
  const allSectionSeats = seatsBySection[element.backendSectionId] || [];
  console.log(`💺 [SectionRenderer] Sillas de sección obtenidas (OPTIMIZADO): ${(performance.now() - sectionSeatsStart).toFixed(2)}ms`, {
    totalSectionSeats: allSectionSeats.length,
    sectionId: element.backendSectionId,
    usedOptimizedIndex: true
  });

  const elementOffset = calculateSeatOffset(element, orderedLayoutElements);

  // 🔍 TIMING: Creación del path de forma
  const shapePathStart = performance.now();
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
  console.log(`🎨 [SectionRenderer] Path forma creado: ${(performance.now() - shapePathStart).toFixed(2)}ms`);

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

  console.log(`🎭 [SectionRenderer] Sección ${element.backendSectionId} completada: ${(performance.now() - sectionRenderStart).toFixed(2)}ms`);

  return (
    <>
      <Path path={shapePath} color={fillColor}   style="fill" />
      <Path path={shapePath} color={strokeColor} style="stroke" strokeWidth={strokeWidth} />

      {/* 🚀 OPTIMIZACIÓN: Renderizado por lotes usando Path único para 100k+ sillas */}
      {!inOverviewMode && pageFilteredSeats.length > 0 && (() => {
        // 🔍 TIMING: Agrupación de sillas por color (puede ser costosa)
        const batchingStart = performance.now();
        // Agrupar sillas por color para renderizado en lotes
        const seatsByColor = {};
        const myReservationSeats = [];
        
        pageFilteredSeats.forEach(({ pos, realIndex }) => {
          const seat = allSectionSeats[elementOffset + realIndex];
          if (!seat) return;
          
          const isMyReservation = seat.status === "RESERVED" && seat.reservedBy === currentUserId;
          const isReserving = reserving.has(seat.seatId);
          const seatColor = getSeatColor(seat, currentUserId, false, isReserving);
          const seatX = transformX(element.x + pos.x);
          const seatY = transformY(element.y + pos.y);
          
          // Calcular radio adaptativo
          let seatRadius = pos.r * scale;
          if (minScreenDimension < 400) {
            seatRadius *= 0.75;
          } else if (minScreenDimension < 500) {
            seatRadius *= 0.85;
          }
          seatRadius = Math.max(seatRadius, 8);
          
          // Agrupar por color para batching
          if (!seatsByColor[seatColor]) {
            seatsByColor[seatColor] = [];
          }
          seatsByColor[seatColor].push({ x: seatX, y: seatY, r: seatRadius });
          
          // Separar mis reservas para el borde blanco
          if (isMyReservation) {
            myReservationSeats.push({ x: seatX, y: seatY, r: seatRadius });
          }
        });
        
        console.log(`🎨 [SectionRenderer] Agrupación sillas: ${(performance.now() - batchingStart).toFixed(2)}ms`, {
          grupos: Object.keys(seatsByColor).length,
          sillasAgrupadas: Object.values(seatsByColor).reduce((sum, group) => sum + group.length, 0),
          misReservas: myReservationSeats.length
        });

        // 🔍 TIMING: Creación de paths (puede ser costosa)
        const pathCreationStart = performance.now();

        return (
          <React.Fragment>
            {/* Renderizar cada grupo de color como un Path único */}
            {Object.entries(seatsByColor).map(([color, seats]) => {
              const path = Skia.Path.Make();
              seats.forEach(({ x, y, r }) => {
                path.addCircle(x, y, r);
              });
              return (
                <Path
                  key={color}
                  path={path}
                  color={color}
                  style="fill"
                />
              );
            })}
            
            {/* Bordes blancos para mis reservas como Path separado */}
            {myReservationSeats.length > 0 && (() => {
              const borderPath = Skia.Path.Make();
              myReservationSeats.forEach(({ x, y, r }) => {
                borderPath.addCircle(x, y, r);
              });
              
              console.log(`🎨 [SectionRenderer] Paths creados: ${(performance.now() - pathCreationStart).toFixed(2)}ms`);
              
              return (
                <Path
                  key="my-reservations-border"
                  path={borderPath}
                  color="#FFFFFF"
                  style="stroke"
                  strokeWidth={Math.max(1, 2 * scale * 0.8)}
                />
              );
            })()}
          </React.Fragment>
        );
      })()}

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
