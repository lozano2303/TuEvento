# Guía para Replicar Panel de Traducciones Admin

Esta guía explica cómo replicar el panel de traducciones administrativas implementado en **Categorías** para otros módulos (Section Types, Comments, Profiles, Notifications).

## Arquitectura Implementada

### Decisiones de Diseño
- **Español (ES)** es el idioma fuente - contenido creado directamente en tablas principales
- **Solo EN, FR, PT** se traducen - almacenadas en tablas `*_translation`
- **Un controlador por entidad** - endpoints independientes para cada módulo
- **Una página frontend por módulo** - identifica idioma desde URL parameter
- **Traducciones automáticas** - solicitadas al crear/actualizar entidad (via listener)
- **Polling activo** - frontend refresca automáticamente cuando hay traducciones generándose

---

## Backend: Estructura de Archivos

Para replicar en otro módulo (ejemplo: `SectionType`):

### 1. Controller de Traducciones
**Archivo:** `SectionTypeTranslationController.java`  
**Ruta:** `modules/sectiontype/interfaces/rest/`

**Responsabilidades:**
- Endpoint `GET /{languageCode}?filter=all|needs-review|translated|not-translated`
- Endpoint `PUT /{entityId}/language/{languageId}` - crear/actualizar traducción
- Endpoint `DELETE /{entityId}/language/{languageId}` - eliminar traducción
- Calcular campo `translationGenerating` verificando `TranslationJob` status

**Dependencias necesarias:**
```java
private final SectionTypeTranslationRepository sectionTypeTranslationRepository;
private final SectionTypeRepository sectionTypeRepository;
private final LanguageRepository languageRepository;
private final TranslationJobRepository translationJobRepository;
```

**Lógica del filtro `needs-review`:**
```java
// Necesita revisión si:
// 1. No tiene traducción
// 2. Traducción idéntica al original
// 3. Traducción solo difiere en mayúsculas/espacios
if (translationOpt.isEmpty()) {
    includeEntity = true;
    needsReview = true;
} else {
    String original = entity.getName();
    String translated = translation.getTranslatedName();
    
    if (translated == null || translated.trim().isEmpty()) {
        needsReview = true;
    } else if (original.equals(translated)) {
        needsReview = true;
    } else if (original.toLowerCase().trim().equals(translated.toLowerCase().trim())) {
        needsReview = true;
    }
}
```

**Calcular `translationGenerating`:**
```java
boolean translationGenerating = false;
if (translationOpt.isEmpty() || translationOpt.get().getTranslatedName() == null) {
    var existingJob = translationJobRepository.findByEntityAndTargetLanguage(
            "section_type", entity.getId(), languageId);
    if (existingJob.isPresent()) {
        var jobStatus = existingJob.get().getStatus();
        translationGenerating = jobStatus.name().equals("PENDING") || 
                                jobStatus.name().equals("PROCESSING");
    }
}
```

---

### 2. Listener de Eventos (para traducción automática)
**Archivo:** `SectionTypeTranslationListener.java`  
**Ruta:** `modules/sectiontype/application/listener/`

**Template:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SectionTypeTranslationListener {

    private final TranslationService translationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSectionTypeCreated(SectionTypeCreatedEvent event) {
        try {
            log.debug("Requesting translations for section type: id={}", event.getSectionTypeId());
            translationService.requestTranslation("section_type", event.getSectionTypeId().longValue(), "es");
            log.info("Translation jobs created for section type: id={}", event.getSectionTypeId());
        } catch (Exception e) {
            log.warn("Failed to request translations for section type {}: {}", event.getSectionTypeId(), e.getMessage());
        }
    }
}
```

**Importante:** Usa `@TransactionalEventListener(phase = AFTER_COMMIT)` para evitar race conditions.

---

### 3. Verificar Migración de Base de Datos

**Verificar que columnas de traducción sean NULLABLE:**

Si al guardar traducción sale error de `not-null constraint`, crear migración:

```yaml
databaseChangeLog:
  - changeSet:
      id: 0XX-fix-sectiontype-translation-description-nullable
      author: capysoft
      changes:
        - dropNotNullConstraint:
            tableName: section_type_translation
            columnName: translated_description
            columnDataType: VARCHAR(255)
```

---

## Frontend: Estructura de Archivos

### 1. Componente de Página
**Archivo:** `AdminSectionTypeTranslations.jsx`  
**Ruta:** `pages/`

**Estados necesarios:**
```javascript
const [entities, setEntities] = useState([]);
const [loading, setLoading] = useState(true);
const [saving, setSaving] = useState(false);
const [error, setError] = useState(null);
const [editingId, setEditingId] = useState(null);
const [editForm, setEditForm] = useState({ translatedName: '' });
const [filter, setFilter] = useState('needs-review');
```

**Hooks importantes:**

1. **Redirigir si idioma es español:**
```javascript
useEffect(() => {
  if (lang === 'es') {
    navigate('/admin-translations/en/section-types', { replace: true });
  }
}, [lang, navigate]);
```

2. **Cargar datos al cambiar idioma o filtro:**
```javascript
useEffect(() => {
  loadEntities();
}, [lang, filter]);
```

3. **Polling para traducciones generándose:**
```javascript
useEffect(() => {
  const hasGeneratingTranslations = entities.some(e => e.translationGenerating);
  if (!hasGeneratingTranslations) return;

  let pollCount = 0;
  const maxPolls = 40; // 2 minutos máximo

  const checkTranslations = async () => {
    pollCount++;
    if (pollCount > maxPolls) return;

    try {
      const response = await getEntityTranslations(lang, filter);
      setEntities(response.data || []);
    } catch (err) {
      console.error('Error checking translation status:', err);
    }
  };

  const pollInterval = setInterval(checkTranslations, 3000);
  return () => clearInterval(pollInterval);
}, [entities, lang, filter]);
```

**Indicador de traducción generándose:**
```jsx
{entity.translationGenerating && !hasTranslation ? (
  <div className="flex items-center gap-2 text-xs text-amber-400">
    <Clock className="w-3.5 h-3.5 animate-spin" />
    <span>Generando traducción...</span>
  </div>
) : hasTranslation ? (
  <p className="text-sm text-textSecondary">{entity.translatedName}</p>
) : (
  <span className="text-xs text-textMuted italic">Sin traducir</span>
)}
```

---

### 2. Servicio de Traducciones
**Archivo:** `TranslationService.js`  
**Ruta:** `services/`

**Agregar métodos:**
```javascript
// ========== SECTION TYPE TRANSLATIONS ==========

export const getSectionTypeTranslations = async (languageCode, filter = 'all') => {
  const response = await httpRequest(`${API_URL}/admin/section-type-translations/${languageCode}?filter=${filter}`);
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error al obtener traducciones' }));
    throw new Error(error.message || 'Error al obtener traducciones de tipos de sección');
  }
  return response.json();
};

export const updateSectionTypeTranslation = async (sectionTypeId, languageId, translationData) => {
  const response = await httpRequest(
    `${API_URL}/admin/section-type-translations/${sectionTypeId}/language/${languageId}`,
    {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(translationData),
    }
  );
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Error al actualizar traducción' }));
    throw new Error(error.message || 'Error al actualizar traducción');
  }
  return response.json();
};

export const createSectionTypeTranslation = updateSectionTypeTranslation;
```

---

### 3. Rutas y Navegación

**Agregar en `App.jsx`:**
```jsx
import AdminSectionTypeTranslations from './pages/AdminSectionTypeTranslations';

// En routes:
<Route path="/admin-translations/:lang/section-types" element={
  <ProtectedRoute requiredRoles={['ADMIN']}>
    <AdminSectionTypeTranslations />
  </ProtectedRoute>
} />

// En showNavbar:
&& !location.pathname.match(/^\/admin-translations/)
```

**Agregar en `AdminSidebar.jsx`:**
```jsx
<button className="w-full text-left px-4 py-2.5 text-sm text-textSecondary hover:bg-surfaceAlt transition-colors flex items-center gap-2"
  onClick={() => navigate('/admin-translations/en/section-types')}>
  Tipos de Sección
</button>
```

---

## Banderas SVG de Idiomas

```jsx
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
```

---

## Checklist de Implementación

### Backend
- [ ] Crear `EntityTranslationController` con filtros y `translationGenerating`
- [ ] Crear `EntityTranslationListener` con `@TransactionalEventListener(AFTER_COMMIT)`
- [ ] Verificar que tabla `*_translation` tenga columnas nullable (crear migración si es necesario)
- [ ] Agregar `TranslationJobRepository` como dependencia en controller
- [ ] Compilar y reiniciar backend

### Frontend
- [ ] Crear `AdminEntityTranslations.jsx` con polling y filtros
- [ ] Agregar métodos en `TranslationService.js`
- [ ] Agregar ruta en `App.jsx` con protección ADMIN
- [ ] Agregar link en `AdminSidebar.jsx` dropdown "Traducciones"
- [ ] Excluir ruta de navbar en `App.jsx`
- [ ] Importar icono `Clock` de lucide-react

### Testing
- [ ] Crear nueva entidad desde panel admin
- [ ] Verificar que aparece "Generando traducción..." en panel de traducciones
- [ ] Esperar 3-10 segundos y verificar que aparece traducción automática
- [ ] Editar traducción manualmente y guardar
- [ ] Probar filtros: Todos, Necesitan revisión, Sin traducir, Traducidos

---

## Módulos Pendientes

1. **Section Types** ✅ (listo para implementar)
2. **Event Ratings (Comments)** - Requiere manejo de replies
3. **Profiles** - Solo bio traducible
4. **Notifications** - title + body traducibles

---

## Notas Importantes

- **Race Condition:** Usar `@TransactionalEventListener(AFTER_COMMIT)` es CRÍTICO para evitar que el listener intente traducir antes de que la entidad se guarde
- **Nullable Descriptions:** Muchas entidades solo tienen `name`, no `description` - verificar constraints
- **Polling Límite:** 40 iteraciones × 3 seg = 2 minutos máximo
- **Filter Default:** Usar `needs-review` para mostrar solo traducciones problemáticas por defecto
- **TranslationSource:** Usar `MANUAL` (no existe `ADMIN` en el enum)
- **Language IDs:** ES=1, EN=2, FR=3, PT=4

---

## Ejemplo Completo: Section Types

### Backend Controller (simplificado)
```java
@RestController
@RequestMapping("/api/v1/admin/section-type-translations")
@RequiredArgsConstructor
@Slf4j
public class SectionTypeTranslationController {
    
    private final SectionTypeTranslationRepository sectionTypeTranslationRepository;
    private final SectionTypeRepository sectionTypeRepository;
    private final LanguageRepository languageRepository;
    private final TranslationJobRepository translationJobRepository;

    @GetMapping("/{languageCode}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWithTranslations(
            @PathVariable String languageCode,
            @RequestParam(defaultValue = "all") String filter) {
        // Implementar lógica similar a CategoryTranslationController
    }

    @PutMapping("/{sectionTypeId}/language/{languageId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SectionTypeTranslation>> createOrUpdate(
            @PathVariable Integer sectionTypeId,
            @PathVariable Integer languageId,
            @RequestBody Map<String, String> translationData) {
        // Implementar lógica similar a CategoryTranslationController
    }
}
```

### Frontend Service (simplificado)
```javascript
export const getSectionTypeTranslations = async (languageCode, filter = 'all') => {
  const response = await httpRequest(`${API_URL}/admin/section-type-translations/${languageCode}?filter=${filter}`);
  if (!response.ok) throw new Error('Error al obtener traducciones');
  return response.json();
};
```

---

**FIN DEL DOCUMENTO**
