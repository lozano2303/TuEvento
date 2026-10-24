/**
 * Textos del panel de administración. El panel admin es solo en español (no usa i18n);
 * las páginas de traducciones son la excepción. Misma firma que t() de i18next:
 * adminText('clave.anidada', { variable, defaultValue }) con interpolación {{variable}}.
 * Los textos viven en adminText.es.json.
 */
import TEXT from './adminText.es.json';

export function adminText(key, vars = {}) {
  const value = key.split('.').reduce((node, k) => (node && typeof node === 'object' ? node[k] : undefined), TEXT);
  if (typeof value !== 'string') return vars.defaultValue ?? key;
  return value.replace(/\{\{(\w+)\}\}/g, (_, name) => (vars[name] ?? ''));
}
