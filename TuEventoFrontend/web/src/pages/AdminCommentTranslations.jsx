import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Clock, Save, X, AlertCircle, Languages } from 'lucide-react';
import AdminSidebar from '../components/admin/AdminSidebar';
import * as TranslationService from '../services/TranslationService';
import { httpRequest } from '../services/httpClient';

const AdminCommentTranslations = () => {
  const { lang } = useParams();
  const navigate = useNavigate();
  
  const [events, setEvents] = useState([]);
  const [selectedEventId, setSelectedEventId] = useState(null);
  const [comments, setComments] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [editValue, setEditValue] = useState('');
  const [filter, setFilter] = useState('not-translated');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [loadingEvents, setLoadingEvents] = useState(true);

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
    void loadEvents();
  }, []);

  useEffect(() => {
    if (selectedEventId) {
      void loadComments();
    }
  }, [lang, selectedEventId, filter]);

  useEffect(() => {
    if (!selectedEventId) return;

    const interval = setInterval(() => {
      const hasGenerating = comments.some(c => c.translationGenerating);
      if (hasGenerating) {
        void loadComments();
      }
    }, 3000);

    return () => clearInterval(interval);
  }, [comments, selectedEventId]);

  const loadEvents = async () => {
    try {
      setLoadingEvents(true);
      setError('');
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8090/api/v1';
      const response = await httpRequest(`${apiUrl}/admin/events`);
      if (!response.ok) {
        const errorText = await response.text();
        console.error('Error response:', errorText);
        throw new Error('Error al cargar eventos');
      }
      const data = await response.json();
      // La respuesta tiene estructura: {success: true, message: '...', data: [...]}
      const eventsList = Array.isArray(data.data) ? data.data : (Array.isArray(data) ? data : []);
      setEvents(eventsList);
      if (eventsList.length > 0) {
        setSelectedEventId(eventsList[0].eventId);
      }
    } catch (err) {
      console.error('Error loading events:', err);
      setError('Error al cargar eventos: ' + err.message);
      setEvents([]); // Asegurar que events sea un array vacío en caso de error
    } finally {
      setLoadingEvents(false);
    }
  };

  const loadComments = async () => {
    if (!selectedEventId) return;
    
    try {
      setLoading(true);
      setError('');
      const data = await TranslationService.getCommentTranslations(lang, selectedEventId, filter);
      setComments(data);
    } catch (err) {
      setError('Error al cargar comentarios: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (comment) => {
    setEditingId(comment.ratingId);
    setEditValue(comment.translatedComment || comment.comment || '');
  };

  const handleCancel = () => {
    setEditingId(null);
    setEditValue('');
  };

  const handleSave = async (ratingId) => {
    try {
      await TranslationService.updateCommentTranslation(ratingId, languageId, {
        translatedComment: editValue,
        status: 'PUBLISHED'
      });
      
      setEditingId(null);
      setEditValue('');
      await loadComments();
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
          <AlertCircle className="mx-auto h-12 w-12 text-error mb-4" />
          <h1 className="text-2xl font-bold text-textPrimary mb-2">Idioma no válido</h1>
          <button
            onClick={() => navigate('/admin-translations/en/comments')}
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
      <AdminSidebar activePage="translation-comments" />
      
      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-7xl mx-auto w-full">
          {/* Header */}
          <div className="mb-6 flex items-start justify-between">
            <div>
              <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1 flex items-center gap-3">
                <Languages className="w-6 h-6" />
                Traducciones de Comentarios
              </h1>
              <p className="text-textSecondary text-sm leading-relaxed">
                Gestiona las traducciones de comentarios de eventos
              </p>
            </div>
          </div>

          {/* Language Selector and Event Selector */}
          <div className="mb-6 space-y-4">
            {/* Language Selector */}
            <div className="flex items-center gap-2 bg-surfaceAlt rounded-xl p-1">
              {Object.entries(languages).map(([code, info]) => (
                <button
                  key={code}
                  onClick={() => navigate(`/admin-translations/${code}/comments`)}
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

            {/* Event Selector */}
            <div className="bg-surface rounded-xl shadow-sm p-4 border border-surfaceAlt">
              <label className="block text-sm font-medium text-textSecondary mb-2">
                Seleccionar evento:
              </label>
              <select
                value={selectedEventId || ''}
                onChange={(e) => setSelectedEventId(Number(e.target.value))}
                className="w-full px-4 py-2 border border-surfaceAlt rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent bg-background text-textPrimary"
                disabled={loadingEvents}
              >
                {loadingEvents ? (
                  <option>Cargando eventos...</option>
                ) : !Array.isArray(events) || events.length === 0 ? (
                  <option>No hay eventos disponibles</option>
                ) : (
                  events.map(event => (
                    <option key={event.eventId} value={event.eventId}>
                      {event.eventName || event.name}
                    </option>
                  ))
                )}
              </select>
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
                <AlertCircle className="h-5 w-5 text-error mr-2" />
                <span className="text-error">{error}</span>
              </div>
            </div>
          )}

          {/* Comments Table */}
          <div className="bg-surface rounded-xl shadow-sm overflow-hidden border border-surfaceAlt">
            {loading ? (
              <div className="p-8 text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
                <p className="text-textSecondary mt-4">Cargando comentarios...</p>
              </div>
            ) : comments.length === 0 ? (
              <div className="p-8 text-center text-textMuted">
                {!selectedEventId ? 'Selecciona un evento para ver sus comentarios' :
                 filter === 'needs-review' ? 'No hay comentarios que requieran revisión' :
                 filter === 'not-translated' ? 'No hay comentarios sin traducir' :
                 filter === 'translated' ? 'No hay comentarios traducidos' :
                 'No hay ningún comentario en este evento'}
              </div>
            ) : (
              <table className="min-w-full divide-y divide-surfaceAlt">
                <thead className="bg-surfaceAlt">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">
                      Comentario Original
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
                  {comments.map((comment) => (
                    <tr key={comment.ratingId} className="hover:bg-surfaceAlt/50">
                      <td className="px-6 py-4">
                        <div className="text-sm text-textPrimary">
                          {truncateText(comment.comment, 100)}
                        </div>
                        {comment.rating && (
                          <div className="text-xs text-textMuted mt-1">
                            Rating: {comment.rating}/5
                          </div>
                        )}
                      </td>
                      <td className="px-6 py-4">
                        {editingId === comment.ratingId ? (
                          <textarea
                            value={editValue}
                            onChange={(e) => setEditValue(e.target.value)}
                            className="w-full px-3 py-2 border border-surfaceAlt rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent bg-background text-textPrimary"
                            rows="3"
                          />
                        ) : comment.translationGenerating ? (
                          <div className="flex items-center text-primary">
                            <Clock className="h-4 w-4 mr-2 animate-spin" />
                            <span className="text-sm">Generando traducción...</span>
                          </div>
                        ) : (
                          <div className="text-sm text-textSecondary">
                            {truncateText(comment.translatedComment || '-', 100)}
                          </div>
                        )}
                      </td>
                      <td className="px-6 py-4">
                        {comment.translationStatus && getStatusBadge(comment.translationStatus)}
                      </td>
                      <td className="px-6 py-4">
                        {editingId === comment.ratingId ? (
                          <div className="flex space-x-2">
                            <button
                              onClick={() => handleSave(comment.ratingId)}
                              className="p-2 text-success hover:bg-green-500/10 rounded-lg"
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
                            onClick={() => handleEdit(comment)}
                            disabled={comment.translationGenerating}
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

export default AdminCommentTranslations;
