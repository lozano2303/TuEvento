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
 * 
 * Por ahora: SOLO dibujo estático, sin interacción ni animaciones.
 */
export default function SeatMapCanvas({ layoutData, containerWidth, containerHeight }) {
  if (!layoutData || !layoutData.elements || layoutData.elements.length === 0) {
    return null;
  }

  // Migrar elementos (asegurar que polygonPoints estén en formato nuevo)
  const elements = layoutData.elements.map(migrateElement);

  // Calcular AABB total para determinar zoom inicial
  const totalAABB = computeTotalAABB(elements);
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

  return (
    <View style={styles.container}>
      <Canvas style={{ width: containerWidth, height: containerHeight }}>
        {elements.map((element) => (
          <SectionRenderer
            key={element.id}
            element={element}
            scale={scale}
            offsetX={offsetX}
            offsetY={offsetY}
          />
        ))}
      </Canvas>
    </View>
  );
}

/**
 * Renderiza una sección individual (rect o polygon) con sus sillas.
 */
function SectionRenderer({ element, scale, offsetX, offsetY }) {
  const shapeMode = element.shapeMode ?? "rect";
  const color = element.color ?? "#3B82F6";
  const label = element.label ?? "";

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

  // Color con alpha para el fondo de la sección
  const fillColor = color + "40"; // 25% opacity
  const strokeColor = color;

  return (
    <>
      {/* Fondo de la sección */}
      <Path path={shapePath} color={fillColor} style="fill" />
      
      {/* Borde de la sección */}
      <Path path={shapePath} color={strokeColor} style="stroke" strokeWidth={2 * scale} />

      {/* Sillas */}
      {seatPositions.map((seat, index) => (
        <Circle
          key={index}
          cx={transformX(element.x + seat.x)}
          cy={transformY(element.y + seat.y)}
          r={seat.r * scale}
          color="#FFFFFF"
        />
      ))}

      {/* Label de la sección */}
      {label && (
        <Text
          x={transformX(element.x + element.width / 2)}
          y={transformY(element.y + 15)}
          text={label}
          color={strokeColor}
          size={14 * scale}
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
