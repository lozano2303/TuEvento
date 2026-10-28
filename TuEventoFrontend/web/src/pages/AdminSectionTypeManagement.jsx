import { useState, useEffect } from 'react';
import { 
  X, Plus, Edit2, Trash2, Layers,
  CheckCircle, AlertCircle
} from 'lucide-react';
import { getAllSectionTypes } from '../services/SectionTypeService';
import { httpRequest } from '../services/httpClient';
import AdminSidebar from '../components/admin/AdminSidebar';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8090/api/v1';

export default function AdminSectionTypeManagement() {
  const [sectionTypes, setSectionTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedSectionType, setSelectedSectionType] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [sectionTypeFilter, setSectionTypeFilter] = useState('all');

  const [formData, setFormData] = useState({
    name: ''
  });

  useEffect(() => {
    void fetchSectionTypes();
  }, []);

  const fetchSectionTypes = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await getAllSectionTypes();
      setSectionTypes(response.data || []);
    } catch (err) {
      setError('Error al cargar tipos de sección: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSectionType = async () => {
    if (!formData.name.trim()) return;
    
    setActionLoading(true);
    try {
      const response = await httpRequest(`${API_URL}/section-types`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: formData.name.trim()
        }),
      });
      
      if (response.ok) {
        setShowCreateModal(false);
        resetForm();
        await fetchSectionTypes();
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Error al crear tipo de sección');
      }
    } catch (err) {
      setError('Error al crear tipo de sección: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleEditSectionType = async () => {
    if (!selectedSectionType || !formData.name.trim()) return;
    
    setActionLoading(true);
    try {
      const response = await httpRequest(`${API_URL}/section-types/${selectedSectionType.sectionTypeId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: formData.name.trim()
        }),
      });
      
      if (response.ok) {
        setShowEditModal(false);
        setSelectedSectionType(null);
        resetForm();
        await fetchSectionTypes();
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Error al actualizar tipo de sección');
      }
    } catch (err) {
      setError('Error al actualizar tipo de sección: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleActive = async (sectionType) => {
    setActionLoading(true);
    try {
      const endpoint = sectionType.active ? 'deactivate' : 'activate';
      const response = await httpRequest(`${API_URL}/section-types/${sectionType.sectionTypeId}/${endpoint}`, {
        method: 'PUT',
      });
      
      if (response.ok) {
        await fetchSectionTypes();
      } else {
        const errorData = await response.json();
        setError(errorData.message || `Error al ${sectionType.active ? 'desactivar' : 'activar'} tipo de sección`);
      }
    } catch (err) {
      setError(`Error al ${sectionType.active ? 'desactivar' : 'activar'} tipo de sección: ` + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const openCreateModal = () => {
    resetForm();
    setShowCreateModal(true);
  };

  const openEditModal = (sectionType) => {
    setSelectedSectionType(sectionType);
    setFormData({
      name: sectionType.name || ''
    });
    setShowEditModal(true);
  };

  const resetForm = () => {
    setFormData({
      name: ''
    });
  };

  const getStatusStyle = (sectionType) => {
    if (sectionType.active) {
      return { bg: 'bg-emerald-500', label: 'Activo', icon: CheckCircle };
    }
    return { bg: 'bg-slate-500', label: 'Inactivo', icon: AlertCircle };
  };

  const filteredSectionTypes = sectionTypes.filter(st => {
    if (sectionTypeFilter === 'all') return true;
    if (sectionTypeFilter === 'active') return st.active;
    if (sectionTypeFilter === 'inactive') return !st.active;
    return true;
  });

  return (
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">
      <AdminSidebar activePage="section-types" />

      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-6xl mx-auto w-full">
          <div className="mb-6">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1">
                  Gestión de Tipos de Sección
                </h1>
                <p className="text-textSecondary text-sm leading-relaxed max-w-xl">
                  Gestiona los tipos de sección del sistema. Define categorías como General, VIP, Palco, Tribuna, Pista para organizar eventos.
                </p>
              </div>
              <button
                onClick={openCreateModal}
                className="flex items-center gap-2 px-4 py-2.5 bg-primary hover:bg-primaryDark text-textPrimary font-semibold rounded-xl transition-all"
              >
                <Plus className="w-4 h-4" />
                Nuevo Tipo
              </button>
            </div>
          </div>

          <div className="flex items-center gap-2 mb-6">
            {[
              { key: 'all', label: 'Todos los Tipos' },
              { key: 'active', label: 'Activos' },
              { key: 'inactive', label: 'Inactivos' }
            ].map(({ key, label }) => (
              <button
                key={key}
                onClick={() => setSectionTypeFilter(key)}
                className={`px-5 py-1.5 rounded-full text-sm font-semibold transition-all
                  ${sectionTypeFilter === key
                    ? 'bg-primary text-textPrimary'
                    : 'bg-surfaceAlt/60 text-textSecondary hover:bg-surfaceAlt'
                  }`}
              >
                {label}
              </button>
            ))}
          </div>

          {error && (
            <div className="mb-6 p-4 bg-error/10 border border-error/30 text-error rounded-xl flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span className="text-sm">{error}</span>
              <button 
                onClick={() => setError(null)}
                className="ml-auto p-1 hover:bg-error/20 rounded-full"
              >
                <X className="w-3 h-3" />
              </button>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {loading ? (
              Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="bg-surface border border-surfaceAlt rounded-xl p-4 animate-pulse">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <div className="w-8 h-8 bg-surfaceAlt rounded-lg"></div>
                      <div className="w-24 h-4 bg-surfaceAlt rounded"></div>
                    </div>
                  </div>
                </div>
              ))
            ) : filteredSectionTypes.length === 0 ? (
              <div className="col-span-full text-center py-12 text-textMuted">
                <Layers className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <p>No hay tipos de sección para mostrar</p>
              </div>
            ) : (
              filteredSectionTypes.map((sectionType) => {
                const status = getStatusStyle(sectionType);
                const StatusIcon = status.icon;
                
                return (
                  <div
                    key={sectionType.sectionTypeId}
                    className="bg-surface border border-surfaceAlt rounded-xl p-4 hover:border-primary/40 transition-all group"
                  >
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center gap-2 flex-1 min-w-0">
                        <div className="w-8 h-8 bg-primary/20 rounded-lg flex items-center justify-center flex-shrink-0">
                          <Layers className="w-4 h-4 text-primary" />
                        </div>
                        <div className="min-w-0 flex-1">
                          <h3 className="font-semibold text-textPrimary truncate">
                            {sectionType.name}
                          </h3>
                          <p className="text-xs text-textMuted mt-0.5">
                            ID: {sectionType.sectionTypeId}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-1">
                        <StatusIcon className="w-3 h-3" style={{ 
                          color: status.bg.includes('emerald') ? '#10b981' : '#6b7280' 
                        }} />
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => openEditModal(sectionType)}
                        className="p-2 hover:bg-primary/20 rounded-lg transition-colors text-primary"
                        title="Editar tipo de sección"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => handleToggleActive(sectionType)}
                        disabled={actionLoading}
                        className={`p-2 rounded-lg transition-colors ${
                          sectionType.active 
                            ? 'hover:bg-red-500/20 text-red-400' 
                            : 'hover:bg-emerald-500/20 text-emerald-400'
                        }`}
                        title={sectionType.active ? 'Desactivar' : 'Activar'}
                      >
                        {sectionType.active ? 
                          <Trash2 className="w-3.5 h-3.5" /> : 
                          <CheckCircle className="w-3.5 h-3.5" />
                        }
                      </button>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </main>

      {showCreateModal && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-surface rounded-2xl border border-surfaceAlt shadow-2xl max-w-md w-full">
            <div className="flex items-center justify-between px-6 py-4 border-b border-surfaceAlt">
              <h2 className="text-lg font-bold text-textPrimary">Crear Nuevo Tipo de Sección</h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="p-1.5 hover:bg-surfaceAlt rounded-full transition-colors text-textMuted"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-textPrimary mb-2">
                  Nombre del Tipo de Sección *
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Ej: General, VIP, Palco, Tribuna..."
                  className="w-full px-3 py-2 bg-background border border-surfaceAlt rounded-lg text-textPrimary placeholder-textMuted focus:border-primary focus:outline-none"
                />
              </div>

              <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-lg">
                <p className="text-xs text-amber-400">
                  <strong>Nota:</strong> Los tipos de sección se usan para categorizar las áreas de un evento (ej: VIP, General, Palco). Asegúrate de usar nombres claros y descriptivos.
                </p>
              </div>
            </div>
            
            <div className="flex gap-3 px-6 py-4 border-t border-surfaceAlt">
              <button
                onClick={() => setShowCreateModal(false)}
                className="flex-1 py-2 px-4 border border-surfaceAlt rounded-lg text-textSecondary hover:bg-surfaceAlt transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={handleCreateSectionType}
                disabled={!formData.name.trim() || actionLoading}
                className="flex-1 py-2 px-4 bg-primary hover:bg-primaryDark text-textPrimary rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {actionLoading ? 'Creando...' : 'Crear Tipo'}
              </button>
            </div>
          </div>
        </div>
      )}

      {showEditModal && selectedSectionType && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-surface rounded-2xl border border-surfaceAlt shadow-2xl max-w-md w-full">
            <div className="flex items-center justify-between px-6 py-4 border-b border-surfaceAlt">
              <h2 className="text-lg font-bold text-textPrimary">Editar Tipo de Sección</h2>
              <button
                onClick={() => setShowEditModal(false)}
                className="p-1.5 hover:bg-surfaceAlt rounded-full transition-colors text-textMuted"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-textPrimary mb-2">
                  Nombre del Tipo de Sección *
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Ej: General, VIP, Palco, Tribuna..."
                  className="w-full px-3 py-2 bg-background border border-surfaceAlt rounded-lg text-textPrimary placeholder-textMuted focus:border-primary focus:outline-none"
                />
              </div>
            </div>
            
            <div className="flex gap-3 px-6 py-4 border-t border-surfaceAlt">
              <button
                onClick={() => setShowEditModal(false)}
                className="flex-1 py-2 px-4 border border-surfaceAlt rounded-lg text-textSecondary hover:bg-surfaceAlt transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={handleEditSectionType}
                disabled={!formData.name.trim() || actionLoading}
                className="flex-1 py-2 px-4 bg-primary hover:bg-primaryDark text-textPrimary rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {actionLoading ? 'Guardando...' : 'Guardar Cambios'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
