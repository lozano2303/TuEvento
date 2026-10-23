# Pipeline CI/CD - Checklist de Validación

## ✅ Evidencias Requeridas

### 1. Jenkinsfile
- [x] Archivo Jenkinsfile en raíz del proyecto
- [x] Stage de Checkout implementado
- [x] Stage de compilación (Clean & Compile)
- [x] Stage de pruebas unitarias
- [x] Stage de análisis SonarQube
- [x] Stage de Quality Gate
- [x] Stage de construcción Docker
- [x] Stage de push a registry
- [x] Stage de despliegue Kubernetes

### 2. Dockerfile
- [x] Dockerfile para backend (TuEventoBackend/tu-evento/Dockerfile)
- [x] Dockerfile para frontend (TuEventoFrontend/web/Dockerfile)

### 3. Docker Compose
- [x] Servicios: Jenkins, SonarQube, PostgreSQL, Redis, MinIO
- [x] Configuración de redes y volúmenes
- [x] Puertos expuestos correctamente

### 4. Kubernetes Manifests
- [ ] deployment.yaml
- [ ] service.yaml
- [ ] ingress.yaml (opcional)
- [ ] secrets.yaml
- [ ] configmap.yaml

### 5. SonarQube Configuration
- [x] sonar-project.properties en backend

## 📋 Comandos de Verificación

```bash
# Verificar que Jenkinsfile es válido
pipeline-linter < Jenkinsfile

# Construir imágenes Docker
docker build -t tu-evento-backend ./TuEventoBackend/tu-evento
docker build -t tu-evento-frontend ./TuEventoFrontend/web

# Desplegar en Kubernetes
kubectl apply -f k8s/ -n tuevento

# Verificar pods
kubectl get pods -n tuevento

# Verificar logs del pipeline
docker logs tuevento-jenkins -f
```

## 🎯 Criterios de Evaluación

| Criterio | Peso | Evaluado |
|----------|------|----------|
| Jenkinsfile funcional | 25% | ✅ |
| Capturas de ejecución | 20% | ⬜ |
| Repositorio actualizado | 15% | ✅ |
| Documento técnico | 20% | ✅ |
| Evidencia K8s | 20% | ⬜ |

---

**Para completar las evidencias faltantes, ejecuta el pipeline en Jenkins y documenta:**

1. **Capturas de pantalla**:
   - Jenkins Dashboard mostrando pipeline
   - Stage de SonarQube Analysis exitoso
   - Stage de Build Docker exitoso
   - Stage de Deploy a Kubernetes exitoso

2. **Comandos Kubernetes**:
   ```bash
   # Después de ejecutar el pipeline
   kubectl get deployments -n tuevento
   kubectl get pods -n tuevento
   kubectl describe deployment tu-evento-backend -n tuevento
   ```

3. **URLs de acceso**:
   - Backend: http://localhost:8081 (modo dev)
   - Jenkins: http://localhost:8080
   - SonarQube: http://localhost:9002