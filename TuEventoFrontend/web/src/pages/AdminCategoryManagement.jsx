import { useState, useEffect } from 'react';
import { 
  X, Plus, Edit2, Trash2, Tag, 
  CheckCircle, AlertCircle, ChevronRight
} from 'lucide-react';
import { getAllCategories, getRootCategories } from '../services/CategoryService';
import { httpRequest } from '../services/httpClient';
import AdminSidebar from '../components/admin/AdminSidebar';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export default function AdminCategoryManagement() {
  const [categories, setCategories] = useState([]);
  const [rootCategories, setRootCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [categoryFilter, setCategoryFilter] = useState('all');

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    dadId: null
  });

  const getParentCategoryName = (dadId) => {
    const parentCategory = categories.find(cat => cat.categoryId === dadId);
    return parentCategory ? parentCategory.name : 'Desconocida';
  };

  useEffect(() => {
    fetchCategories();
    fetchRootCategories();
  }, []);

  const fetchCategories = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await getAllCategories('es');
      setCategories(response.data || []);
    } catch (err) {
      setError('Error al cargar categorías: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchRootCategories = async () => {
    try {
      const response = await getRootCategories('es');
      setRootCategories(response.data || []);
    } catch (err) {
      console.warn('Error al cargar categorías principales:', err.message);
    }
  };

  const handleCreateCategory = async () => {
    if (!formData.name.trim()) return;
    
    setActionLoading(true);
    try {
      const response = await httpRequest(`${API_URL}/categories`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: formData.name.trim(),
          description: formData.description.trim() || null,
          dadId: formData.dadId || null
        }),
      });
      
      if (response.ok) {
        setShowCreateModal(false);
        resetForm();
        await fetchCategories();
        await fetchRootCategories();
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Error al crear categoría');
      }
    } catch (err) {
      setError('Error al crear categoría: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleEditCategory = async () => {
    if (!selectedCategory || !formData.name.trim()) return;
    
    setActionLoading(true);
    try {
      const response = await httpRequest(`${API_URL}/categories/${selectedCategory.categoryId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: formData.name.trim(),
          description: formData.description.trim() || null,
          dadId: formData.dadId || null
        }),
      });
      
      if (response.ok) {
        setShowEditModal(false);
        setSelectedCategory(null);
        resetForm();
        await fetchCategories();
        await fetchRootCategories();
      } else {
        const errorData = await response.json();
        setError(errorData.message || 'Error al actualizar categoría');
      }
    } catch (err) {
      setError('Error al actualizar categoría: ' + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleActive = async (category) => {
    setActionLoading(true);
    try {
      const endpoint = category.active ? 'deactivate' : 'activate';
      const response = await httpRequest(`${API_URL}/categories/${category.categoryId}/${endpoint}`, {
        method: 'PUT',
      });
      
      if (response.ok) {
        await fetchCategories();
        await fetchRootCategories();
      } else {
        const errorData = await response.json();
        setError(errorData.message || `Error al ${category.active ? 'desactivar' : 'activar'} categoría`);
      }
    } catch (err) {
      setError(`Error al ${category.active ? 'desactivar' : 'activar'} categoría: ` + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const openCreateModal = () => {
    resetForm();
    setShowCreateModal(true);
  };

  const openEditModal = (category) => {
    setSelectedCategory(category);
    setFormData({
      name: category.name || '',
      description: category.description || '',
      dadId: category.dadId || null
    });
    setShowEditModal(true);
  };

  const resetForm = () => {
    setFormData({
      name: '',
      description: '',
      dadId: null
    });
  };

  const getStatusStyle = (category) => {
    if (category.active) {
      return { bg: 'bg-emerald-500', label: 'Activa', icon: CheckCircle };
    }
    return { bg: 'bg-slate-500', label: 'Inactiva', icon: AlertCircle };
  };

  const filteredCategories = categories.filter(cat => {
    if (categoryFilter === 'all') return true;
    if (categoryFilter === 'active') return cat.active;
    if (categoryFilter === 'inactive') return !cat.active;
    if (categoryFilter === 'parents') return !cat.dadId;
    if (categoryFilter === 'subcategories') return cat.dadId;
    return true;
  });

  return (
    <div className="min-h-screen flex bg-background text-textPrimary font-sans">
      <AdminSidebar activePage="categories" />

      {/* Main */}
      <main className="flex-1 overflow-y-auto bg-background">
        <div className="p-8 max-w-6xl mx-auto w-full">

          {/* Page header */}
          <div className="mb-6">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-2xl font-black uppercase tracking-wide text-textPrimary mb-1">
                  Gestión de Categorías
                </h1>
                <p className="text-textSecondary text-sm leading-relaxed max-w-xl">
                  Gestiona las categorías de eventos del sistema. Crea categorías principales y subcategorías para organizar mejor los eventos.
                </p>
              </div>
              <button
                onClick={openCreateModal}
                className="flex items-center gap-2 px-4 py-2.5 bg-primary hover:bg-primaryDark text-textPrimary font-semibold rounded-xl transition-all"
              >
                <Plus className="w-4 h-4" />
                Nueva Categoría
              </button>
            </div>
          </div>

          {/* Filters */}
          <div className="flex items-center gap-2 mb-6">
            {[
              { key: 'all', label: 'Todas las Categorías' },
              { key: 'active', label: 'Activas' },
              { key: 'inactive', label: 'Inactivas' },
              { key: 'parents', label: 'Categorías Principales' },
              { key: 'subcategories', label: 'Subcategorías' }
            ].map(({ key, label }) => (
              <button
                key={key}
                onClick={() => setCategoryFilter(key)}
                className={`px-5 py-1.5 rounded-full text-sm font-semibold transition-all
                  ${categoryFilter === key
                    ? 'bg-primary text-textPrimary'
                    : 'bg-surfaceAlt/60 text-textSecondary hover:bg-surfaceAlt'
                  }`}
              >
                {label}
              </button>
            ))}
          </div>

          {/* Error Alert */}
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

          {/* Categories Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {loading ? (
              Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="bg-surface border border-surfaceAlt rounded-xl p-4 animate-pulse">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <div className="w-8 h-8 bg-surfaceAlt rounded-lg"></div>
                      <div className="w-24 h-4 bg-surfaceAlt rounded"></div>
                    </div>
                    <div className="w-16 h-6 bg-surfaceAlt rounded-full"></div>
                  </div>
                  <div className="w-full h-3 bg-surfaceAlt rounded mb-3"></div>
                  <div className="w-3/4 h-3 bg-surfaceAlt rounded"></div>
                </div>
              ))
            ) : filteredCategories.length === 0 ? (
              <div className="col-span-full text-center py-12 text-textMuted">
                <Tag className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <p>No hay categorías para mostrar</p>
              </div>
            ) : (
              filteredCategories.map((category) => {
                const status = getStatusStyle(category);
                const StatusIcon = status.icon;
                
                return (
                  <div
                    key={category.categoryId}
                    className="bg-surface border border-surfaceAlt rounded-xl p-4 hover:border-primary/40 transition-all group"
                  >
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center gap-2 flex-1 min-w-0">
                        <div className="w-8 h-8 bg-primary/20 rounded-lg flex items-center justify-center flex-shrink-0">
                          <Tag className="w-4 h-4 text-primary" />
                        </div>
                        <div className="min-w-0 flex-1">
                          <h3 className="font-semibold text-textPrimary truncate flex items-center">
                            {category.dadId && (
                              <span className="text-textMuted text-sm mr-2 flex items-center">
                                <ChevronRight className="w-3 h-3 mr-1" />
                              </span>
                            )}
                            {category.name}
                          </h3>
                          <div className="flex flex-wrap items-center gap-2 mt-1">
                            {category.dadId && (
                              <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-400 flex items-center gap-1">
                                <ChevronRight className="w-2.5 h-2.5" />
                                Subcategoría
                              </span>
                            )}
                            {!category.dadId && (
                              <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400">
                                Categoría Principal
                              </span>
                            )}
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-1">
                        <StatusIcon className="w-3 h-3" style={{ 
                          color: status.bg.includes('emerald') ? '#10b981' : '#6b7280' 
                        }} />
                      </div>
                    </div>

                    {category.description && (
                      <p className="text-xs text-textSecondary mb-3 line-clamp-2">
                        {category.description}
                      </p>
                    )}

                    {category.dadId && (
                      <div className="mb-3 p-2 rounded-lg bg-amber-500/10 border border-amber-500/20">
                        <div className="flex items-center gap-1 text-xs text-amber-400">
                          <ChevronRight className="w-3 h-3" />
                          <span className="font-semibold">Subcategoría de:</span>
                          <span>{getParentCategoryName(category.dadId)}</span>
                        </div>
                      </div>
                    )}

                    {/* Actions */}
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => openEditModal(category)}
                        className="p-2 hover:bg-primary/20 rounded-lg transition-colors text-primary"
                        title="Editar categoría"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => handleToggleActive(category)}
                        disabled={actionLoading}
                        className={`p-2 rounded-lg transition-colors ${
                          category.active 
                            ? 'hover:bg-red-500/20 text-red-400' 
                            : 'hover:bg-emerald-500/20 text-emerald-400'
                        }`}
                        title={category.active ? 'Desactivar' : 'Activar'}
                      >
                        {category.active ? 
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

      {/* Create Category Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-surface rounded-2xl border border-surfaceAlt shadow-2xl max-w-md w-full">
            <div className="flex items-center justify-between px-6 py-4 border-b border-surfaceAlt">
              <h2 className="text-lg font-bold text-textPrimary">Crear Nueva Categoría</h2>
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
                  Nombre de la Categoría *
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Ej: Música, Deportes, Arte..."
                  className="w-full px-3 py-2 bg-background border border-surfaceAlt rounded-lg text-textPrimary placeholder-textMuted focus:border-primary focus:outline-none"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-textPrimary mb-2">
                  Descripción
                </label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Descripción opcional de la categoría..."
                  rows={3}
                  className="w-full px-3 py-2 bg-background border border-surfaceAlt rounded-lg text-textPrimary placeholder-textMuted focus:border-primary focus:outline-none resize-none"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-textPrimary mb-2">
                  Categoría Padre
                  <span className="text-xs text-textMuted ml-2">(Opcional - para crear subcategorías)</span>
                </label>
                <select
                  value={formData.dadId || ''}
                  onChange={(e) => setFormData({ ...formData, dadId: e.target.value ? parseInt(e.target.value) : null })}
                  className="w-full px-3 py-2 bg-background border border-surfaceAlt rounded-lg text-textPrimary focus:border-primary focus:outline-none"
                >
                  <option value="">-- Categoría Principal (sin padre) --</option>
                  {rootCategories.map((category) => (
                    <option key={category.categoryId} value={category.categoryId}>
                      {category.name}
                    </option>
                  ))}
                </select>
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
                onClick={handleCreateCategory}
                disabled={!formData.name.trim() || actionLoading}
                className="flex-1 py-2 px-4 bg-primary hover:bg-primaryDark text-textPrimary rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {actionLoading ? 'Creando...' : 'Guardar Categoría'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Category Modal */}
      {showEditModal && selectedCategory && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-surface rounded-2xl border border-surfaceAlt shadow-2xl max-w-md w-full">
            <div className="flex items-center justify-between px-6 py-4 border-b border-surfaceAlt">
              <h2 className="text-lg font-bold text-textPrimary">Editar Categoría</h2>
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
                  Nombre de la Categoría *
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Ej: Música, Deportes, Arte..."
                  className="w-full px-3 py-2 bg-background border border-surfaceAlt rounded-lg text-textPrimary placeholder-textMuted focus:border-primary focus:outline-none"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-textPrimary mb-2">
                  Descripción
                </label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Descripción opcional de la categoría..."
                  rows={3}
                  className="w-full px-3 py-2 bg-background border border-surfaceAlt rounded-lg text-textPrimary placeholder-textMuted focus:border-primary focus:outline-none resize-none"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-textPrimary mb-2">
                  Categoría Padre
                  <span className="text-xs text-textMuted ml-2">(Opcional - para crear subcategorías)</span>
                </label>
                <select
                  value={formData.dadId || ''}
                  onChange={(e) => setFormData({ ...formData, dadId: e.target.value ? parseInt(e.target.value) : null })}
                  className="w-full px-3 py-2 bg-background border border-surfaceAlt rounded-lg text-textPrimary focus:border-primary focus:outline-none"
                >
                  <option value="">-- Categoría Principal (sin padre) --</option>
                  {rootCategories
                    .filter(category => category.categoryId !== selectedCategory?.categoryId)
                    .map((category) => (
                      <option key={category.categoryId} value={category.categoryId}>
                        {category.name}
                      </option>
                    ))}
                </select>
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
                onClick={handleEditCategory}
                disabled={!formData.name.trim() || actionLoading}
                className="flex-1 py-2 px-4 bg-primary hover:bg-primaryDark text-textPrimary rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {actionLoading ? 'Actualizando...' : 'Guardar Cambios'}
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
