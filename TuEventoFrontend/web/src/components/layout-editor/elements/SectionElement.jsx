import { useRef, useEffect } from 'react';
import { Group, Rect, Circle, Text, Transformer } from 'react-konva';
import { computeSeatPositions, computeMinSectionSize, snapToGrid } from '../layoutEditorUtils';

export default function SectionElement({
  element,
  isSelected,
  isGroupLeader,      // Fix 5: true cuando este elemento lidera el drag grupal
  onSelect,
  onChange,
  onGroupDragStart,   // Fix 5: (elementId, startPos) => void
  onGroupDragMove,    // Fix 5: (elementId, currentPos) => void
  onGroupDragEnd,     // Fix 5: (elementId, finalPos)  => void
}) {
  const groupRef = useRef();
  const trRef    = useRef();

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

  // Fix 6: tamaño mínimo calculado según la grilla actual
  const minSize = computeMinSectionSize(element.seatLayout);

  // ── Handlers de drag ──────────────────────────────────────────────────────
  const handleDragStart = (e) => {
    if (onGroupDragStart) {
      onGroupDragStart(element.id, { x: e.target.x(), y: e.target.y() });
    }
  };

  const handleDragMove = (e) => {
    if (onGroupDragMove) {
      onGroupDragMove(element.id, { x: e.target.x(), y: e.target.y() });
    }
  };

  const handleDragEnd = (e) => {
    if (onGroupDragEnd) {
      onGroupDragEnd(element.id, { x: e.target.x(), y: e.target.y() });
    } else {
      // Drag individual (selectedIds.length === 1)
      onChange({
        ...element,
        x: snapToGrid(e.target.x()),
        y: snapToGrid(e.target.y()),
      });
    }
  };

  const handleTransformEnd = () => {
    const node  = groupRef.current;
    const scaleX = node.scaleX();
    const scaleY = node.scaleY();
    node.scaleX(1);
    node.scaleY(1);
    onChange({
      ...element,
      x:      snapToGrid(node.x()),
      y:      snapToGrid(node.y()),
      width:  snapToGrid(Math.max(minSize.width,  node.width()  * scaleX)),
      height: snapToGrid(Math.max(minSize.height, node.height() * scaleY)),
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
        onDragStart={handleDragStart}
        onDragMove={handleDragMove}
        onDragEnd={handleDragEnd}
        onTransformEnd={handleTransformEnd}
      >
        <Rect
          width={element.width}
          height={element.height}
          fill={element.color}
          opacity={0.85}
          cornerRadius={6}
          stroke={isSelected ? '#ffffff' : 'rgba(255,255,255,0.3)'}
          strokeWidth={isSelected ? 2 : 1}
        />

        {seatPositions.map((pos, i) => (
          <Circle
            key={i}
            x={pos.x}
            y={pos.y}
            radius={pos.r}
            fill="rgba(255,255,255,0.75)"
            stroke="rgba(255,255,255,0.4)"
            strokeWidth={0.5}
            listening={false}
          />
        ))}

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
          // Fix 6: tamaño mínimo específico de esta sección
          boundBoxFunc={(oldBox, newBox) => {
            if (newBox.width < minSize.width || newBox.height < minSize.height) return oldBox;
            return newBox;
          }}
        />
      )}
    </>
  );
}
