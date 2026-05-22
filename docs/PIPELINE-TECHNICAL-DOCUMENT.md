# Documento Técnico - Pipeline CI/CD TuEvento

## 1. Descripción General del Pipeline

El pipeline de TuEvento implementa un flujo completo de **CI/CD (Continuous Integration/Continuous Deployment)** utilizando Jenkins como motor de automatización. Este pipeline automatiza todo el proceso desde la obtención del código fuente hasta el despliegue en producción.

## 2. Arquitectura del Pipeline

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   GitHub    │────▶│   Jenkins   │────▶│   SonarQube │────▶│   Docker    │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                         │                                        │
                         ▼                                        ▼
                    ┌─────────────┐                      ┌─────────────┐
                    │    Maven    │                      │ Kubernetes  │
                    └─────────────┘                      └─────────────┘
```

## 3. Etapas del Pipeline (Stages)

### Stage 1: Checkout
- **Propósito**: Descarga el código fuente desde el repositorio Git
- **Comando**: `checkout scm`
- **Salida**: Código fuente disponible en workspace de Jenkins

### Stage 2: Clean & Compile
- **Propósito**: Compila el proyecto Spring Boot y resuelve dependencias
- **Directorio**: `TuEventoBackend/tu-evento`
- **Comando**: `mvn clean compile`
- **Salida**: Archivos compilados en `target/`

### Stage 3: Unit Tests
- **Propósito**: Ejecuta pruebas unitarias automatizadas
- **Comando**: `mvn test`
- **Salida**: Reporte de pruebas en `target/surefire-reports/*.xml`
- **Publicación**: Resultados publicados con `publishTestResults`

### Stage 4: SonarQube Analysis
- **Propósito**: Análisis de calidad de código estático
- **Herramienta**: SonarQube LTS Community
- **Métricas evaluadas**:
  - Bugs
  - Vulnerabilidades
  - Code Smells
  - Cobertura de código (>80% requerido)
  - Deuda técnica
- **Comando**: `mvn sonar:sonar`

### Stage 5: Quality Gate
- **Propósito**: Verifica que el código cumpla estándares de calidad
- **Timeout**: 5 minutos
- **Criterios de aprobación**:
  - Cobertura > 80%
  - 0 bugs críticos
  - 0 vulnerabilidades críticas
- **Bloqueo**: Si falla, detiene el pipeline

### Stage 6: Build Docker Image
- **Propósito**: Crea la imagen Docker del backend
- **Imagen base**: `eclipse-temurin:21-jre-alpine`
- **Comando**: `docker build -t ${IMAGE_NAME} .`
- **Tagging**: Usa BUILD_ID para versionado único

### Stage 7: Push to Registry
- **Propósito**: Sube la imagen al registro Docker Hub
- **Credenciales**: Almacenadas en Jenkins Credentials (docker-hub)
- **Tags generados**:
  - `${IMAGE}:${BUILD_ID}` (versión específica)
  - `${IMAGE}:latest` (última versión)

### Stage 8: Deploy to Kubernetes
- **Propósito**: Despliegue en el cluster Kubernetes
- **Comandos**:
  ```bash
  kubectl set image deployment/tu-evento-backend tu-evento-backend=${IMAGE}:${BUILD_ID} --record
  kubectl rollout status deployment/tu-evento-backend
  ```
- **Verificación**: Espera a que el rollout termine exitosamente

## 4. Herramientas Utilizadas

| Herramienta | Versión | Propósito |
|-------------|---------|-----------|
| Jenkins | LTS JDK17 | Orquestación del pipeline |
| SonarQube | LTS | Análisis de calidad |
| Docker | 24+ | Containerización |
| PostgreSQL | 17 | Base de datos |
| Redis | 7 | Cache y sesiones |
| MinIO | latest | Almacenamiento de archivos |

## 5. Variables de Entorno

```groovy
DOCKER_IMAGE = "tu-evento-backend:${BUILD_ID}"
DOCKER_REGISTRY = "capysoft/tu-evento"
SONARQUBE_SERVER = 'SonarQube'
```

## 6. Manejo de Credenciales

Las credenciales se manejan de forma segura mediante el **Jenkins Credentials Store**:
- `docker-hub-credentials`: Usuario/contraseña de Docker Hub
- Token SonarQube: Para autenticación en análisis

## 7. Notificaciones

El pipeline envía notificaciones vía Slack:
- **Éxito**: Mensaje verde con build ID
- **Fallo**: Mensaje rojo con build ID

## 8. Limpieza Post-Ejecución

- Workspace limpiado en stage `post` con `cleanWs()`
- Contenedores temporales eliminados automáticamente

## 9. Calidad de Código - Quality Gates

| Métrica | Umbral | Estado |
|---------|--------|--------|
| Cobertura | > 80% | ✅ Requerido |
| Bugs críticos | 0 | ✅ Bloqueado |
| Vulnerabilidades | 0 | ✅ Bloqueado |
| Code Smells | < 50 | ⚠️ Advertencia |

## 10. Troubleshooting

### Error: "Port already allocated"
**Causa**: Conflicto de puertos entre SonarQube y MinIO
**Solución**: Cambiar puerto SonarQube a 9002 en docker-compose.yml

### Error: "Quality Gate failed"
**Causa**: No se alcanzó el umbral de calidad
**Solución**: Revisar reporte en SonarQube, mejorar cobertura y corregir bugs

### Error: "kubectl command not found"
**Causa**: Kubernetes no configurado en agente
**Solución**: Verificar kubeconfig y credenciales en Jenkins

---

**Autor**: Jhampier Santos Ortiz  - Cristofer David Lozano Contreras
**Fecha**: 2026
**Versión**: 1.0