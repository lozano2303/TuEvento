/**
 * Emoji de cada elemento del recinto que no se vende (mismo mapa que la web).
 * Un tipo desconocido no lleva emoji.
 */
const DECOR_ICONS = {
  stage: "🎤",
  screen: "📺",
  dance_floor: "💃",
  entrance: "🚪",
  exit: "🏃",
  bar: "🍹",
  restroom: "🚻",
};

export const decorIcon = (type) => DECOR_ICONS[type] ?? "";

/** Texto del elemento: el emoji sobre el nombre, o al lado si el elemento es bajo. */
export function decorText(element) {
  const icon = decorIcon(element.type);
  const label = element.label ?? "";
  if (!icon) return label;
  if (!label) return icon;
  return element.height >= 40 ? `${icon}\n${label}` : `${icon} ${label}`;
}
