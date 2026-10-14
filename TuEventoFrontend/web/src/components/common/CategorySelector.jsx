import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useLanguageSync } from '../../hooks/useLanguageSync';
import { getActiveCategories } from '../../services/CategoryService';
import { Clock, ChevronDown, Tag } from 'lucide-react';

/**
 * Componente selector de categorías con soporte completo para traducciones dinámicas.
 * Ejemplifica el patrón híbrido: i18n para UI + traducciones dinámicas para contenido.
 */
const CategorySelector = ({ 
  selectedCategory, 
  onCategoryChange, 
  placeholder = null,
  showTranslationIndicators = true 
}) => {
  const { t } = useTranslation();
  const currentLanguage = useLanguageSync(); // Sincroniza automáticamente con idioma preferido
  
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isOpen, setIsOpen] = useState(false);
  const [error, setError] = useState(null);

  // Auto-reload cuando se detecta que alguna traducción está generándose
  useEffect(() => {
    const hasGeneratingTranslations = categories.some(cat => cat.translationGenerating);
    if (hasGeneratingTranslations) {
      const timer = setTimeout(() => {
        loadCategories(); // Recargar categorías
      }, 20000); // 20 segundos

      return () => clearTimeout(timer);
    }
  }, [categories]);

  const loadCategories = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Cargar categorías con el idioma actual del usuario
      const response = await getActiveCategories(currentLanguage);
      setCategories(response.data || []);
    } catch (err) {
      setError(err.message);
      setCategories([]);
    } finally {
      setLoading(false);
    }
  };

  // Cargar categorías cuando cambie el idioma
  useEffect(() => {
    loadCategories();
  }, [currentLanguage]);

  const handleCategorySelect = (category) => {
    onCategoryChange(category);
    setIsOpen(false);
  };

  const renderCategoryOption = (category) => {
    // Usar nombre traducido si está disponible, sino el original
    const displayName = category.translatedName || category.name;
    const displayDescription = category.translatedDescription || category.description;

    return (
      <div
        key={category.categoryId}
        onClick={() => handleCategorySelect(category)}
        className="px-4 py-3 hover:bg-primary/10 cursor-pointer transition-colors border-b border-surfaceAlt/50 last:border-b-0"
      >
        <div className="flex items-start justify-between gap-3">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <Tag className="w-4 h-4 text-primary flex-shrink-0" />
              <span className="font-medium text-textPrimary truncate">{displayName}</span>
              
              {/* Indicador de traducción */}
              {showTranslationIndicators && category.languageCode && !category.translationGenerating && (
                <span className="text-[9px] px-1.5 py-0.5 rounded-full bg-primary/20 border border-primary/40 text-accent font-bold uppercase">
                  {t('categories.translation.translated', { lang: category.languageCode.toUpperCase() })}
                </span>
              )}
            </div>
            
            {displayDescription && (
              <p className="text-xs text-textSecondary line-clamp-2 ml-6">
                {displayDescription}
              </p>
            )}
          </div>

          {/* Indicador de traducción generándose */}
          {showTranslationIndicators && category.translationGenerating && (
            <div className="flex items-center gap-1 text-xs text-yellow-400 flex-shrink-0">
              <Clock className="w-3 h-3 animate-spin" />
              <span className="hidden sm:inline">{t('categories.translation.generating')}</span>
            </div>
          )}
        </div>
      </div>
    );
  };

  if (loading) {
    return (
      <div className="relative">
        <div className="w-full px-4 py-3 rounded-xl bg-surface border border-surfaceAlt flex items-center justify-center gap-2 text-textMuted">
          <Clock className="w-4 h-4 animate-spin" />
          <span className="text-sm">{t('common.loading')}</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="relative">
        <div className="w-full px-4 py-3 rounded-xl bg-error/10 border border-error/30 text-error text-sm">
          {error}
        </div>
      </div>
    );
  }

  return (
    <div className="relative">
      {/* Selector principal */}
      <div
        onClick={() => setIsOpen(!isOpen)}
        className="w-full px-4 py-3 rounded-xl bg-surface border border-surfaceAlt hover:border-primary/40 cursor-pointer transition-all flex items-center justify-between gap-3"
      >
        <div className="flex-1 min-w-0">
          {selectedCategory ? (
            <div className="flex items-center gap-2">
              <Tag className="w-4 h-4 text-primary flex-shrink-0" />
              <span className="font-medium text-textPrimary truncate">
                {selectedCategory.translatedName || selectedCategory.name}
              </span>
              
              {/* Badge de traducción en el elemento seleccionado */}
              {showTranslationIndicators && selectedCategory.languageCode && (
                <span className="text-[9px] px-1.5 py-0.5 rounded-full bg-primary/20 text-accent">
                  {selectedCategory.languageCode.toUpperCase()}
                </span>
              )}
            </div>
          ) : (
            <span className="text-textMuted">
              {placeholder || t('categories.selectCategory')}
            </span>
          )}
        </div>
        
        <ChevronDown className={`w-4 h-4 text-textMuted transition-transform ${isOpen ? 'rotate-180' : ''}`} />
      </div>

      {/* Dropdown */}
      {isOpen && (
        <div className="absolute top-full left-0 right-0 mt-2 bg-surface border border-surfaceAlt rounded-xl shadow-lg overflow-hidden z-50 max-h-64 overflow-y-auto">
          {categories.length === 0 ? (
            <div className="px-4 py-6 text-center text-textMuted">
              {t('categories.noCategories')}
            </div>
          ) : (
            categories.map(renderCategoryOption)
          )}
        </div>
      )}

      {/* Backdrop para cerrar */}
      {isOpen && (
        <div 
          className="fixed inset-0 z-40" 
          onClick={() => setIsOpen(false)}
        />
      )}
    </div>
  );
};

export default CategorySelector;