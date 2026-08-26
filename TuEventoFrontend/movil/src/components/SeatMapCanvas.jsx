import { useState } from "react";
import { View, StyleSheet } from "react-native";
import { Canvas, Path, Circle, Text, rect, Skia, useFont } from "@shopify/react-native-skia";
import {
  migrateElement,
  distributeSeats,
  computeTotalAABB,
  flattenPolygonForFill,
} from "../utils/layoutUtils";

/**
 * Canvas de Skia que renderiza el mapa de sillas de un evento.
 * 
 * Props:
 * - layoutData: objeto con { canvasWidth, canvasHeight, elements }
 * - containerWidth: ancho del contenedor (del onLayout)
 * - containerHeight: alto del contenedor (del onLayout)
 * - focusedSectionId: ID de la sección enfocada (null = vista general)
 * - sections: Array de EventSectionResponse con datos del backend (nombre, precio)
 * 
 * Por ahora: SOLO dibujo estático, sin interacción.
 * - Vista general: secciones visibles, sillas NO visibles
 * - Vista filtrada: solo sección seleccionada, sillas visibles pero NO clickeables
 */
export default function SeatMapCanvas({ layoutData, containerWidth, containerHeight, focusedSectionId = null, sections = [] }) {
  if (!layoutData || !layoutData.elements || layoutData.elements.length === 0) {
    return null;
  }

  // Migrar elementos (asegurar que polygonPoints estén en formato nuevo)
  const elements = layoutData.elements.map(migrateElement);

  // Filtrar elementos si hay una sección enfocada
  const visibleElements = focusedSectionId
    ? elements.filter((el) => el.backendSectionId === focusedSectionId)
    : elements;

  // Calcular AABB total para determinar zoom inicial (de los elementos visibles)
  const totalAABB = computeTotalAABB(visibleElements);
  const contentWidth = totalAABB.maxX - totalAABB.minX;
  const contentHeight = totalAABB.maxY - totalAABB.minY;

  // Calcular zoom para que todo el contenido quepa con un pequeño padding
  const PADDING = 40;
  const availWidth = containerWidth - PADDING * 2;
  const availHeight = containerHeight - PADDING * 2;

  const scaleX = availWidth / contentWidth;
  const scaleY = availHeight / contentHeight;
  const scale = Math.min(scaleX, scaleY, 1); // No hacer zoom-in, solo zoom-out

  // Calcular offset para centrar el contenido
  const scaledWidth = contentWidth * scale;
  const scaledHeight = contentHeight * scale;
  const offsetX = (containerWidth - scaledWidth) / 2 - totalAABB.minX * scale;
  const offsetY = (containerHeight - scaledHeight) / 2 - totalAABB.minY * scale;

  // Determinar si estamos en vista general o filtrada
  const inOverviewMode = focusedSectionId === null;

  return (
    <View style={styles.container}>
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
          />
        ))}
      </Canvas>
    </View>
  );
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
 * - Sillas visibles pero NO clickeables
 * - Muestra nombre y precio de la sección
 */
function SectionRenderer({ element, scale, offsetX, offsetY, inOverviewMode, sections }) {
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
  const seatPositions = distributeSeats(element);

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
      {!inOverviewMode && seatPositions.map((seat, index) => (
        <Circle
          key={index}
          cx={transformX(element.x + seat.x)}
          cy={transformY(element.y + seat.y)}
          r={seat.r * scale}
          color="#FFFFFF"
        />
      ))}

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

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#1A1A1A",
  },
});
