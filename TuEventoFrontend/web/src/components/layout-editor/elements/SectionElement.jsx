import { useRef, useEffect } from 'react';
import { Group, Rect, Circle, Text, Transformer } from 'react-konva';
import { computeSeatPositions, snapToGrid } from '../layoutEditorUtils';

export default function SectionElement({
  element,
  isSelected,
  onChange,
  onSelect,
}) {
  const groupRef = useRef();
  const trRef = useRef();

  useEffect(() => {
    if (isSelected && trRef.current && groupRef.current) {
      trRef.current.nodes([groupRef.current]);
      trRef.current.getLayer().batchDraw();
    }
  }, [isSelected]);

  const seatPositions = computeSeatPositions(
    element.width,
    element.height,
    element.seatLayout
  );
  const totalSeats = element.seatLayout
    ? element.seatLayout.rows * element.seatLayout.cols
    : 0;
  const showGrid = seatPositions.length > 0;

  const handleDragEnd = (e) => {
    onChange({
      ...element,
      x: snapToGrid(e.target.x()),
      y: snapToGrid(e.target.y()),
    });
  };

  const handleTransformEnd = () => {
    const node = groupRef.current;
    const scaleX = node.scaleX();
    const scaleY = node.scaleY();
    node.scaleX(1);
    node.scaleY(1);
    onChange({
      ...element,
      x: snapToGrid(node.x()),
      y: snapToGrid(node.y()),
      width: snapToGrid(Math.max(60, node.width() * scaleX)),
      height: snapToGrid(Math.max(40, node.height() * scaleY)),
      rotation: node.rotation(),
    });
  };

  return (
    <>
      <Group
        ref={groupRef}
        x={element.x}
        y={element.y}
        width={element.width}
        height={element.height}
        rotation={element.rotation ?? 0}
        draggable
        onClick={onSelect}
        onTap={onSelect}
        onDragEnd={handleDragEnd}
        onTransformEnd={handleTransformEnd}
      >
        {/* Fondo de la sección */}
        <Rect
          width={element.width}
          height={element.height}
          fill={element.color}
          opacity={0.85}
          cornerRadius={6}
          stroke={isSelected ? '#ffffff' : 'rgba(255,255,255,0.3)'}
          strokeWidth={isSelected ? 2 : 1}
        />

        {/* Grilla de sillas */}
        {showGrid &&
          seatPositions.map((pos, i) => (
            <Circle
              key={i}
              x={pos.x}
              y={pos.y}
              radius={pos.r}
              fill="rgba(255,255,255,0.75)"
              stroke="rgba(255,255,255,0.4)"
              strokeWidth={0.5}
            />
          ))}

        {/* Badge cuando la grilla es demasiado pequeña para mostrarse */}
        {!showGrid && totalSeats > 0 && (
          <Text
            x={4}
            y={4}
            text={`${totalSeats} asientos`}
            fontSize={10}
            fill="rgba(255,255,255,0.7)"
            fontStyle="italic"
          />
        )}

        {/* Label centrado */}
        <Text
          x={0}
          y={element.height / 2 - 8}
          width={element.width}
          text={element.label}
          fontSize={13}
          fontStyle="bold"
          fill="#ffffff"
          align="center"
          listening={false}
        />
      </Group>

      {isSelected && (
        <Transformer
          ref={trRef}
          rotateEnabled
          enabledAnchors={[
            'top-left', 'top-right',
            'bottom-left', 'bottom-right',
            'middle-left', 'middle-right',
            'top-center', 'bottom-center',
          ]}
          boundBoxFunc={(oldBox, newBox) => {
            if (newBox.width < 60 || newBox.height < 40) return oldBox;
            return newBox;
          }}
        />
      )}
    </>
  );
}
