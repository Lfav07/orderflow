
pipeline {
    agent { label 'docker' }

    options {
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Check Docker') {
            steps {
                sh 'docker info'
            }
        }

        stage('Build & Test - Inventory') {
            steps {
                dir('services/inventory') {
                    sh 'mvn -B clean verify'
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'services/inventory/target/surefire-reports/TEST-*.xml, services/inventory/target/failsafe-reports/TEST-*.xml'
        }

        success {
            echo 'Inventory CI pipeline ran successfully!'
        }

        failure {
            echo 'Inventory CI pipeline failed!'
        }
    }
}
