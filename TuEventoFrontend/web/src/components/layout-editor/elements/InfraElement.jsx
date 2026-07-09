import { useRef, useEffect } from 'react';
import { Group, Rect, Text, Transformer } from 'react-konva';
import { snapToGrid, getElementAABB, CANVAS_MARGIN } from '../layoutEditorUtils';

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
  onDragMove,
  onDragEnd,
  onGroupDragStart,
  onGroupDragMove,
  onGroupDragEnd,
  canvasSizeRef,
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
    const node = e.target;
    const cs   = canvasSizeRef?.current ?? { width: 9999, height: 9999 };

    const clamp = (px, py) => {
      const origX = node.x(), origY = node.y();
      node.x(px); node.y(py);
      const r = node.getClientRect({ relativeTo: node.getLayer() });
      node.x(origX); node.y(origY);
      let ox = px, oy = py;
      if (r.x < 0)                    ox += -r.x;
      if (r.y < 0)                    oy += -r.y;
      if (r.x + r.width  > cs.width)  ox -= (r.x + r.width)  - cs.width;
      if (r.y + r.height > cs.height) oy -= (r.y + r.height) - cs.height;
      return { x: ox, y: oy };
    };

    const raw = { x: node.x(), y: node.y() };
    const clamped = clamp(raw.x, raw.y);
    if (clamped.x !== raw.x) node.x(clamped.x);
    if (clamped.y !== raw.y) node.y(clamped.y);

    if (onGroupDragMove) {
      onGroupDragMove(element.id, { x: clamped.x, y: clamped.y });
    } else if (onDragMove) {
      const result = onDragMove(element.id, { x: clamped.x, y: clamped.y });
      if (result && (result.dx !== 0 || result.dy !== 0)) {
        const afterSnap = clamp(node.x() + result.dx, node.y() + result.dy);
        node.x(afterSnap.x);
        node.y(afterSnap.y);
      }
    }
  };

  const handleDragEnd = (e) => {
    const node = e.target;
    const cs   = canvasSizeRef?.current ?? { width: 9999, height: 9999 };
    const r    = node.getClientRect({ relativeTo: node.getLayer() });
    let fx = node.x(), fy = node.y();
    if (r.x < 0)                    fx += -r.x;
    if (r.y < 0)                    fy += -r.y;
    if (r.x + r.width  > cs.width)  fx -= (r.x + r.width)  - cs.width;
    if (r.y + r.height > cs.height) fy -= (r.y + r.height) - cs.height;
    const finalX = snapToGrid(fx);
    const finalY = snapToGrid(fy);
    if (onGroupDragEnd) {
      onGroupDragEnd(element.id, { x: finalX, y: finalY });
    } else if (onDragEnd) {
      onDragEnd({ ...element, x: finalX, y: finalY });
    } else {
      onChange({ ...element, x: finalX, y: finalY });
    }
  };

  const handleTransformEnd = () => {
    const node   = groupRef.current;
    const scaleX = node.scaleX();
    const scaleY = node.scaleY();
    node.scaleX(1);
    node.scaleY(1);
    const newW = snapToGrid(Math.max(40, node.width()  * scaleX));
    const newH = snapToGrid(Math.max(30, node.height() * scaleY));
    const cs   = canvasSizeRef?.current ?? { width: 9999, height: 9999 };
    const r    = node.getClientRect({ relativeTo: node.getLayer() });
    let px = node.x(), py = node.y();
    if (r.x < 0)                    px += -r.x;
    if (r.y < 0)                    py += -r.y;
    if (r.x + r.width  > cs.width)  px -= (r.x + r.width)  - cs.width;
    if (r.y + r.height > cs.height) py -= (r.y + r.height) - cs.height;
    onChange({
      ...element,
      x:        snapToGrid(px),
      y:        snapToGrid(py),
      width:    newW,
      height:   newH,
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
          rotationSnaps={[0, 45, 90, 135, 180, 225, 270, 315]}
          rotationSnapTolerance={10}
          rotateAnchorOffset={28}
          rotateAnchorCursor="grab"
          enabledAnchors={[
            'top-left', 'top-right',
            'bottom-left', 'bottom-right',
            'middle-left', 'middle-right',
            'top-center', 'bottom-center',
          ]}
          anchorStyleFunc={(anchor) => {
            if (anchor.hasName('rotater')) {
              anchor.cornerRadius(10);
              anchor.fill('#818cf8');
              anchor.stroke('#6366f1');
              anchor.strokeWidth(2);
              anchor.width(18);
              anchor.height(18);
              anchor.offsetX(9);
              anchor.offsetY(9);
            }
          }}
          boundBoxFunc={(oldBox, newBox) => {
            if (newBox.width < 40 || newBox.height < 30) return oldBox;
            return newBox;
          }}
        />
      )}
    </>
  );
}
