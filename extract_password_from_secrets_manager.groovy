pipeline {
    environment {
        username = "admin"
        password = ""
        secretName = "rds!db-9a935fef-2911-4f19-86e2-10dc56e2cbee"
        awsRegion = "us-east-1"
    }
    agent any

    stages {
        stage('Retrieve AWS Secret') {
            steps {
                script {
                    credentials = [
                        $class: 'AmazonWebServicesCredentialsBinding',
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        credentialsId: 'aws-sm-getsecretvalue',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
                    ]

                    withCredentials([credentials]) {
                        password = sh(script: "aws secretsmanager get-secret-value --region ${awsRegion} --secret-id ${secretName} | jq -r .SecretString | jq -r .password", returnStdout: true).trim()

                        echo "Retrieved password: ${password}"
                    }
                }
            }
        }
    }
}
