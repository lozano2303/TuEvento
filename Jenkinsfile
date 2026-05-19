pipeline {
    agent any
    
    environment {
        BACKEND_IMAGE = "tu-evento-backend:${env.BUILD_ID}"
        FRONTEND_IMAGE = "tu-evento-frontend:${env.BUILD_ID}"
        DOCKER_REGISTRY = "capysoft/tu-evento"
        SONARQUBE_SERVER = 'SonarQube'
        VITE_API_URL = 'http://localhost:8080'
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
                    sh 'mvn clean compile'
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                dir('TuEventoBackend/tu-evento') {
                    sh 'mvn test'
                }
                publishTestResults testResultsPattern: 'TuEventoBackend/tu-evento/target/surefire-reports/*.xml'
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                dir('TuEventoBackend/tu-evento') {
                    withSonarQubeEnv(env.SONARQUBE_SERVER) {
                        sh 'mvn sonar:sonar -Dsonar.projectKey=tu-evento-backend -Dsonar.host.url=http://sonarqube:9000'
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
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
                    docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
                        // Backend images
                        sh "docker tag ${BACKEND_IMAGE} ${DOCKER_REGISTRY}:${env.BUILD_ID}"
                        sh "docker tag ${BACKEND_IMAGE} ${DOCKER_REGISTRY}:latest"
                        sh "docker push ${DOCKER_REGISTRY}:${env.BUILD_ID}"
                        sh "docker push ${DOCKER_REGISTRY}:latest"
                        
                        // Frontend images
                        sh "docker tag ${FRONTEND_IMAGE} ${DOCKER_REGISTRY}:frontend-${env.BUILD_ID}"
                        sh "docker tag ${FRONTEND_IMAGE} ${DOCKER_REGISTRY}:frontend-latest"
                        sh "docker push ${DOCKER_REGISTRY}:frontend-${env.BUILD_ID}"
                        sh "docker push ${DOCKER_REGISTRY}:frontend-latest"
                    }
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    sh "kubectl set image deployment/tu-evento-backend tu-evento-backend=${DOCKER_REGISTRY}:${env.BUILD_ID} --record"
                    sh "kubectl rollout status deployment/tu-evento-backend"
                }
            }
        }
    }
    
    post {
        success {
            echo '✅ Pipeline completed successfully!'
            slackSend(
                color: 'good',
                message: "✅ TuEvento backend deployed successfully - Build ${env.BUILD_ID}"
            )
        }
        failure {
            echo '❌ Pipeline failed!'
            slackSend(
                color: 'danger',
                message: "❌ TuEvento backend deployment failed - Build ${env.BUILD_ID}"
            )
        }
        always {
            cleanWs()
        }
    }
}
