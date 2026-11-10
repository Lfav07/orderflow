
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

        stage('Build & Test - Inventory') {
            steps {
                dir('inventory-service') {
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
