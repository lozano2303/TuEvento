import { useState, useEffect } from 'react';
import { getActiveCategories } from '../../services/CategoryService';
import { getDepartments, getCitiesByDepartment } from '../../services/GeolocationService';

export default function StepGeneralInfo({ formData, onChange, onNext, onCityChange, onDepartmentChange }) {
  const [categories,   setCategories]   = useState([]);
  const [departments,  setDepartments]  = useState([]);
  const [cities,       setCities]       = useState([]);
  const [loadingCats,  setLoadingCats]  = useState(false);
  const [loadingDepts, setLoadingDepts] = useState(false);
  const [loadingCities,setLoadingCities]= useState(false);
  const [errors,       setErrors]       = useState([]);

  // ── Cargar categorías y departamentos al montar ───────────────────────────
  useEffect(() => {
    setLoadingCats(true);
    getActiveCategories()
      .then(r => setCategories((r.data ?? []).filter(c => c.dadId === null)))
      .catch(() => setCategories([]))
      .finally(() => setLoadingCats(false));

    setLoadingDepts(true);
    getDepartments()
      .then(r => setDepartments(r.data ?? []))
      .catch(() => setDepartments([]))
      .finally(() => setLoadingDepts(false));
  }, []);

  // ── Cargar ciudades cuando cambia el departamento ─────────────────────────
  useEffect(() => {
    if (!formData.departmentId) { setCities([]); return; }
    setLoadingCities(true);
    getCitiesByDepartment(formData.departmentId)
      .then(r => setCities(r.data ?? []))
      .catch(() => setCities([]))
      .finally(() => setLoadingCities(false));
  }, [formData.departmentId]);

  // ── Validación del paso 1 ─────────────────────────────────────────────────
  const validate = () => {
    const e = {};
    if (!formData.eventName.trim())   e.eventName   = 'El nombre es requerido';
    if (!formData.description.trim()) e.description = 'La descripción es requerida';
    if (!formData.startDate)          e.startDate   = 'La fecha de inicio es requerida';
    if (!formData.finishDate)         e.finishDate  = 'La fecha de fin es requerida';
    if (formData.startDate && formData.finishDate && formData.finishDate <= formData.startDate)
      e.finishDate = 'La fecha de fin debe ser posterior a la de inicio';
    if (!formData.categoryId)         e.categoryId  = 'Selecciona una categoría';
    if (!formData.cityId)             e.cityId      = 'Selecciona una ciudad';
    return e;
  };

  const handleNext = () => {
    const e = validate();
    setErrors(e);
    if (Object.keys(e).length === 0) onNext();
  };

  // ── Estilos reutilizables ─────────────────────────────────────────────────
  const inputClass =
    'w-full bg-background border border-surfaceAlt rounded-lg px-3 py-2 text-sm text-textPrimary ' +
    'focus:outline-none focus:border-accent transition-colors placeholder:text-textMuted';
  const labelClass = 'block text-xs font-semibold text-textMuted uppercase tracking-wider mb-1';
  const errorClass = 'text-xs text-red-400 mt-1';

  return (
    <div className="space-y-5">
      {/* Nombre del evento */}
      <div>
        <label className={labelClass}>Nombre del evento *</label>
        <input
          type="text"
          className={inputClass}
          placeholder="Ej. Festival de Música 2025"
          value={formData.eventName}
          onChange={e => onChange({ eventName: e.target.value })}
        />
        {errors.eventName && <p className={errorClass}>{errors.eventName}</p>}
      </div>

      {/* Descripción */}
      <div>
        <label className={labelClass}>Descripción *</label>
        <textarea
          rows={3}
          className={inputClass + ' resize-none'}
          placeholder="Describe el evento..."
          value={formData.description}
          onChange={e => onChange({ description: e.target.value })}
        />
        {errors.description && <p className={errorClass}>{errors.description}</p>}
      </div>

      {/* Fechas */}
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className={labelClass}>Fecha de inicio *</label>
          <input
            type="date"
            className={inputClass}
            value={formData.startDate}
            onChange={e => onChange({ startDate: e.target.value })}
          />
          {errors.startDate && <p className={errorClass}>{errors.startDate}</p>}
        </div>
        <div>
          <label className={labelClass}>Fecha de fin *</label>
          <input
            type="date"
            className={inputClass}
            value={formData.finishDate}
            onChange={e => onChange({ finishDate: e.target.value })}
          />
          {errors.finishDate && <p className={errorClass}>{errors.finishDate}</p>}
        </div>
      </div>

      {/* Público */}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={() => onChange({ isPublic: !formData.isPublic })}
          className={[
            'relative inline-flex h-6 w-11 items-center rounded-full transition-colors',
            formData.isPublic ? 'bg-accent' : 'bg-surfaceAlt',
          ].join(' ')}
          aria-checked={formData.isPublic}
          role="switch"
        >
          <span
            className={[
              'inline-block h-4 w-4 rounded-full bg-white shadow transition-transform',
              formData.isPublic ? 'translate-x-6' : 'translate-x-1',
            ].join(' ')}
          />
        </button>
        <span className="text-sm text-textSecondary">
          Evento {formData.isPublic ? 'público' : 'privado'}
        </span>
      </div>

      {/* Categoría */}
      <div>
        <label className={labelClass}>Categoría *</label>
        <select
          className={inputClass}
          value={formData.categoryId ?? ''}
          onChange={e => onChange({ categoryId: e.target.value ? Number(e.target.value) : null })}
          disabled={loadingCats}
        >
          <option value="">{loadingCats ? 'Cargando...' : 'Selecciona una categoría'}</option>
          {categories.map(c => (
            <option key={c.categoryId} value={c.categoryId}>{c.name}</option>
          ))}
        </select>
        {errors.categoryId && <p className={errorClass}>{errors.categoryId}</p>}
      </div>

      {/* Departamento */}
      <div>
        <label className={labelClass}>Departamento *</label>
        <select
          className={inputClass}
          value={formData.departmentId ?? ''}
          onChange={e => {
            const dept = departments.find(d => d.departmentId === Number(e.target.value)) ?? null;
            onChange({ departmentId: dept ? dept.departmentId : null, cityId: null });
            onDepartmentChange?.(dept);
          }}
          disabled={loadingDepts}
        >
          <option value="">{loadingDepts ? 'Cargando...' : 'Selecciona un departamento'}</option>
          {departments.map(d => (
            <option key={d.departmentId} value={d.departmentId}>{d.name}</option>
          ))}
        </select>
      </div>

      {/* Ciudad */}
      <div>
        <label className={labelClass}>Ciudad *</label>
        <select
          className={inputClass}
          value={formData.cityId ?? ''}
          onChange={e => {
            const city = cities.find(c => c.cityId === Number(e.target.value)) ?? null;
            onChange({ cityId: city ? city.cityId : null });
            onCityChange?.(city);
          }}
          disabled={!formData.departmentId || loadingCities}
        >
          <option value="">
            {!formData.departmentId
              ? 'Primero selecciona un departamento'
              : loadingCities
              ? 'Cargando ciudades...'
              : 'Selecciona una ciudad'}
          </option>
          {cities.map(c => (
            <option key={c.cityId} value={c.cityId}>{c.name}</option>
          ))}
        </select>
        {errors.cityId && <p className={errorClass}>{errors.cityId}</p>}
      </div>

      {/* Siguiente */}
      <div className="pt-2">
        <button
          type="button"
          onClick={handleNext}
          className="w-full py-2.5 rounded-lg text-sm font-semibold bg-accent text-white
                     hover:bg-accent/90 transition-colors"
        >
          Siguiente →
        </button>
      </div>
    </div>
  );
}
