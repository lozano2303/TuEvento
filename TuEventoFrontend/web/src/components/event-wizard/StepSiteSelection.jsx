import { useState, useEffect } from 'react';
import { getSitesByCity, createSite } from '../../services/GeolocationService';
import { geocodeCity, reverseGeocode } from '../../services/GeocodingService';
import SiteMapPicker from './SiteMapPicker';

export default function StepSiteSelection({
  formData,
  onChange,
  selectedSite,
  onSiteSelect,
  selectedCity,
  selectedDepartment,
  onBack,
  onSubmit,
  isSubmitting,
  submitError,
}) {
  const [sites,           setSites]           = useState([]);
  const [loading,         setLoading]         = useState(false);
  const [loadError,       setLoadError]        = useState(null);
  const [errors,          setErrors]           = useState({});

  // ── Estado del formulario "crear sede" ───────────────────────────────────
  const [isCreatingSite,  setIsCreatingSite]  = useState(false);
  const [newSite,         setNewSite]         = useState({ name: '', address: '', capacity: '', location: null });
  const [siteFormErrors,  setSiteFormErrors]  = useState({});
  const [siteError,       setSiteError]       = useState(null);
  const [creatingLoading, setCreatingLoading] = useState(false);

  // ── Geocodificación de la ciudad para centrar el mapa ─────────────────────
  const [cityCenter, setCityCenter] = useState(null);
  const [cityBbox,   setCityBbox]   = useState(null);

  useEffect(() => {
    if (!selectedCity) return;
    geocodeCity(selectedCity.name, selectedDepartment?.name)
      .then(result => {
        if (result) {
          setCityCenter({ lat: result.lat, lng: result.lng });
          setCityBbox(result.boundingbox);
        }
      })
      .catch(() => {}); // fallback silencioso
  }, [selectedCity, selectedDepartment]);

  // ── Cargar sedes de la ciudad elegida ────────────────────────────────────
  useEffect(() => {
    if (!formData.cityId) return;
    setLoading(true);
    setLoadError(null);
    setIsCreatingSite(false);
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

  // ── Cambio de ubicación en el mapa (con reverse geocoding) ───────────────
  const handleLocationChange = async (pos, opts = {}) => {
    setNewSite(s => ({ ...s, location: pos }));
    if (opts.skipReverseGeocode) return;
    try {
      const addr = await reverseGeocode(pos.lat, pos.lng);
      if (addr) setNewSite(s => ({ ...s, address: addr }));
    } catch { /* fallo silencioso — el usuario puede escribir la dirección a mano */ }
  };

  // ── Validar capacidad en tiempo real ─────────────────────────────────────
  const seatsNum        = Number(formData.availableSeats);
  const exceedsCapacity = selectedSite && formData.availableSeats !== '' &&
                          !isNaN(seatsNum) && seatsNum > selectedSite.capacity;

  // ── Validación antes de enviar evento ────────────────────────────────────
  const validateEvent = () => {
    const e = {};
    if (!formData.siteId) e.siteId = 'Selecciona una sede';
    if (!formData.availableSeats) e.availableSeats = 'Ingresa la capacidad disponible';
    else if (isNaN(seatsNum) || seatsNum <= 0) e.availableSeats = 'Debe ser un número positivo';
    else if (selectedSite && seatsNum > selectedSite.capacity)
      e.availableSeats = `No puede superar la capacidad máxima (${selectedSite.capacity})`;
    return e;
  };

  const handleSubmit = () => {
    const e = validateEvent();
    setErrors(e);
    if (Object.keys(e).length === 0) onSubmit();
  };

  // ── Crear sede ────────────────────────────────────────────────────────────
  const validateNewSite = () => {
    const e = {};
    if (!newSite.name.trim())    e.name     = 'El nombre es requerido';
    if (!newSite.address.trim()) e.address  = 'La dirección es requerida';
    if (!newSite.capacity || Number(newSite.capacity) <= 0)
      e.capacity = 'La capacidad debe ser un número positivo';
    if (!newSite.location) e.location = 'Selecciona una ubicación en el mapa';
    return e;
  };

  const handleCreateSite = async () => {
    const e = validateNewSite();
    setSiteFormErrors(e);
    if (Object.keys(e).length > 0) return;

    setCreatingLoading(true);
    setSiteError(null);
    try {
      const response    = await createSite({
        cityId:    formData.cityId,
        name:      newSite.name,
        address:   newSite.address,
        capacity:  Number(newSite.capacity),
        latitude:  newSite.location.lat,
        longitude: newSite.location.lng,
      });
      const createdSite = response.data;
      setSites(prev => [...prev, createdSite]);
      onSiteSelect(createdSite);
      onChange({ siteId: createdSite.siteId, availableSeats: '' });
      setIsCreatingSite(false);
      setNewSite({ name: '', address: '', capacity: '', location: null });
    } catch (err) {
      if (err.message?.includes('already exists near')) {
        setSiteError('Ya existe una sede registrada muy cerca de esta ubicación. Intenta marcar un punto distinto o revisa si la sede ya existe en la lista.');
      } else {
        setSiteError(err.message || 'Error al crear la sede');
      }
    } finally {
      setCreatingLoading(false);
    }
  };

  // ── Estilos ───────────────────────────────────────────────────────────────
  const inputClass =
    'w-full bg-background border border-surfaceAlt rounded-lg px-3 py-2 text-sm text-textPrimary ' +
    'focus:outline-none focus:border-accent transition-colors placeholder:text-textMuted';
  const labelClass  = 'block text-xs font-semibold text-textMuted uppercase tracking-wider mb-1';
  const errorClass  = 'text-xs text-red-400 mt-1';

  return (
    <div className="space-y-5">

      {loading  && <p className="text-sm text-textMuted text-center py-4">Cargando sedes...</p>}
      {loadError && <p className="text-sm text-red-400 text-center">{loadError}</p>}

      {/* ── Vista normal: seleccionar sede ─────────────────────────────── */}
      {!loading && !loadError && !isCreatingSite && (
        <>
          {sites.length === 0 ? (
            <div className="rounded-xl border border-surfaceAlt bg-surfaceAlt/40 p-6 text-center space-y-3">
              <p className="text-sm text-textSecondary">Esta ciudad aún no tiene sedes registradas.</p>
              <button
                type="button"
                onClick={() => setIsCreatingSite(true)}
                className="px-4 py-2 rounded-lg text-xs font-semibold border border-accent/40
                           text-accent hover:bg-accent/10 transition-colors"
              >
                + Crear nueva sede
              </button>
            </div>
          ) : (
            <>
              <div>
                <label className={labelClass}>Sede *</label>
                <select className={inputClass} value={formData.siteId ?? ''} onChange={handleSiteChange}>
                  <option value="">Selecciona una sede</option>
                  {sites.map(s => (
                    <option key={s.siteId} value={s.siteId}>{s.name} — {s.address}</option>
                  ))}
                </select>
                {errors.siteId && <p className={errorClass}>{errors.siteId}</p>}
              </div>

              <button
                type="button"
                onClick={() => setIsCreatingSite(true)}
                className="text-xs text-accent hover:underline bg-transparent border-none cursor-pointer"
              >
                + Registrar nueva sede en esta ciudad
              </button>

              {selectedSite && (
                <div className="rounded-lg border border-accent/30 bg-accent/10 px-4 py-3 space-y-1">
                  <p className="text-xs font-semibold text-accent uppercase tracking-wide">Sede seleccionada</p>
                  <p className="text-sm text-textPrimary font-medium">{selectedSite.name}</p>
                  <p className="text-xs text-textMuted">{selectedSite.address}</p>
                  <p className="text-xs text-textSecondary">
                    Capacidad máxima: <span className="font-bold text-textPrimary">{selectedSite.capacity}</span> personas
                  </p>
                </div>
              )}

              <div>
                <label className={labelClass}>Aforo disponible *</label>
                <input
                  type="number" min="1"
                  max={selectedSite?.capacity ?? undefined}
                  className={[inputClass, exceedsCapacity ? 'border-red-400' : ''].join(' ')}
                  placeholder={selectedSite ? `Máximo ${selectedSite.capacity}` : 'Ingresa la cantidad'}
                  value={formData.availableSeats}
                  onChange={e => onChange({ availableSeats: e.target.value })}
                />
                {exceedsCapacity && <p className={errorClass}>⚠ Supera la capacidad máxima ({selectedSite.capacity})</p>}
                {errors.availableSeats && !exceedsCapacity && <p className={errorClass}>{errors.availableSeats}</p>}
              </div>
            </>
          )}
        </>
      )}

      {/* ── Formulario: crear nueva sede ───────────────────────────────── */}
      {!loading && isCreatingSite && (
        <div className="space-y-4 rounded-xl border border-accent/20 bg-accent/5 p-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-textPrimary">Registrar nueva sede</h3>
            <button
              type="button"
              onClick={() => { setIsCreatingSite(false); setSiteError(null); setSiteFormErrors({}); }}
              className="text-textMuted hover:text-textPrimary text-xs"
            >✕ Cancelar</button>
          </div>

          <div>
            <label className={labelClass}>Nombre de la sede *</label>
            <input type="text" maxLength={150} className={inputClass}
              placeholder="Ej. Centro de Convenciones Ágora"
              value={newSite.name}
              onChange={e => setNewSite(s => ({ ...s, name: e.target.value }))} />
            {siteFormErrors.name && <p className={errorClass}>{siteFormErrors.name}</p>}
          </div>

          <div>
            <label className={labelClass}>Dirección *</label>
            <input type="text" maxLength={200} className={inputClass}
              placeholder="Se completa automáticamente al hacer clic en el mapa, o escríbela"
              value={newSite.address}
              onChange={e => setNewSite(s => ({ ...s, address: e.target.value }))} />
            {siteFormErrors.address && <p className={errorClass}>{siteFormErrors.address}</p>}
          </div>

          <div>
            <label className={labelClass}>Capacidad máxima *</label>
            <input type="number" min="1" className={inputClass} placeholder="Ej. 500"
              value={newSite.capacity}
              onChange={e => setNewSite(s => ({ ...s, capacity: e.target.value }))} />
            {siteFormErrors.capacity && <p className={errorClass}>{siteFormErrors.capacity}</p>}
          </div>

          <div>
            <label className={labelClass}>Ubicación en el mapa *</label>
            <p className="text-[11px] text-textMuted mb-2">
              Haz clic en el mapa para marcar la ubicación. La dirección se completará automáticamente.
            </p>
            <SiteMapPicker
              value={newSite.location}
              onChange={handleLocationChange}
              defaultCenter={cityCenter ? [cityCenter.lat, cityCenter.lng] : undefined}
              cityBbox={cityBbox}
            />
            {newSite.location && (
              <p className="text-[11px] text-textMuted mt-1.5 font-mono">
                Lat: {newSite.location.lat.toFixed(6)}, Lng: {newSite.location.lng.toFixed(6)}
              </p>
            )}
            {siteFormErrors.location && <p className={errorClass}>{siteFormErrors.location}</p>}
          </div>

          {siteError && (
            <div className="rounded-lg border border-red-400/30 bg-red-400/10 px-3 py-2.5">
              <p className="text-xs text-red-400">{siteError}</p>
            </div>
          )}

          <button
            type="button"
            onClick={handleCreateSite}
            disabled={creatingLoading}
            className="w-full py-2.5 rounded-lg text-sm font-semibold bg-accent text-white
                       hover:bg-accent/90 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {creatingLoading ? 'Registrando sede...' : 'Registrar sede'}
          </button>
        </div>
      )}

      {submitError && !isCreatingSite && (
        <div className="rounded-lg border border-red-400/30 bg-red-400/10 px-4 py-3">
          <p className="text-sm text-red-400">{submitError}</p>
        </div>
      )}

      {!isCreatingSite && (
        <div className="flex gap-3 pt-2">
          <button type="button" onClick={onBack} disabled={isSubmitting}
            className="flex-1 py-2.5 rounded-lg text-sm font-semibold border border-surfaceAlt
                       text-textSecondary hover:bg-surfaceAlt transition-colors
                       disabled:opacity-40 disabled:cursor-not-allowed">
            ← Atrás
          </button>
          <button type="button" onClick={handleSubmit}
            disabled={isSubmitting || (sites.length === 0 && !formData.siteId)}
            className="flex-1 py-2.5 rounded-lg text-sm font-semibold bg-accent text-white
                       hover:bg-accent/90 transition-colors
                       disabled:opacity-40 disabled:cursor-not-allowed">
            {isSubmitting ? 'Creando evento...' : 'Crear evento'}
          </button>
        </div>
      )}
    </div>
  );
}
