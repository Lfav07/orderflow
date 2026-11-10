
pipeline {
    agent any

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
        success {
            echo 'Inventory CI pipeline ran successfully!'
        }

        failure {
            echo 'Inventory CI pipeline failed!'
        }
    }
}
