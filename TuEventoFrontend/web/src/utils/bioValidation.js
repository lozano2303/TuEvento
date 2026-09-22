/**
 * bioValidation.js — validación client-side de lenguaje inapropiado en bio.
 *
 * Mantener sincronizado con forbidden-words-es.txt del backend y con
 * normaliseBio() en ValidationUtils.java.
 * El backend sigue siendo la fuente de verdad definitiva;
 * este módulo solo da feedback inmediato al usuario.
 */

// ── Lista de palabras prohibidas ──────────────────────────────────────────────
// Agrega/quita aquí Y en el .txt del backend para mantener paridad.
// NOTA: la lista usa formas ya normalizadas (sin tildes, k→c, z→s, etc.)
const FORBIDDEN_WORDS = new Set([
  'tonto', 'tonta', 'bobo', 'boba',
  'estupido', 'estupida',
  'idiota', 'imbecil',
  'pendejo', 'pendeja',
  'malparido', 'malparida',
  'gonorrea',
  'hijueputa', 'hijuepuerca', 'hdp',
  'marica', 'maricon',        // "marikon" normaliza a "maricon" vía k→c
  'gilipola', 'gilipollas',   // "gilipolla" colapsa ll→l tras deduplicar
  'subnormal',
  'retrasado', 'retrasada',
  'mongolo', 'mogolico', 'mogolica',
  'mentecato', 'mentecata',
  'cretino', 'cretina',
  'baboso', 'babosa',
  'bruto', 'bruta',
  'animal', 'bestia',
  // Abreviaturas
  'hp',
  // Contenido sexual explícito
  'porno', 'pornografia',
  'sexo',
  'puteria', 'puton', 'putona',
]);

/**
 * Normaliza el texto antes de comparar, replicando la lógica del backend.
 * Pasos (el orden importa):
 *   1. Minúsculas
 *   2. Quita tildes / diacríticos (NFD + eliminar combining marks)
 *   3. Sustituciones multi-char: ph→f, ck→c
 *   4. Leetspeak + fonética: 0→o 1→i 3→e 4→a 5→s @→a k→c z→s w→v y→i
 *   5. Elimina separadores inline: guiones, puntos, guiones bajos entre letras
 *   6. Colapsa letras aisladas separadas por espacios (t o n t o → tonto)
 *   7. Colapsa letras repetidas consecutivas (tooontooo → tonto)
 */
function normaliseBio(text) {
  let s = text.toLowerCase();

  // 2. Quitar diacríticos
  s = s.normalize('NFD').replace(/\p{Mn}/gu, '');

  // 3. Multi-char fonética
  s = s.replace(/ph/g, 'f').replace(/ck/g, 'c');

  // 4. Leetspeak + fonética single-char
  s = s
    .replace(/0/g, 'o')
    .replace(/1/g, 'i')
    .replace(/3/g, 'e')
    .replace(/4/g, 'a')
    .replace(/5/g, 's')
    .replace(/@/g, 'a')
    .replace(/k/g, 'c')
    .replace(/z/g, 's')
    .replace(/w/g, 'v')
    .replace(/y/g, 'i');

  // 5. Separadores inline entre letras (t-o-n-t-o → tonto)
  s = s.replace(/(?<=[a-z])[.\-_](?=[a-z])/g, '');

  // 6. Colapsar letras individuales separadas por espacio (t o n t o → tonto)
  //    Repite hasta que no haya más cambios (cubre múltiples espacios)
  let prev;
  do {
    prev = s;
    s = s.replace(/(?<![a-z])([a-z]) (?=[a-z]\b)/g, '$1');
    s = s.replace(/(?<![a-z])([a-z]) (?=[a-z] )/g, '$1');
  } while (s !== prev);

  // 7. Colapsar letras repetidas consecutivas (aaa→a, tooonto→tonto)
  s = s.replace(/(.)\1+/g, '$1');

  return s;
}

/**
 * Valida el texto de la bio contra la lista de palabras prohibidas.
 * Devuelve true si el texto es apropiado, false si contiene alguna palabra prohibida.
 * Comparación de PALABRA COMPLETA (split por espacios).
 *
 * @param {string} bio
 * @returns {boolean} true = limpio, false = contiene lenguaje inapropiado
 */
export function isBioClean(bio) {
  if (!bio || !bio.trim()) return true;
  const words = normaliseBio(bio).split(/\s+/).filter(Boolean);
  return !words.some(w => FORBIDDEN_WORDS.has(w));
}
