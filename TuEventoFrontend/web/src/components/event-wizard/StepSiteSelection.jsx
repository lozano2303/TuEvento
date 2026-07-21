import { useState, useEffect } from 'react';
import { getSitesByCity } from '../../services/GeolocationService';

export default function StepSiteSelection({
  formData,
  onChange,
  selectedSite,
  onSiteSelect,
  onBack,
  onSubmit,
  isSubmitting,
  submitError,
}) {
  const [sites,        setSites]        = useState([]);
  const [loading,      setLoading]      = useState(false);
  const [loadError,    setLoadError]    = useState(null);
  const [errors,       setErrors]       = useState({});

  // ── Cargar sedes de la ciudad elegida ────────────────────────────────────
  useEffect(() => {
    if (!formData.cityId) return;
    setLoading(true);
    setLoadError(null);
    getSitesByCity(formData.cityId)
      .then(r => setSites(r.data ?? []))
      .catch(() => { setSites([]); setLoadError('Error al cargar sedes'); })
      .finally(() => setLoading(false));
  }, [formData.cityId]);

  // ── Seleccionar sitio ─────────────────────────────────────────────────────
  const handleSiteChange = (e) => {
    const siteId = e.target.value ? Number(e.target.value) : null;
    const site   = sites.find(s => s.siteId === siteId) ?? null;
    onChange({ siteId, availableSeats: '' });
    onSiteSelect(site);
  };

  // ── Validar capacidad en tiempo real ─────────────────────────────────────
  const seatsNum       = Number(formData.availableSeats);
  const exceedsCapacity = selectedSite && formData.availableSeats !== '' &&
                          !isNaN(seatsNum) && seatsNum > selectedSite.capacity;

  // ── Validación antes de enviar ────────────────────────────────────────────
  const validate = () => {
    const e = {};
    if (!formData.siteId) e.siteId = 'Selecciona una sede';
    if (!formData.availableSeats) e.availableSeats = 'Ingresa la capacidad disponible';
    else if (isNaN(seatsNum) || seatsNum <= 0) e.availableSeats = 'Debe ser un número positivo';
    else if (selectedSite && seatsNum > selectedSite.capacity)
      e.availableSeats = `No puede superar la capacidad máxima (${selectedSite.capacity})`;
    return e;
  };

  const handleSubmit = () => {
    const e = validate();
    setErrors(e);
    if (Object.keys(e).length === 0) onSubmit();
  };

  const inputClass =
    'w-full bg-background border border-surfaceAlt rounded-lg px-3 py-2 text-sm text-textPrimary ' +
    'focus:outline-none focus:border-accent transition-colors placeholder:text-textMuted';
  const labelClass = 'block text-xs font-semibold text-textMuted uppercase tracking-wider mb-1';
  const errorClass = 'text-xs text-red-400 mt-1';

  return (
    <div className="space-y-5">

      {/* Estado de carga / error de carga */}
      {loading && (
        <p className="text-sm text-textMuted text-center py-4">Cargando sedes...</p>
      )}
      {loadError && (
        <p className="text-sm text-red-400 text-center">{loadError}</p>
      )}

      {/* Ciudad sin sedes */}
      {!loading && !loadError && sites.length === 0 && (
        <div className="rounded-xl border border-surfaceAlt bg-surfaceAlt/40 p-6 text-center space-y-3">
          <p className="text-sm text-textSecondary">
            Esta ciudad aún no tiene sedes registradas.
          </p>
          <button
            disabled
            className="px-4 py-2 rounded-lg text-xs font-semibold border border-surfaceAlt
                       text-textMuted opacity-40 cursor-not-allowed"
          >
            Crear nueva sede (próximamente)
          </button>
        </div>
      )}

      {/* Selector de sede */}
      {!loading && sites.length > 0 && (
        <>
          <div>
            <label className={labelClass}>Sede *</label>
            <select
              className={inputClass}
              value={formData.siteId ?? ''}
              onChange={handleSiteChange}
            >
              <option value="">Selecciona una sede</option>
              {sites.map(s => (
                <option key={s.siteId} value={s.siteId}>
                  {s.name} — {s.address}
                </option>
              ))}
            </select>
            {errors.siteId && <p className={errorClass}>{errors.siteId}</p>}
          </div>

          {/* Info de la sede seleccionada */}
          {selectedSite && (
            <div className="rounded-lg border border-accent/30 bg-accent/10 px-4 py-3 space-y-1">
              <p className="text-xs font-semibold text-accent uppercase tracking-wide">
                Sede seleccionada
              </p>
              <p className="text-sm text-textPrimary font-medium">{selectedSite.name}</p>
              <p className="text-xs text-textMuted">{selectedSite.address}</p>
              <p className="text-xs text-textSecondary">
                Capacidad máxima: <span className="font-bold text-textPrimary">{selectedSite.capacity}</span> personas
              </p>
            </div>
          )}

          {/* Aforo disponible */}
          <div>
            <label className={labelClass}>Aforo disponible *</label>
            <input
              type="number"
              min="1"
              max={selectedSite?.capacity ?? undefined}
              className={[
                inputClass,
                exceedsCapacity ? 'border-red-400 focus:border-red-400' : '',
              ].join(' ')}
              placeholder={selectedSite ? `Máximo ${selectedSite.capacity}` : 'Ingresa la cantidad'}
              value={formData.availableSeats}
              onChange={e => onChange({ availableSeats: e.target.value })}
            />
            {exceedsCapacity && (
              <p className={errorClass}>
                ⚠ Supera la capacidad máxima de la sede ({selectedSite.capacity})
              </p>
            )}
            {errors.availableSeats && !exceedsCapacity && (
              <p className={errorClass}>{errors.availableSeats}</p>
            )}
          </div>
        </>
      )}

      {/* Error del backend */}
      {submitError && (
        <div className="rounded-lg border border-red-400/30 bg-red-400/10 px-4 py-3">
          <p className="text-sm text-red-400">{submitError}</p>
        </div>
      )}

      {/* Botones */}
      <div className="flex gap-3 pt-2">
        <button
          type="button"
          onClick={onBack}
          disabled={isSubmitting}
          className="flex-1 py-2.5 rounded-lg text-sm font-semibold border border-surfaceAlt
                     text-textSecondary hover:bg-surfaceAlt transition-colors
                     disabled:opacity-40 disabled:cursor-not-allowed"
        >
          ← Atrás
        </button>
        <button
          type="button"
          onClick={handleSubmit}
          disabled={isSubmitting || sites.length === 0}
          className="flex-1 py-2.5 rounded-lg text-sm font-semibold bg-accent text-white
                     hover:bg-accent/90 transition-colors
                     disabled:opacity-40 disabled:cursor-not-allowed"
        >
          {isSubmitting ? 'Creando evento...' : 'Crear evento'}
        </button>
      </div>
    </div>
  );
}
