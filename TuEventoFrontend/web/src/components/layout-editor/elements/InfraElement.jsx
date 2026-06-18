import { useRef, useEffect } from 'react';
import { Group, Rect, Text, Transformer } from 'react-konva';
import { snapToGrid } from '../layoutEditorUtils';

const TYPE_ABBR = {
  stage:       '🎭',
  screen:      '🖥',
  dance_floor: '🕺',
  entrance:    '🚪↓',
  exit:        '🚪↑',
  bar:         '🍺',
  restroom:    '🚻',
};

export default function InfraElement({
  element,
  isSelected,
  onSelect,
  onChange,
  onDragMove,          // Fase 1.6: smart guides
  onDragEnd,           // Fase 1.6: smart guides
  onGroupDragStart,
  onGroupDragMove,
  onGroupDragEnd,
}) {
  const groupRef = useRef();
  const trRef    = useRef();

  useEffect(() => {
    if (isSelected && trRef.current && groupRef.current) {
      trRef.current.nodes([groupRef.current]);
      trRef.current.getLayer().batchDraw();
    }
  }, [isSelected]);

  // ── Handlers de drag ──────────────────────────────────────────────────────
  const handleDragStart = (e) => {
    if (onGroupDragStart) onGroupDragStart(element.id, { x: e.target.x(), y: e.target.y() });
  };

  const handleDragMove = (e) => {
    if (onGroupDragMove) {
      onGroupDragMove(element.id, { x: e.target.x(), y: e.target.y() });
    } else if (onDragMove) {
      const result = onDragMove(element.id, { x: e.target.x(), y: e.target.y() });
      if (result && (result.dx !== 0 || result.dy !== 0)) {
        e.target.x(e.target.x() + result.dx);
        e.target.y(e.target.y() + result.dy);
      }
    }
  };

  const handleDragEnd = (e) => {
    if (onGroupDragEnd) {
      onGroupDragEnd(element.id, { x: e.target.x(), y: e.target.y() });
    } else if (onDragEnd) {
      onDragEnd({ ...element, x: snapToGrid(e.target.x()), y: snapToGrid(e.target.y()) });
    } else {
      onChange({ ...element, x: snapToGrid(e.target.x()), y: snapToGrid(e.target.y()) });
    }
  };

  const handleTransformEnd = () => {
    const node   = groupRef.current;
    const scaleX = node.scaleX();
    const scaleY = node.scaleY();
    node.scaleX(1);
    node.scaleY(1);
    onChange({
      ...element,
      x:        snapToGrid(node.x()),
      y:        snapToGrid(node.y()),
      width:    snapToGrid(Math.max(40, node.width()  * scaleX)),
      height:   snapToGrid(Math.max(30, node.height() * scaleY)),
      rotation: node.rotation(),
    });
  };

  const abbr          = TYPE_ABBR[element.type] ?? element.type.slice(0, 3).toUpperCase();
  const iconFontSize  = Math.min(element.width, element.height) * 0.3;
  const labelFontSize = Math.max(10, Math.min(13, element.width / 10));

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
          opacity={0.9}
          cornerRadius={4}
          stroke={isSelected ? '#ffffff' : 'rgba(255,255,255,0.25)'}
          strokeWidth={isSelected ? 2 : 1}
        />

        <Text
          x={0}
          y={element.height / 2 - iconFontSize - 2}
          width={element.width}
          text={abbr}
          fontSize={Math.max(10, iconFontSize)}
          align="center"
          listening={false}
        />

        <Text
          x={0}
          y={element.height / 2 + 4}
          width={element.width}
          text={element.label}
          fontSize={labelFontSize}
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
            if (newBox.width < 40 || newBox.height < 30) return oldBox;
            return newBox;
          }}
        />
      )}
    </>
  );
}
