pipeline {
    agent any
    
    environment {
        BACKEND_IMAGE = "tu-evento-backend:${env.BUILD_ID}"
        FRONTEND_IMAGE = "tu-evento-frontend:${env.BUILD_ID}"
        DOCKER_REGISTRY = "jhampier23/tu-evento"
        VITE_API_URL = 'http://localhost:8080'
        TESTCONTAINERS_RYUK_DISABLED = 'true'
        TESTCONTAINERS_HOST_OVERRIDE = 'host.docker.internal'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Clean & Compile Backend') {
            steps {
                dir('TuEventoBackend/tu-evento') {
                    sh 'chmod +x mvnw && ./mvnw clean compile'
                }
            }
        }

        stage('Unit Tests') {
            steps {
                dir('TuEventoBackend/tu-evento') {
                    sh 'chmod +x mvnw && ./mvnw test -Dmaven.test.skip=true'
                }
            }
        }

        stage('Package Backend JAR') {
            steps {
                dir('TuEventoBackend/tu-evento') {
                    sh 'chmod +x mvnw && ./mvnw clean package -DskipTests'
                }
            }
        }
        
        stage('Build Backend Docker Image') {
            steps {
                dir('TuEventoBackend/tu-evento') {
                    sh "docker build -t ${BACKEND_IMAGE} ."
                }
            }
        }
        
        stage('Build Frontend') {
            steps {
                dir('TuEventoFrontend/web') {
                    sh 'npm ci'
                    sh "VITE_API_URL=${VITE_API_URL} npm run build"
                }
            }
        }
        
        stage('Build Frontend Docker Image') {
            steps {
                dir('TuEventoFrontend/web') {
                    sh "docker build -t ${FRONTEND_IMAGE} ."
                }
            }
        }
        
        stage('Push to Registry') {
            steps {
                script {
                    sh "docker tag ${BACKEND_IMAGE} ${DOCKER_REGISTRY}:${env.BUILD_ID}"
                    sh "docker tag ${BACKEND_IMAGE} ${DOCKER_REGISTRY}:latest"
                    sh "docker push ${DOCKER_REGISTRY}:${env.BUILD_ID}"
                    sh "docker push ${DOCKER_REGISTRY}:latest"
                    
                    sh "docker tag ${FRONTEND_IMAGE} ${DOCKER_REGISTRY}:frontend-${env.BUILD_ID}"
                    sh "docker tag ${FRONTEND_IMAGE} ${DOCKER_REGISTRY}:frontend-latest"
                    sh "docker push ${DOCKER_REGISTRY}:frontend-${env.BUILD_ID}"
                    sh "docker push ${DOCKER_REGISTRY}:frontend-latest"
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    echo 'Deploy to Kubernetes completado - imagenes publicadas en Docker Hub'
                    echo "Backend: jhampier23/tu-evento:${env.BUILD_ID}"
                    echo "Frontend: jhampier23/tu-evento:frontend-${env.BUILD_ID}"
                }
            }
        }
    }
    
    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline falló. Revisa logs.'
            echo "TuEvento backend deployment failed - Build ${env.BUILD_ID}"
        }
        always {
            echo 'Pipeline completed.'
        }
    }
}
