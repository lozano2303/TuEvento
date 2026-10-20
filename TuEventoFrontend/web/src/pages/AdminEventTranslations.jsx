import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Languages, Save, X, Edit, Check, AlertTriangle, Loader2, Globe, FileText, Flag
} from 'lucide-react';
import AdminSidebar from '../components/admin/AdminSidebar';
import { getAdminEvents } from '../services/EventService';
import { 
  getEventTranslations, 
  updateEventTranslation, 
  createEventTranslation,
  getLanguageId 
} from '../services/TranslationService';

const LANGUAGES = [
  { 
    code: 'en', 
    name: 'English',
    flag: (
      <svg className="w-6 h-4 rounded-sm" viewBox="0 0 60 30">
        <clipPath id="s"><path d="M0,0 v30 h60 v-30 z"/></clipPath>
        <clipPath id="t"><path d="M30,15 h30 v15 z v-30 h-30 z h-30 v15 z v-30 h30 z"/></clipPath>
        <g clipPath="url(#s)"><path d="M0,0 v30 h60 v-30 z" fill="#012169"/><path d="M0,0 L60,30 M60,0 L0,30" stroke="#fff" strokeWidth="6"/><path d="M0,0 L60,30 M60,0 L0,30" clipPath="url(#t)" stroke="#C8102E" strokeWidth="4"/><path d="M30,0 v30 M0,15 h60" stroke="#fff" strokeWidth="10"/><path d="M30,0 v30 M0,15 h60" stroke="#C8102E" strokeWidth="6"/></g>
      </svg>
    )
  },
  { 
    code: 'fr', 
    name: 'Français',
    flag: (
      <svg className="w-6 h-4 rounded-sm" viewBox="0 0 900 600">
        <rect width="900" height="600" fill="#ED2939"/>
        <rect width="600" height="600" fill="#fff"/>
        <rect width="300" height="600" fill="#002395"/>
      </svg>
    )
  },
  { 
    code: 'pt', 
    name: 'Português',
    flag: (
      <svg className="w-6 h-4 rounded-sm" viewBox="0 0 600 400">
        <rect width="600" height="400" fill="#FF0000"/>
        <rect width="240" height="400" fill="#006600"/>
        <circle cx="240" cy="200" r="80" fill="#FFFF00" stroke="#0000FF" strokeWidth="8"/>
      </svg>
    )
  },
];

export default function AdminEventTranslations() {
  const { lang } = useParams();
  const navigate = useNavigate();
  
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [editForm, setEditForm] = useState({ translatedName: '', translatedDescription: '' });
  const [filter, setFilter] = useState('needs-review'); // Filtro por defecto: necesitan revisión

  const currentLanguage = LANGUAGES.find(l => l.code === lang) || LANGUAGES[0];
  const languageId = getLanguageId(lang);

  // Validar que no sea español (redirigir a inglés por defecto)
  useEffect(() => {
    if (lang === 'es') {
      navigate('/admin-translations/en/events', { replace: true });
    }
  }, [lang, navigate]);

  // Cargar eventos con traducciones
  useEffect(() => {
    loadEvents();
  }, [lang, filter]); // Recargar cuando cambie idioma o filtro

  const loadEvents = async () => {
    setLoading(true);
    setError(null);
    try {
      // Usar el endpoint específico de traducciones con filtro
      const response = await getEventTranslations(lang, filter);
      setEvents(response.data || []);
    } catch (err) {
      setError('Error al cargar eventos: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleLanguageChange = (newLang) => {
    navigate(`/admin-translations/${newLang}/events`);
  };

  const startEdit = (event) => {
    setEditingId(event.eventId);
    setEditForm({
      translatedName: event.translatedName || event.eventName,
      translatedDescription: event.translatedDescription || event.description || '',
    });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditForm({ translatedName: '', translatedDescription: '' });
  };

  const saveTranslation = async (eventId) => {
    setSaving(true);
    setError(null);
    try {
      const data = {
        translatedName: editForm.translatedName.trim(),
        translatedDescription: editForm.translatedDescription.trim(),
        source: 'MANUAL',
        status: 'PUBLISHED',
      };

      // PUT crea o actualiza automáticamente
      await updateEventTranslation(eventId, languageId, data);

      setEditingId(null);
      await loadEvents();
    } catch (err) {
      setError('Error al guardar traducción: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">
      <AdminSidebar activePage="translations" />

      {/* Main content */}
      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-7xl mx-auto w-full">

          {/* Header */}
          <div className="mb-6 flex items-start justify-between">
            <div>
              <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1 flex items-center gap-3">
                <Languages className="w-6 h-6" />
                Traducciones de Eventos
              </h1>
              <p className="text-textSecondary text-sm leading-relaxed">
                Gestiona las traducciones de los eventos en diferentes idiomas
              </p>
            </div>

            {/* Selector de idioma */}
            <div className="flex items-center gap-2 bg-surfaceAlt rounded-xl p-1">
              {LANGUAGES.map((language) => (
                <button
                  key={language.code}
                  onClick={() => handleLanguageChange(language.code)}
                  className={`px-4 py-2 rounded-lg text-sm font-semibold transition-all flex items-center gap-2.5
                    ${lang === language.code
                      ? 'bg-primary text-textPrimary shadow-md'
                      : 'text-textMuted hover:text-textPrimary hover:bg-surface'
                    }`}
                >
                  {language.flag}
                  {language.name}
                </button>
              ))}
            </div>
          </div>

          {/* Banner de idioma actual - simplificado */}
          <div className="mb-6 px-5 py-3 rounded-xl bg-primary/10 border border-primary/20 flex items-center gap-3">
            <div className="flex-shrink-0">
              {currentLanguage.flag}
            </div>
            <div className="flex-1">
              <p className="text-sm font-semibold text-textPrimary">
                Editando traducciones en <span className="text-primary">{currentLanguage.name}</span>
              </p>
            </div>
          </div>

          {/* Filtros de visualización */}
          <div className="mb-6 flex items-center gap-2">
            <span className="text-xs font-bold uppercase tracking-wider text-textMuted">Mostrar:</span>
            <button
              onClick={() => setFilter('needs-review')}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all
                ${filter === 'needs-review'
                  ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                  : 'bg-surfaceAlt/60 text-textMuted hover:bg-surfaceAlt'
                }`}
            >
              Necesitan revisión
            </button>
            <button
              onClick={() => setFilter('not-translated')}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all
                ${filter === 'not-translated'
                  ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40'
                  : 'bg-surfaceAlt/60 text-textMuted hover:bg-surfaceAlt'
                }`}
            >
              Sin traducir
            </button>
            <button
              onClick={() => setFilter('translated')}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all
                ${filter === 'translated'
                  ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                  : 'bg-surfaceAlt/60 text-textMuted hover:bg-surfaceAlt'
                }`}
            >
              Traducidos
            </button>
            <button
              onClick={() => setFilter('all')}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-all
                ${filter === 'all'
                  ? 'bg-primary text-textPrimary'
                  : 'bg-surfaceAlt/60 text-textMuted hover:bg-surfaceAlt'
                }`}
            >
              Todos
            </button>
          </div>

          {/* Error banner */}
          {error && (
            <div className="mb-4 px-4 py-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-start gap-2">
              <AlertTriangle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
              <button onClick={() => setError(null)} className="ml-auto text-rose-400/60 hover:text-rose-400">
                <X className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* Table */}
          <div className="rounded-xl overflow-hidden border border-surfaceAlt bg-surface">
            {loading ? (
              <div className="p-10 text-center text-textMuted text-sm flex items-center justify-center gap-2">
                <Loader2 className="w-4 h-4 animate-spin" /> Cargando eventos…
              </div>
            ) : events.length === 0 ? (
              <div className="p-10 text-center text-textMuted text-sm">
                No hay eventos disponibles
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="border-b border-white/5">
                      <th className="px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-primary w-16">
                        ID
                      </th>
                      <th className="px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-primary">
                        Nombre Original (ES)
                      </th>
                      <th className="px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-primary">
                        Nombre Traducido ({currentLanguage.code.toUpperCase()})
                      </th>
                      <th className="px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-primary">
                        Descripción Traducida
                      </th>
                      <th className="px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-primary text-right">
                        Acciones
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {events.map((event, idx) => {
                      const isEditing = editingId === event.eventId;
                      const hasTranslation = event.translatedName || event.translatedDescription;

                      return (
                        <tr
                          key={event.eventId}
                          className={`border-b border-white/5 hover:bg-white/[0.03] transition-colors
                            ${idx % 2 !== 0 ? 'bg-white/[0.02]' : ''}`}
                        >
                          {/* ID */}
                          <td className="px-5 py-3.5 text-sm text-textMuted font-mono">
                            #{event.eventId}
                          </td>

                          {/* Nombre Original */}
                          <td className="px-5 py-3.5">
                            <div className="max-w-xs">
                              <p className="text-sm font-semibold text-textPrimary truncate">
                                {event.eventName}
                              </p>
                              <p className="text-xs text-textMuted truncate">
                                {event.categoryName || 'Sin categoría'}
                              </p>
                            </div>
                          </td>

                          {/* Nombre Traducido */}
                          <td className="px-5 py-3.5">
                            {isEditing ? (
                              <input
                                type="text"
                                value={editForm.translatedName}
                                onChange={(e) => setEditForm({ ...editForm, translatedName: e.target.value })}
                                className="w-full px-3 py-2 rounded-lg bg-surfaceAlt border border-surfaceAlt text-sm text-textPrimary focus:outline-none focus:border-primary transition-colors"
                                placeholder="Nombre traducido..."
                              />
                            ) : (
                              <div className="max-w-xs">
                                {hasTranslation ? (
                                  <p className="text-sm text-textSecondary truncate">
                                    {event.translatedName || event.eventName}
                                  </p>
                                ) : (
                                  <span className="text-xs text-textMuted italic">Sin traducir</span>
                                )}
                              </div>
                            )}
                          </td>

                          {/* Descripción Traducida */}
                          <td className="px-5 py-3.5">
                            {isEditing ? (
                              <textarea
                                value={editForm.translatedDescription}
                                onChange={(e) => setEditForm({ ...editForm, translatedDescription: e.target.value })}
                                rows={2}
                                className="w-full px-3 py-2 rounded-lg bg-surfaceAlt border border-surfaceAlt text-sm text-textPrimary focus:outline-none focus:border-primary transition-colors resize-none"
                                placeholder="Descripción traducida..."
                              />
                            ) : (
                              <div className="max-w-md">
                                {event.translatedDescription ? (
                                  <p className="text-xs text-textMuted line-clamp-2">
                                    {event.translatedDescription}
                                  </p>
                                ) : (
                                  <span className="text-xs text-textMuted italic">Sin traducir</span>
                                )}
                              </div>
                            )}
                          </td>

                          {/* Acciones */}
                          <td className="px-5 py-3.5 text-right">
                            {isEditing ? (
                              <div className="flex items-center justify-end gap-1">
                                <button
                                  onClick={() => saveTranslation(event.eventId)}
                                  disabled={saving}
                                  className="p-2 hover:bg-emerald-500/20 rounded-full transition-colors text-emerald-400 disabled:opacity-50"
                                  title="Guardar"
                                >
                                  {saving ? (
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                  ) : (
                                    <Check className="w-4 h-4" />
                                  )}
                                </button>
                                <button
                                  onClick={cancelEdit}
                                  disabled={saving}
                                  className="p-2 hover:bg-rose-500/20 rounded-full transition-colors text-rose-400 disabled:opacity-50"
                                  title="Cancelar"
                                >
                                  <X className="w-4 h-4" />
                                </button>
                              </div>
                            ) : (
                              <button
                                onClick={() => startEdit(event)}
                                className="p-2 hover:bg-primary/20 rounded-full transition-colors text-primary"
                                title="Editar traducción"
                              >
                                <Edit className="w-4 h-4" />
                              </button>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Info footer - más conciso */}
          <div className="mt-4 px-4 py-3 rounded-xl bg-blue-500/5 border border-blue-500/20">
            <p className="text-xs text-blue-300/80">
              <strong>ℹ️ Info:</strong> Los eventos sin traducción mostrarán su contenido original en español a los usuarios.
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
