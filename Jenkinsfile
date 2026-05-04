pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = "tu-evento-backend:${env.BUILD_ID}"
        DOCKER_REGISTRY = "capysoft/tu-evento"
        SONARQUBE_SERVER = 'SonarQube'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Clean & Compile') {
            steps {
                dir('TuEventoBackend/tu-evento') {
                    sh 'mvn clean compile'
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                script {
                    // Iniciar contenedores externos para pruebas
                    sh 'docker compose up -d postgres redis'
                    // Esperar a que postgres esté listo
                    sh 'sleep 10'
                }
                dir('TuEventoBackend/tu-evento') {
                    sh 'mvn test'
                }
                publishTestResults testResultsPattern: 'TuEventoBackend/tu-evento/target/surefire-reports/*.xml'
            }
            post {
                always {
                    // Detener contenedores después de las pruebas
                    sh 'docker compose stop postgres redis'
                }
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
        
        stage('Build Docker Image') {
            steps {
                dir('TuEventoBackend/tu-evento') {
                    sh "docker build -t ${DOCKER_IMAGE} ."
                }
            }
        }
        
        stage('Push to Registry') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
                        sh "docker tag ${DOCKER_IMAGE} ${DOCKER_REGISTRY}:${env.BUILD_ID}"
                        sh "docker tag ${DOCKER_IMAGE} ${DOCKER_REGISTRY}:latest"
                        sh "docker push ${DOCKER_REGISTRY}:${env.BUILD_ID}"
                        sh "docker push ${DOCKER_REGISTRY}:latest"
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
            echo 'Pipeline exitoso! App desplegada.'
            echo "TuEvento backend deployed successfully - Build ${env.BUILD_ID}"
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
