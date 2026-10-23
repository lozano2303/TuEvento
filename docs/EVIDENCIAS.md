# Guía de Evidencias - Pipeline CI/CD TuEvento

Esta guía indica paso a paso cómo generar las evidencias requeridas para la guía de aprendizaje.

## 1. Pre-requisitos

Asegúrate de tener ejecutando Docker Desktop con los siguientes recursos:
- 4GB RAM mínimo (8GB recomendado)
- 2 CPUs virtuales

```bash
# Iniciar el stack de DevOps
docker-compose up -d

# Verificar que todos los servicios están corriendo
docker ps
```

## 2. Evidencias a Generar

### Evidencia 1: Jenkinsfile Funcional
✅ **Ya creado** en la raíz del proyecto

### Evidencia 2: Capturas de Ejecución Exitosa

**Paso 1:** Configurar Jenkins
```bash
# Obtener la contraseña inicial de Jenkins
docker logs tuevento-jenkins 2>&1 | grep -A 20 "Jenkins initial setup"
```

**Paso 2:** Acceder a Jenkins (http://localhost:8080)
- Usuario: admin
- Contraseña: (la obtenida del log)

**Paso 3:** Crear credenciales en Jenkins
1. Ve a **Manage Jenkins** > **Manage Credentials**
2. Agrega credencial con ID `docker-hub-credentials`:
   - Username: tu-usuario-docker
   - Password: tu-contraseña-docker
   - ID: docker-hub-credentials

**Paso 4:** Configurar SonarQube Server
1. Ve a **Manage Jenkins** > **Configure System**
2. En **SonarQube servers**, agrega:
   - Name: SonarQube
   - Server URL: http://sonarqube:9000

**Paso 5:** Crear Pipeline Job
1. **New Item** > **Pipeline**
2. Name: tu-evento-pipeline
3. **Pipeline** > **Definition**: Pipeline script from SCM
4. **SCM**: Git
5. **Repository URL**: https://github.com/tu-usuario/tu-evento.git
6. **Credentials**: (si es privado)
7. **Script Path**: Jenkinsfile

**Paso 6:** Ejecutar Pipeline
1. Click en **Build Now**
2. **Capturar pantalla** de cada stage exitoso:

| Stage | Captura Requerida |
|-------|-------------------|
| Checkout | ✅ |
| Clean & Compile | ✅ |
| Unit Tests | ✅ (con reporte) |
| SonarQube Analysis | ✅ |
| Quality Gate | ✅ |
| Build Docker Image | ✅ |
| Push to Registry | ✅ |
| Deploy to Kubernetes | ✅ |

### Evidencia 3: Repositorio GitHub Actualizado

```bash
git add .
git commit -m "feat: Complete CI/CD pipeline with frontend support"
git push origin main
```

### Evidencia 4: Documento Técnico
✅ **Ya creado** en `docs/PIPELINE-TECHNICAL-DOCUMENT.md`

### Evidencia 5: Evidencia del Despliegue en Kubernetes

```bash
# Si usas Minikube
minikube start

# Aplicar manifiestos
kubectl apply -f k8s/ -n tuevento

# Verificar deployments
kubectl get deployments -n tuevento
kubectl get pods -n tuevento
kubectl get services -n tuevento

# **Captura de pantalla** de:
# - kubectl get pods -n tuevento (todos los pods Running)
# - kubectl describe deployment tu-evento-backend -n tuevento
# - kubectl logs deployment/tu-evento-backend -n tuevento (últimos logs)
```

## 3. Checklist Final

- [ ] Jenkinsfile en repositorio
- [ ] Capturas de pipeline ejecutándose (mínimo 8 imágenes)
- [ ] Documento técnico del pipeline
- [ ] Evidencia de despliegue Kubernetes (comandos + screenshots)
- [ ] README actualizado con instrucciones de CI/CD

## 4. Comandos Útiles para Verificación

```bash
# Ver logs del backend en Kubernetes
kubectl logs -f deployment/tu-evento-backend -n tuevento

# Ver logs de Jenkins
docker logs -f tuevento-jenkins

# Ver análisis en SonarQube
# http://localhost:9002/dashboard?id=tu-evento-backend

# Verificar que el backend responde
curl http://localhost:8080/actuator/health
```

## 5. Estructura Final del Proyecto

```
TuEvento/
├── Jenkinsfile                 # ✅ Pipeline CI/CD
├── Dockerfile                  # Backend Dockerfile
├── docker-compose.yml          # Servicios DevOps
├── k8s/
│   ├── deployment.yaml         # Backend deployment
│   ├── frontend-deployment.yaml # ✅ Frontend deployment
│   ├── service.yaml            # Backend service
│   ├── frontend-service.yaml   # ✅ Frontend service
│   └── ...
├── TuEventoBackend/tu-evento/
│   ├── Dockerfile              # ✅ Backend Dockerfile
│   ├── pom.xml                 # ✅ Config Maven + plugins
│   └── sonar-project.properties # ✅ Config SonarQube
├── TuEventoFrontend/web/
│   ├── Dockerfile              # ✅ Frontend Dockerfile
│   └── nginx.conf              # ✅ Config Nginx
├── docs/
│   ├── PIPELINE-TECHNICAL-DOCUMENT.md # ✅ Documento técnico
│   └── PIPELINE-CHECKLIST.md   # ✅ Checklist
└── README.md                   # ✅ Actualizado
```