import { useState, useEffect, useCallback, useMemo } from 'react';
import { X, RotateCcw, ChevronDown, Check, AlertCircle, Loader2, Info } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';
import {
  customizeTheme,
  resetCustomization,
  getCustomizationLog,
} from '../../services/themeService';
import { contrastRatio, contrastLevel, suggestAccent, WCAG_AA } from '../../utils/colorUtils';

// ── Swatch palettes per theme ─────────────────────────────────────────────────
const THEME_SWATCHES = {
  DARK: [
    '#0D0D0D','#1A1A1A','#2C2C2C','#3D3D3D','#4F4F4F',
    '#616161','#757575','#8E8E8E','#B0B0B0','#BDBDBD',
    '#E0E0E0','#EEEEEE','#FFFFFF','#CF6679','#4CAF50',
  ],
  LIGHT: [
    '#FFFFFF','#FAFAFA','#F5F5F5','#EEEEEE','#E0E0E0',
    '#BDBDBD','#9E9E9E','#757575','#616161','#424242',
    '#212121','#D32F2F','#388E3C','#1565C0','#F57F17',
  ],
  PASTEL: [
    '#FFF9FB','#FFE4EE','#FFD6E7','#F8BBD9','#F48FB1',
    '#F06292','#CE93D8','#BA68C8','#80DEEA','#80CBC4',
    '#A5D6A7','#E6EE9C','#FFF59D','#FFCC80','#E57373',
  ],
  VIBRANT: [
    '#FF1744','#D50000','#FF6D00','#FFEA00','#00E676',
    '#00B0FF','#651FFF','#FF4081','#F50057','#69F0AE',
    '#40C4FF','#E040FB','#FF6E40','#CCFF90','#FFD740',
  ],
  NATURE: [
    '#F1F8E9','#DCEDC8','#C5E1A5','#AED581','#8BC34A',
    '#7CB342','#558B2F','#33691E','#1B5E20','#8D6E63',
    '#795548','#6D4C41','#BF360C','#FF8F00','#F9A825',
  ],
  OCEAN: [
    '#E3F2FD','#BBDEFB','#90CAF9','#64B5F6','#42A5F5',
    '#1E88E5','#1565C0','#0D47A1','#01579B','#006064',
    '#00838F','#00ACC1','#00BCD4','#80DEEA','#B2EBF2',
  ],
  SUNSET: [
    '#FFF3E0','#FFE0B2','#FFCC80','#FFB74D','#FFA726',
    '#FB8C00','#F57C00','#E65100','#BF360C','#FF8A65',
    '#FF7043','#FF5722','#E64A19','#D84315','#C62828',
  ],
  PRINCIPAL: [],
};

// ── Token groups ──────────────────────────────────────────────────────────────
const PROPERTY_GROUPS = [
  { label: 'Fondos',    icon: '▣', properties: ['background', 'surface', 'surfaceAlt'] },
  { label: 'Primarios', icon: '◉', properties: ['primary', 'primaryDark', 'accent'] },
  { label: 'Textos',    icon: 'T', properties: ['textPrimary', 'textSecondary', 'textMuted'] },
  { label: 'Estados',   icon: '!', properties: ['error', 'errorBg', 'success', 'successBg'] },
];

// ── Spanish labels ────────────────────────────────────────────────────────────
const PROPERTY_LABELS = {
  background:    'Fondo principal',
  surface:       'Superficie',
  surfaceAlt:    'Superficie alternativa',
  primary:       'Color primario',
  primaryDark:   'Primario oscuro',
  accent:        'Acento',
  textPrimary:   'Texto principal',
  textSecondary: 'Texto secundario',
  textMuted:     'Texto sutil',
  error:         'Error',
  errorBg:       'Fondo de error',
  success:       'Éxito',
  successBg:     'Fondo de éxito',
};

/**
 * Pairs (foreground-token, background-token) that should meet WCAG AA.
 * Keys are the token being edited; value is the background to check against.
 * Tokens that ARE backgrounds (errorBg, successBg, surfaceAlt…) are omitted
 * because checking a background against itself is meaningless.
 */
const CONTRAST_PAIRS = {
  primary:       'background',
  primaryDark:   'background',
  accent:        'background',
  textPrimary:   'background',
  textSecondary: 'surface',
  textMuted:     'surface',
  error:         'background',
  success:       'background',
};

// ── Validation ────────────────────────────────────────────────────────────────
const HEX_REGEX = /^#([A-Fa-f0-9]{3,4}|[A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$/;
const isValidColor = (v) => HEX_REGEX.test(v) || /^rgba?\(/.test(v);

function toInputHex(color) {
  if (!color) return '#000000';
  const m3 = color.match(/^#([0-9a-f]{3})$/i);
  if (m3) {
    const [r, g, b] = m3[1].split('').map(c => c + c);
    return `#${r}${g}${b}`;
  }
  if (/^#[0-9a-f]{6}$/i.test(color)) return color;
  if (/^#[0-9a-f]{8}$/i.test(color)) return color.slice(0, 7);
  return '#7c3aed';
}

// ── ContrastBadge — shows ratio + WCAG level inline ─────────────────────────
function ContrastBadge({ ratio }) {
  if (ratio === null) return null;
  const level = contrastLevel(ratio);
  const passing = level !== 'fail';

  const label = passing
    ? level === 'aaa' ? 'AAA' : level === 'aa' ? 'AA' : 'AA Large'
    : 'Sin contraste';

  return (
    <span
      className={`inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full border
                  ${passing
                    ? 'bg-success/10 text-success border-success/30'
                    : 'bg-error/10 text-error border-error/30'}`}
      title={`Relación de contraste: ${ratio.toFixed(2)}:1`}
    >
      {passing
        ? <Check className="w-2.5 h-2.5" />
        : <AlertCircle className="w-2.5 h-2.5" />}
      {ratio.toFixed(1)}:1 {label}
    </span>
  );
}

// ── ContrastWarning — advisory block shown when contrast fails ───────────────
function ContrastWarning({ property, ratio, suggestedColor, onApplySuggestion }) {
  if (ratio === null || ratio >= WCAG_AA) return null;

  return (
    <div className="tcp-contrast-warning space-y-2">
      <div className="flex items-start gap-2">
        <AlertCircle className="w-3.5 h-3.5 text-amber-400 flex-shrink-0 mt-0.5" />
        <p className="text-[11px] text-amber-300 leading-snug">
          <span className="font-semibold">Contraste bajo</span> — este color puede ser difícil
          de leer sobre el fondo actual ({ratio.toFixed(2)}:1, mínimo WCAG AA 4.5:1).
          Puedes guardarlo igualmente.
        </p>
      </div>
      {suggestedColor && (
        <div className="flex items-center gap-2 pt-0.5">
          <span className="text-[10px] text-textMuted">Sugerido:</span>
          <button
            onClick={() => onApplySuggestion(suggestedColor)}
            className="flex items-center gap-1.5 text-[11px] font-semibold
                       hover:opacity-80 transition-opacity"
            title={`Aplicar ${suggestedColor}`}
          >
            <span
              className="w-4 h-4 rounded border border-white/20 flex-shrink-0"
              style={{ background: suggestedColor }}
            />
            <span className="font-mono text-textSecondary">{suggestedColor}</span>
            <span className="text-primary">← usar este</span>
          </button>
        </div>
      )}
    </div>
  );
}

// ── ContextPreview ────────────────────────────────────────────────────────────
function ContextPreview({ property, color }) {
  const isBg      = ['background', 'surface', 'surfaceAlt'].includes(property);
  const isPrimary = ['primary', 'primaryDark', 'accent'].includes(property);
  const isText    = ['textPrimary', 'textSecondary', 'textMuted'].includes(property);

  if (isBg) return (
    <div className="w-14 h-10 rounded-lg border border-white/10 flex flex-col justify-center gap-1 px-2 flex-shrink-0"
      style={{ background: color }}>
      <div className="h-1.5 rounded-full bg-white/30 w-full" />
      <div className="h-1.5 rounded-full bg-white/20 w-2/3" />
    </div>
  );
  if (isPrimary) return (
    <div className="w-14 h-10 rounded-lg flex items-center justify-center text-sm font-bold text-white flex-shrink-0"
      style={{ background: color }}>
      Aa
    </div>
  );
  if (isText) return (
    <div className="w-14 h-10 rounded-lg flex items-center justify-center bg-surfaceAlt flex-shrink-0">
      <span className="text-sm font-bold" style={{ color }}>Texto</span>
    </div>
  );
  return (
    <div className="w-14 h-10 rounded-lg flex items-center justify-center border flex-shrink-0"
      style={{ background: color + '33', borderColor: color + '88' }}>
      <span className="text-[11px] font-bold" style={{ color }}>Estado</span>
    </div>
  );
}

// ── PropertyRow ───────────────────────────────────────────────────────────────
function PropertyRow({ property, currentColor, isCustomized, contrastInfo, onEdit }) {
  const hasProblem = contrastInfo?.ratio !== null && contrastInfo?.ratio < WCAG_AA;
  return (
    <button onClick={() => onEdit(property)} className="tcp-token-row group">
      {/* Colour dot */}
      <div
        className="w-8 h-8 rounded-lg flex-shrink-0 border border-white/15 shadow-sm"
        style={{ background: currentColor }}
      />
      {/* Label + hex value */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-textPrimary truncate leading-tight">
          {PROPERTY_LABELS[property] ?? property}
        </p>
        <p className="text-xs text-textMuted font-mono mt-0.5 leading-tight">{currentColor}</p>
      </div>
      {/* Contrast warning dot — only shown on collapsed rows */}
      {hasProblem && (
        <span
          title={`Contraste bajo: ${contrastInfo.ratio.toFixed(2)}:1`}
          className="w-4 h-4 rounded-full bg-amber-400/20 border border-amber-400/50
                     flex items-center justify-center flex-shrink-0"
        >
          <AlertCircle className="w-2.5 h-2.5 text-amber-400" />
        </span>
      )}
      {/* Custom badge */}
      {isCustomized && <span className="tcp-badge-custom">Custom</span>}
      {/* Arrow */}
      <ChevronDown
        className="w-4 h-4 text-textMuted -rotate-90 flex-shrink-0
                   group-hover:text-primary transition-colors duration-150"
      />
    </button>
  );
}

// ── LogEntry ──────────────────────────────────────────────────────────────────
function LogEntry({ entry }) {
  const date = entry.createdAt
    ? new Date(entry.createdAt).toLocaleString('es-ES', {
        day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit',
      })
    : '';
  const isUpdate = entry.action === 'UPDATE';

  return (
    <div className="tcp-log-entry">
      <div className={`tcp-log-icon ${isUpdate ? 'tcp-log-icon--update' : 'tcp-log-icon--reset'}`}>
        {isUpdate
          ? <div className="w-3 h-3 rounded-sm" style={{ background: entry.newValue ?? '#888' }} />
          : <RotateCcw className="w-3.5 h-3.5 text-textMuted" />}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-semibold text-textPrimary leading-tight">
          {PROPERTY_LABELS[entry.property] ?? entry.property}
        </p>
        {isUpdate ? (
          <div className="flex items-center gap-1.5 mt-1 flex-wrap">
            <span className="tcp-log-dot" style={{ background: entry.oldValue ?? '#888' }} title={entry.oldValue} />
            <span className="text-textMuted text-[11px] leading-none">→</span>
            <span className="tcp-log-dot" style={{ background: entry.newValue ?? '#888' }} title={entry.newValue} />
            <span className="text-[11px] font-mono text-textMuted">{entry.newValue}</span>
          </div>
        ) : (
          <p className="text-[11px] text-textMuted mt-0.5">Restablecido al valor base</p>
        )}
      </div>
      <span className="text-[10px] text-textMuted flex-shrink-0 mt-0.5 whitespace-nowrap">{date}</span>
    </div>
  );
}

// ╔══════════════════════════════════════════════════════════════════════════╗
// ║  ThemeCustomizePanel — main export                                       ║
// ╚══════════════════════════════════════════════════════════════════════════╝
export default function ThemeCustomizePanel({ themeName, onClose }) {
  const { palette, applyPalette, refreshPalette } = useTheme();

  if (themeName === 'PRINCIPAL') {
    return (
      <PanelShell onClose={onClose}>
        <div className="flex flex-col items-center justify-center h-full gap-4 p-8 text-center">
          <span className="text-5xl">⭐</span>
          <h3 className="text-lg font-bold text-textPrimary">Tema Oficial</h3>
          <p className="text-sm text-textSecondary max-w-xs">
            El tema PRINCIPAL es la identidad oficial de Tu Evento y no puede ser personalizado.
          </p>
        </div>
      </PanelShell>
    );
  }

  // ── State ─────────────────────────────────────────────────────────────────
  const [customizedProps, setCustomizedProps] = useState(new Set());
  const [log, setLog]                         = useState([]);
  const [logOpen, setLogOpen]                 = useState(false);
  const [logLoading, setLogLoading]           = useState(false);
  const [resetAllLoading, setResetAllLoading] = useState(false);
  const [resetAllError, setResetAllError]     = useState(null);
  const [globalError, setGlobalError]         = useState(null);

  const [editing, setEditing]           = useState(null);
  const [hexInput, setHexInput]         = useState('');
  const [hexError, setHexError]         = useState(false);
  const [previewColor, setPreviewColor] = useState('#000000');
  const [saving, setSaving]             = useState(false);
  const [saveError, setSaveError]       = useState(null);
  const [resetting, setResetting]       = useState(null);

  const swatches = THEME_SWATCHES[themeName] ?? THEME_SWATCHES.DARK;

  // ── Contrast computation for the currently open editor ─────────────────
  const editorContrastInfo = useMemo(() => {
    if (!editing || !palette) return { ratio: null, suggested: null };
    const bgToken = CONTRAST_PAIRS[editing];
    if (!bgToken) return { ratio: null, suggested: null }; // no check needed (e.g. surfaceAlt)
    const bgColor = palette[bgToken] ?? '#000000';
    const ratio   = contrastRatio(previewColor, bgColor);
    const suggested = (editing === 'accent' && ratio !== null && ratio < WCAG_AA)
      ? suggestAccent(palette.primary ?? previewColor, bgColor)
      : null;
    return { ratio, suggested };
  }, [editing, previewColor, palette]);

  // ── Per-row contrast info for collapsed rows (warning dot) ─────────────
  const rowContrastMap = useMemo(() => {
    if (!palette) return {};
    const map = {};
    for (const [token, bgToken] of Object.entries(CONTRAST_PAIRS)) {
      const color   = palette[token];
      const bgColor = palette[bgToken];
      if (!color || !bgColor) continue;
      map[token] = { ratio: contrastRatio(color, bgColor) };
    }
    return map;
  }, [palette]);

  // ── Load log + derive customised props ───────────────────────────────────
  useEffect(() => {
    const load = async () => {
      try {
        setLogLoading(true);
        const data = await getCustomizationLog();
        if (!Array.isArray(data)) return;
        setLog(data);
        const customized = new Set();
        data.forEach(e => {
          if (e.action === 'UPDATE') customized.add(e.property);
          if (e.action === 'RESET')  customized.delete(e.property);
        });
        setCustomizedProps(customized);
      } catch { /* non-critical */ }
      finally { setLogLoading(false); }
    };
    load();
  }, []);

  // ── Editor open/close ────────────────────────────────────────────────────
  const openEditor = useCallback((property) => {
    const current = palette?.[property] ?? '#000000';
    setEditing(property);
    setPreviewColor(current);
    setHexInput(current);
    setHexError(false);
    setSaveError(null);
  }, [palette]);

  const closeEditor = () => {
    setEditing(null);
    applyPalette(palette);
  };

  // ── Colour changes → live preview ────────────────────────────────────────
  const commitColor = useCallback((value) => {
    setPreviewColor(value);
    setHexError(false);
    applyPalette({ ...palette, [editing]: value });
  }, [palette, editing, applyPalette]);

  const handleHexChange = (value) => {
    setHexInput(value);
    if (isValidColor(value)) commitColor(value);
    else setHexError(true);
  };

  const handlePickerChange = (value) => {
    setHexInput(value);
    commitColor(value);
  };

  const handleSwatchClick = (hex) => {
    setHexInput(hex);
    commitColor(hex);
  };

  const handleApplySuggestion = (hex) => {
    setHexInput(hex);
    commitColor(hex);
  };

  // ── Save ─────────────────────────────────────────────────────────────────
  const handleSave = async () => {
    if (hexError || !editing) return;
    setSaving(true);
    setSaveError(null);
    try {
      const data = await customizeTheme(editing, previewColor);
      if (data?.palette) applyPalette(data.palette);
      setCustomizedProps(prev => new Set(prev).add(editing));
      setLog(prev => [{
        logId: Date.now(),
        action: 'UPDATE',
        property: editing,
        oldValue: palette?.[editing],
        newValue: previewColor,
        createdAt: new Date().toISOString(),
      }, ...prev]);
      setEditing(null);
    } catch {
      setSaveError('No se pudo guardar. Intenta de nuevo.');
    } finally {
      setSaving(false);
    }
  };

  // ── Reset single ─────────────────────────────────────────────────────────
  const handleReset = async (property) => {
    setResetting(property);
    try {
      const data = await resetCustomization(property);
      if (data?.palette) applyPalette(data.palette);
      setCustomizedProps(prev => { const n = new Set(prev); n.delete(property); return n; });
      setLog(prev => [{
        logId: Date.now(), action: 'RESET', property,
        createdAt: new Date().toISOString(),
      }, ...prev]);
      if (editing === property) setEditing(null);
    } catch {
      setGlobalError(`No se pudo restablecer "${PROPERTY_LABELS[property]}".`);
      setTimeout(() => setGlobalError(null), 3000);
    } finally {
      setResetting(null);
    }
  };

  // ── Reset all ────────────────────────────────────────────────────────────
  const handleResetAll = async () => {
    if (customizedProps.size === 0) return;
    setResetAllLoading(true);
    setResetAllError(null);
    try {
      let lastData = null;
      for (const prop of [...customizedProps]) lastData = await resetCustomization(prop);
      if (lastData?.palette) applyPalette(lastData.palette);
      else await refreshPalette();
      setCustomizedProps(new Set());
      setEditing(null);
    } catch {
      setResetAllError('Error al restablecer todos los colores.');
    } finally {
      setResetAllLoading(false);
    }
  };

  // ─────────────────────────────────────────────────────────────────────────
  // RENDER
  // ─────────────────────────────────────────────────────────────────────────
  return (
    <PanelShell onClose={onClose}>

      {/* ── Header ─────────────────────────────────────────────────────── */}
      <div className="tcp-header px-5 pt-5 pb-4 flex items-center justify-between flex-shrink-0 gap-3">
        <div className="min-w-0">
          <h2 className="text-base font-bold text-textPrimary leading-tight">Personalizar tema</h2>
          <p className="text-xs text-textMuted mt-0.5 font-medium tracking-wide uppercase">{themeName}</p>
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
          {customizedProps.size > 0 && (
            <button
              onClick={handleResetAll}
              disabled={resetAllLoading}
              className="flex items-center gap-1.5 text-xs text-error border border-error/40
                         px-3 py-1.5 rounded-lg hover:bg-error/10 transition-all
                         disabled:opacity-50 whitespace-nowrap"
            >
              {resetAllLoading
                ? <Loader2 className="w-3.5 h-3.5 animate-spin" />
                : <RotateCcw className="w-3.5 h-3.5" />}
              Restablecer todo
            </button>
          )}
          <button onClick={onClose} aria-label="Cerrar panel"
            className="w-8 h-8 rounded-full flex items-center justify-center
                       bg-surfaceAlt hover:bg-primary/20 text-textMuted
                       hover:text-textPrimary transition-all flex-shrink-0">
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* ── Error banners ──────────────────────────────────────────────── */}
      {(globalError || resetAllError) && (
        <div className="mx-5 mt-3 flex items-center gap-2 p-3 rounded-xl
                        bg-error/10 border border-error/30 text-error text-xs">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{globalError || resetAllError}</span>
        </div>
      )}

      {/* ── Scrollable body ─────────────────────────────────────────────── */}
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-7">

        {/* ── Property groups ───────────────────────────────────────────── */}
        {PROPERTY_GROUPS.map(group => (
          <div key={group.label}>
            <p className="tcp-group-label">
              <span className="mr-1.5 opacity-60">{group.icon}</span>
              {group.label}
            </p>
            <div className="space-y-2">
              {group.properties.map(prop => {
                const isThis       = editing === prop;
                const currentColor = palette?.[prop] ?? '#888888';
                return (
                  <div key={prop}>
                    {!isThis ? (
                      /* ── Collapsed token row ── */
                      <div className="flex items-center gap-2">
                        <div className="flex-1 min-w-0">
                          <PropertyRow
                            property={prop}
                            currentColor={currentColor}
                            isCustomized={customizedProps.has(prop)}
                            contrastInfo={rowContrastMap[prop] ?? null}
                            onEdit={openEditor}
                          />
                        </div>
                        {customizedProps.has(prop) && (
                          <button
                            onClick={() => handleReset(prop)}
                            disabled={resetting === prop}
                            title="Restablecer al valor base"
                            className="w-8 h-8 rounded-lg flex items-center justify-center
                                       bg-surfaceAlt hover:bg-error/10 text-textMuted
                                       hover:text-error border border-surfaceAlt
                                       hover:border-error/30 transition-all flex-shrink-0
                                       disabled:opacity-50"
                          >
                            {resetting === prop
                              ? <Loader2 className="w-3.5 h-3.5 animate-spin" />
                              : <RotateCcw className="w-3.5 h-3.5" />}
                          </button>
                        )}
                      </div>
                    ) : (
                      /* ── Expanded colour editor ── */
                      <div className="tcp-editor space-y-3">

                        {/* Editor header with live contrast badge */}
                        <div className="flex items-center justify-between gap-2">
                          <div className="flex items-center gap-2 min-w-0">
                            <span className="text-sm font-semibold text-textPrimary truncate">
                              {PROPERTY_LABELS[prop]}
                            </span>
                            <ContrastBadge ratio={editorContrastInfo.ratio} />
                          </div>
                          <button onClick={closeEditor} aria-label="Cerrar editor"
                            className="w-7 h-7 rounded-lg flex items-center justify-center
                                       text-textMuted hover:text-textPrimary
                                       hover:bg-surfaceAlt transition-all flex-shrink-0">
                            <X className="w-4 h-4" />
                          </button>
                        </div>

                        {/* Context preview + colour picker + hex input */}
                        <div className="flex items-center gap-3">
                          <ContextPreview property={prop} color={previewColor} />
                          <div
                            className="tcp-color-swatch-btn"
                            style={{ background: toInputHex(previewColor) }}
                            title="Abrir selector de color"
                          >
                            <input
                              type="color"
                              value={toInputHex(previewColor)}
                              onChange={e => handlePickerChange(e.target.value)}
                              data-no-theme-transition
                              aria-label={`Selector de color para ${PROPERTY_LABELS[prop]}`}
                            />
                          </div>
                          <input
                            type="text"
                            value={hexInput}
                            onChange={e => handleHexChange(e.target.value)}
                            placeholder="#RRGGBB"
                            spellCheck={false}
                            data-no-theme-transition
                            aria-label="Valor hexadecimal del color"
                            className={`flex-1 min-w-0 font-mono text-sm px-3 py-2.5 rounded-xl
                                        bg-background border text-textPrimary
                                        placeholder-textMuted focus:outline-none focus:ring-2
                                        transition-all
                                        ${hexError
                                          ? 'border-error focus:ring-error/30'
                                          : 'border-surfaceAlt focus:ring-primary/30 focus:border-primary'}`}
                          />
                        </div>

                        {hexError && (
                          <p className="text-[11px] text-error flex items-center gap-1.5">
                            <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
                            Formato inválido — usa #RGB, #RRGGBB o rgba(…)
                          </p>
                        )}

                        {/* ── Contrast warning (non-blocking) ─────────── */}
                        <ContrastWarning
                          property={prop}
                          ratio={editorContrastInfo.ratio}
                          suggestedColor={editorContrastInfo.suggested}
                          onApplySuggestion={handleApplySuggestion}
                        />

                        {/* Swatch grid */}
                        {swatches.length > 0 && (
                          <div className="grid grid-cols-[repeat(auto-fill,minmax(2rem,1fr))] gap-1.5">
                            {swatches.map(hex => (
                              <button
                                key={hex}
                                onClick={() => handleSwatchClick(hex)}
                                title={hex}
                                aria-label={`Color ${hex}`}
                                className={`w-8 h-8 rounded-lg border-2 transition-all duration-150
                                            hover:scale-110 hover:shadow-lg active:scale-95
                                            focus:outline-none focus:ring-2 focus:ring-primary/50
                                            ${previewColor.toLowerCase() === hex.toLowerCase()
                                              ? 'border-primary ring-2 ring-primary/40 scale-110 shadow-md'
                                              : 'border-white/10 hover:border-white/30'}`}
                                style={{ background: hex }}
                              />
                            ))}
                          </div>
                        )}

                        {saveError && (
                          <p className="text-[11px] text-error flex items-center gap-1.5">
                            <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
                            {saveError}
                          </p>
                        )}

                        {/* Action buttons */}
                        <div className="flex gap-2 pt-1">
                          <button onClick={closeEditor}
                            className="flex-1 py-2.5 rounded-xl bg-background text-textSecondary
                                       text-sm font-medium border border-surfaceAlt
                                       hover:border-primary/30 hover:text-textPrimary transition-all">
                            Cancelar
                          </button>
                          <button
                            onClick={handleSave}
                            disabled={hexError || saving}
                            className="flex-1 py-2.5 rounded-xl bg-primary hover:bg-primaryDark
                                       text-textPrimary text-sm font-semibold transition-all
                                       disabled:opacity-50 flex items-center justify-center gap-1.5
                                       shadow-md hover:shadow-lg"
                          >
                            {saving
                              ? <><Loader2 className="w-3.5 h-3.5 animate-spin" />Guardando…</>
                              : <><Check className="w-3.5 h-3.5" />Aplicar</>}
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        ))}

        {/* ── Customisation log ─────────────────────────────────────────── */}
        <div className="rounded-xl border border-surfaceAlt overflow-hidden">
          <button
            onClick={() => setLogOpen(o => !o)}
            className="w-full flex items-center justify-between px-4 py-3
                       bg-surface hover:bg-surfaceAlt transition-colors text-left"
          >
            <span className="text-sm font-semibold text-textPrimary flex items-center gap-2">
              Historial de cambios
              {log.length > 0 && (
                <span className="text-[11px] bg-primary/20 text-primary
                                 border border-primary/30 px-1.5 py-0.5 rounded-full font-bold">
                  {log.length}
                </span>
              )}
            </span>
            <ChevronDown className={`w-4 h-4 text-textMuted transition-transform duration-200
                                      ${logOpen ? 'rotate-180' : ''}`} />
          </button>
          <div className={`bg-surface overflow-hidden transition-all duration-300 ease-out
                           ${logOpen ? 'max-h-72 opacity-100' : 'max-h-0 opacity-0'}`}>
            <div className="px-4 pb-3 max-h-72 overflow-y-auto pt-1">
              {logLoading ? (
                <div className="flex items-center justify-center py-6">
                  <Loader2 className="w-5 h-5 animate-spin text-primary" />
                </div>
              ) : log.length === 0 ? (
                <p className="text-xs text-textMuted text-center py-4">Sin cambios registrados</p>
              ) : (
                log.map((entry, i) => <LogEntry key={entry.logId ?? i} entry={entry} />)
              )}
            </div>
          </div>
        </div>

        {/* ── Contrast info footnote ────────────────────────────────────── */}
        <div className="flex items-start gap-2 px-1 pb-2">
          <Info className="w-3 h-3 text-textMuted flex-shrink-0 mt-0.5" />
          <p className="text-[10px] text-textMuted leading-relaxed">
            Los valores de contraste siguen el estándar WCAG 2.1 AA (mínimo 4.5:1 para texto normal).
            Las advertencias son indicativas — puedes guardar cualquier color.
          </p>
        </div>

      </div>{/* end scrollable body */}
    </PanelShell>
  );
}

// ── PanelShell ────────────────────────────────────────────────────────────────
function PanelShell({ onClose, children }) {
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-50 flex" role="dialog" aria-modal="true">
      <div className="absolute inset-0 theme-overlay"
        style={{ animation: 'tcpFadeIn 200ms ease-out' }}
        onClick={onClose}
      />
      <aside
        className="tcp-panel relative ml-auto w-full max-w-md h-full flex flex-col overflow-hidden"
        style={{ animation: 'tcpSlideIn 250ms cubic-bezier(0.32, 0.72, 0, 1)' }}
      >
        {children}
      </aside>
      <style>{`
        @keyframes tcpSlideIn { from { transform: translateX(100%); } to { transform: translateX(0); } }
        @keyframes tcpFadeIn  { from { opacity: 0; } to { opacity: 1; } }
      `}</style>
    </div>
  );
}
