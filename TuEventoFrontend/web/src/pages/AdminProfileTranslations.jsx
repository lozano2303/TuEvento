import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Clock, Save, X, AlertCircle, Languages } from 'lucide-react';
import AdminSidebar from '../components/admin/AdminSidebar';
import * as TranslationService from '../services/TranslationService';

const AdminProfileTranslations = () => {
  const { lang } = useParams();
  const navigate = useNavigate();
  
  const [profiles, setProfiles] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [editValue, setEditValue] = useState('');
  const [filter, setFilter] = useState('not-translated');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const languages = {
    en: { 
      name: 'English',
      flag: (
        <svg className="w-6 h-4 rounded-sm" viewBox="0 0 60 30">
          <clipPath id="s"><path d="M0,0 v30 h60 v-30 z"/></clipPath>
          <clipPath id="t"><path d="M30,15 h30 v15 z v-30 h-30 z h-30 v15 z v-30 h30 z"/></clipPath>
          <g clipPath="url(#s)"><path d="M0,0 v30 h60 v-30 z" fill="#012169"/><path d="M0,0 L60,30 M60,0 L0,30" stroke="#fff" strokeWidth="6"/><path d="M0,0 L60,30 M60,0 L0,30" clipPath="url(#t)" stroke="#C8102E" strokeWidth="4"/><path d="M30,0 v30 M0,15 h60" stroke="#fff" strokeWidth="10"/><path d="M30,0 v30 M0,15 h60" stroke="#C8102E" strokeWidth="6"/></g>
        </svg>
      )
    },
    fr: { 
      name: 'Français',
      flag: (
        <svg className="w-6 h-4 rounded-sm" viewBox="0 0 900 600">
          <rect width="900" height="600" fill="#ED2939"/>
          <rect width="600" height="600" fill="#fff"/>
          <rect width="300" height="600" fill="#002395"/>
        </svg>
      )
    },
    pt: { 
      name: 'Português',
      flag: (
        <svg className="w-6 h-4 rounded-sm" viewBox="0 0 600 400">
          <rect width="600" height="400" fill="#FF0000"/>
          <rect width="240" height="400" fill="#006600"/>
          <circle cx="240" cy="200" r="80" fill="#FFFF00" stroke="#0000FF" strokeWidth="8"/>
        </svg>
      )
    }
  };

  const currentLang = languages[lang];
  const languageId = { en: 2, fr: 3, pt: 4 }[lang];

  useEffect(() => {
    loadProfiles();
  }, [lang, filter]);

  useEffect(() => {
    const interval = setInterval(() => {
      const hasGenerating = profiles.some(p => p.translationGenerating);
      if (hasGenerating) {
        loadProfiles();
      }
    }, 3000);

    return () => clearInterval(interval);
  }, [profiles]);

  const loadProfiles = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await TranslationService.getProfileTranslations(lang, filter);
      setProfiles(data || []);
    } catch (err) {
      setError('Error al cargar perfiles: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (profile) => {
    setEditingId(profile.profileId);
    setEditValue(profile.translatedBio || profile.bio || '');
  };

  const handleCancel = () => {
    setEditingId(null);
    setEditValue('');
  };

  const handleSave = async (profileId) => {
    try {
      await TranslationService.updateProfileTranslation(profileId, languageId, {
        translatedBio: editValue,
        status: 'PUBLISHED'
      });
      
      setEditingId(null);
      setEditValue('');
      await loadProfiles();
    } catch (err) {
      setError('Error al guardar traducción: ' + err.message);
    }
  };

  const getStatusBadge = (status) => {
    const styles = {
      'PENDING_REVIEW': 'bg-yellow-100 text-yellow-800',
      'PUBLISHED': 'bg-green-100 text-green-800',
      'DRAFT': 'bg-gray-100 text-gray-800'
    };
    
    const labels = {
      'PENDING_REVIEW': 'Necesita revisión',
      'PUBLISHED': 'Publicado',
      'DRAFT': 'Borrador'
    };
    
    return (
      <span className={`px-2 py-1 text-xs font-medium rounded ${styles[status] || styles.DRAFT}`}>
        {labels[status] || status}
      </span>
    );
  };

  const truncateText = (text, maxLength = 100) => {
    if (!text) return '-';
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  };

  if (!currentLang) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <AlertCircle className="mx-auto h-12 w-12 text-red-500 mb-4" />
          <h1 className="text-2xl font-bold text-textPrimary mb-2">Idioma no válido</h1>
          <button
            onClick={() => navigate('/admin-translations/en/profiles')}
            className="text-primary hover:text-primaryDark font-medium"
          >
            Volver a traducciones
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">
      <AdminSidebar activePage="translation-profiles" />
      
      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-7xl mx-auto w-full">
          {/* Header */}
          <div className="mb-6">
            <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1 flex items-center gap-3">
              <Languages className="w-6 h-6" />
              Traducciones de Perfiles
            </h1>
            <p className="text-textSecondary text-sm leading-relaxed">
              Gestiona las traducciones de biografías de perfiles de usuario
            </p>
          </div>

          {/* Language Selector */}
          <div className="mb-6">
            <div className="flex items-center gap-2 bg-surfaceAlt rounded-xl p-1">
              {Object.entries(languages).map(([code, info]) => (
                <button
                  key={code}
                  onClick={() => navigate(`/admin-translations/${code}/profiles`)}
                  className={`px-4 py-2 rounded-lg text-sm font-semibold transition-all flex items-center gap-2.5
                    ${lang === code
                      ? 'bg-primary text-textPrimary shadow-md'
                      : 'text-textMuted hover:text-textPrimary hover:bg-surface'
                    }`}
                >
                  {info.flag}
                  {info.name}
                </button>
              ))}
            </div>
          </div>

          {/* Filtros de visualización */}
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

          {/* Error Message */}
          {error && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-xl p-4 mb-6">
              <div className="flex items-center">
                <AlertCircle className="h-5 w-5 text-red-500 mr-2" />
                <span className="text-red-500">{error}</span>
              </div>
            </div>
          )}

          {/* Profiles Table */}
          <div className="bg-surface rounded-xl shadow-sm overflow-hidden border border-surfaceAlt">
            {loading ? (
              <div className="p-8 text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
                <p className="text-textSecondary mt-4">Cargando perfiles...</p>
              </div>
            ) : profiles.length === 0 ? (
              <div className="p-8 text-center text-textMuted">
                {filter === 'needs-review' ? 'No hay perfiles que requieran revisión' :
                 filter === 'not-translated' ? 'No hay perfiles sin traducir' :
                 filter === 'translated' ? 'No hay perfiles traducidos' :
                 'No hay perfiles disponibles'}
              </div>
            ) : (
              <table className="min-w-full divide-y divide-surfaceAlt">
                <thead className="bg-surfaceAlt">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                      Usuario
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                      Biografía Original
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                      Traducción
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                      Estado
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                      Acciones
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-surface divide-y divide-surfaceAlt">
                  {profiles.map((profile) => (
                    <tr key={profile.profileId} className="hover:bg-surfaceAlt/50">
                      <td className="px-6 py-4">
                        <div className="text-sm">
                          <div className="font-medium text-textPrimary">{profile.fullName}</div>
                          <div className="text-textMuted">ID: {profile.profileId}</div>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="text-sm text-textSecondary">
                          {truncateText(profile.bio, 100)}
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        {editingId === profile.profileId ? (
                          <textarea
                            value={editValue}
                            onChange={(e) => setEditValue(e.target.value)}
                            className="w-full px-3 py-2 border border-surfaceAlt rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent bg-background text-textPrimary"
                            rows="3"
                          />
                        ) : profile.translationGenerating ? (
                          <div className="flex items-center text-primary">
                            <Clock className="h-4 w-4 mr-2 animate-spin" />
                            <span className="text-sm">Generando traducción...</span>
                          </div>
                        ) : (
                          <div className="text-sm text-textSecondary">
                            {truncateText(profile.translatedBio || '-', 100)}
                          </div>
                        )}
                      </td>
                      <td className="px-6 py-4">
                        {profile.translationStatus && getStatusBadge(profile.translationStatus)}
                      </td>
                      <td className="px-6 py-4">
                        {editingId === profile.profileId ? (
                          <div className="flex space-x-2">
                            <button
                              onClick={() => handleSave(profile.profileId)}
                              className="p-2 text-green-600 hover:bg-green-500/10 rounded-lg"
                              title="Guardar"
                            >
                              <Save className="h-5 w-5" />
                            </button>
                            <button
                              onClick={handleCancel}
                              className="p-2 text-textMuted hover:bg-surfaceAlt rounded-lg"
                              title="Cancelar"
                            >
                              <X className="h-5 w-5" />
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={() => handleEdit(profile)}
                            disabled={profile.translationGenerating}
                            className="px-4 py-2 bg-primary text-textPrimary rounded-lg hover:bg-primaryDark disabled:bg-surfaceAlt disabled:text-textMuted disabled:cursor-not-allowed"
                          >
                            Editar
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </main>
    </div>
  );
};

export default AdminProfileTranslations;
