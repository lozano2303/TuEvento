import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Languages, Save, X, Edit, Check, AlertTriangle, Loader2, Flag, Clock
} from 'lucide-react';
import AdminSidebar from '../components/admin/AdminSidebar';
import { 
  getSectionTypeTranslations, 
  updateSectionTypeTranslation,
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

export default function AdminSectionTypeTranslations() {
  const { lang } = useParams();
  const navigate = useNavigate();
  
  const [sectionTypes, setSectionTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [editForm, setEditForm] = useState({ translatedName: '' });
  const [filter, setFilter] = useState('not-translated');

  const currentLanguage = LANGUAGES.find(l => l.code === lang) || LANGUAGES[0];
  const languageId = getLanguageId(lang);

  useEffect(() => {
    if (lang === 'es') {
      navigate('/admin-translations/en/section-types', { replace: true });
    }
  }, [lang, navigate]);

  useEffect(() => {
    loadSectionTypes();
  }, [lang, filter]);

  // Polling para verificar si hay traducciones generándose
  useEffect(() => {
    const hasGeneratingTranslations = sectionTypes.some(st => st.translationGenerating);
    if (!hasGeneratingTranslations) return;

    let pollCount = 0;
    const maxPolls = 40;

    const checkTranslations = async () => {
      pollCount++;
      if (pollCount > maxPolls) return;

      try {
        const response = await getSectionTypeTranslations(lang, filter);
        setSectionTypes(response.data || []);
      } catch (err) {
        console.error('Error checking translation status:', err);
      }
    };

    const pollInterval = setInterval(checkTranslations, 3000);
    return () => clearInterval(pollInterval);
  }, [sectionTypes, lang, filter]);

  const loadSectionTypes = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getSectionTypeTranslations(lang, filter);
      setSectionTypes(response.data || []);
    } catch (err) {
      setError('Error al cargar tipos de sección: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleLanguageChange = (newLang) => {
    navigate(`/admin-translations/${newLang}/section-types`);
  };

  const startEdit = (sectionType) => {
    setEditingId(sectionType.sectionTypeId);
    setEditForm({
      translatedName: sectionType.translatedName || sectionType.sectionTypeName,
    });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditForm({ translatedName: '' });
  };

  const saveTranslation = async (sectionTypeId) => {
    setSaving(true);
    setError(null);
    try {
      const data = {
        translatedName: editForm.translatedName.trim(),
        source: 'MANUAL',
        status: 'PUBLISHED',
      };

      await updateSectionTypeTranslation(sectionTypeId, languageId, data);

      setEditingId(null);
      await loadSectionTypes();
    } catch (err) {
      setError('Error al guardar traducción: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">
      <AdminSidebar activePage="translation-section-types" />

      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-7xl mx-auto w-full">

          <div className="mb-6 flex items-start justify-between">
            <div>
              <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1 flex items-center gap-3">
                <Languages className="w-6 h-6" />
                Traducciones de Tipos de Sección
              </h1>
              <p className="text-textSecondary text-sm leading-relaxed">
                Gestiona las traducciones de tipos de sección en diferentes idiomas
              </p>
            </div>

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

          <div className="mb-6 flex items-center gap-2">
            <span className="text-xs font-bold uppercase tracking-wider text-textMuted">Mostrar:</span>
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

          {error && (
            <div className="mb-4 px-4 py-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-start gap-2">
              <AlertTriangle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
              <button onClick={() => setError(null)} className="ml-auto text-rose-400/60 hover:text-rose-400">
                <X className="w-4 h-4" />
              </button>
            </div>
          )}

          <div className="rounded-xl overflow-hidden border border-surfaceAlt bg-surface">
            {loading ? (
              <div className="p-10 text-center text-textMuted text-sm flex items-center justify-center gap-2">
                <Loader2 className="w-4 h-4 animate-spin" /> Cargando tipos de sección…
              </div>
            ) : sectionTypes.length === 0 ? (
              <div className="p-10 text-center text-textMuted text-sm">
                No hay tipos de sección disponibles
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="border-b border-white/5">
                      <th className="px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-primary w-16">ID</th>
                      <th className="px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-primary">Nombre Original (ES)</th>
                      <th className="px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-primary">Nombre Traducido ({currentLanguage.code.toUpperCase()})</th>
                      <th className="px-5 py-3.5 text-xs font-bold uppercase tracking-widest text-primary text-right">Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sectionTypes.map((sectionType, idx) => {
                      const isEditing = editingId === sectionType.sectionTypeId;
                      const hasTranslation = sectionType.translatedName;

                      return (
                        <tr
                          key={sectionType.sectionTypeId}
                          className={`border-b border-white/5 hover:bg-white/[0.03] transition-colors
                            ${idx % 2 !== 0 ? 'bg-white/[0.02]' : ''}`}
                        >
                          <td className="px-5 py-3.5 text-sm text-textMuted font-mono">#{sectionType.sectionTypeId}</td>

                          <td className="px-5 py-3.5">
                            <p className="text-sm font-semibold text-textPrimary">{sectionType.sectionTypeName}</p>
                          </td>

                          <td className="px-5 py-3.5">
                            {isEditing ? (
                              <input
                                type="text"
                                value={editForm.translatedName}
                                onChange={(e) => setEditForm({ translatedName: e.target.value })}
                                className="w-full px-3 py-2 rounded-lg bg-surfaceAlt border border-surfaceAlt text-sm text-textPrimary focus:outline-none focus:border-primary transition-colors"
                                placeholder="Nombre traducido..."
                              />
                            ) : (
                              <div className="max-w-xs">
                                {sectionType.translationGenerating && !hasTranslation ? (
                                  <div className="flex items-center gap-2 text-xs text-amber-400">
                                    <Clock className="w-3.5 h-3.5 animate-spin" />
                                    <span>Generando traducción...</span>
                                  </div>
                                ) : hasTranslation ? (
                                  <p className="text-sm text-textSecondary">{sectionType.translatedName}</p>
                                ) : (
                                  <span className="text-xs text-textMuted italic">Sin traducir</span>
                                )}
                              </div>
                            )}
                          </td>

                          <td className="px-5 py-3.5 text-right">
                            {isEditing ? (
                              <div className="flex items-center justify-end gap-1">
                                <button
                                  onClick={() => saveTranslation(sectionType.sectionTypeId)}
                                  disabled={saving}
                                  className="p-2 hover:bg-emerald-500/20 rounded-full transition-colors text-emerald-400 disabled:opacity-50"
                                  title="Guardar"
                                >
                                  {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
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
                                onClick={() => startEdit(sectionType)}
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

          <div className="mt-4 px-4 py-3 rounded-xl bg-blue-500/5 border border-blue-500/20">
            <p className="text-xs text-blue-300/80">
              <strong>ℹ️ Info:</strong> Los tipos de sección sin traducción mostrarán su nombre original en español a los usuarios.
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
