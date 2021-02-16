pipeline {
    agent any
    tools {
        maven 'Maven'
        jdk 'JDK_8'
    }
    options {
        buildDiscarder(logRotator(artifactNumToKeepStr: '10'))
    }
    stages {
        stage ('Build') {
            steps {
                withMaven(options: [pipelineGraphPublisher(lifecycleThreshold: 'install')]) {
                    sh 'mvn clean install'
                }
            }
        }

        stage('Snapshot') {
            when {
                branch "master"
            }
            steps {
                sh 'mvn source:jar deploy -DskipTests'
            }
        }
    }
    post {
        always {
            deleteDir()
        }
    }
}