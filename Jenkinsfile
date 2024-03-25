def getUptime() {
    def fileContent = readFile('result.txt')
    def integerValue = fileContent.find(/\d+/)?.toInteger()
    return integerValue
}

pipeline {
  
  environment {
      username = "admin"
      mysecret = "rds!db-dbe838e2-f474-4249-b44f-8dc56ec605c4"
      aws_region = 'us-east-1'
      fpath="${WORKSPACE}"
      dsn = "asterisk-ipcc-db"
  }
  
  agent any
 
  stages {
      
      stage ('Retrieve secret') {
          steps {
                script {
                    
                    withCredentials([[
                        $class: 'AmazonWebServicesCredentialsBinding',
                        credentialsId: 'aws-sm-getsecretvalue',
                        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                            
                        def rds_password = sh(script: """aws secretsmanager get-secret-value --region ${aws_region} \
                        --secret-id ${mysecret} | jq -r .SecretString | jq -r .password""", returnStdout: true).trim()
                        
                        echo "${env.fpath}"

                        env.password=rds_password
                        echo "RDS DB Password: ${password}"
            
                    }
                }
            }
        }
      
      stage('Run Ansible Playbook') {
          steps {
              withEnv(["fpath=${env.fpath}","password=${env.password}"]) {
                  sh 'ansible-playbook -i inventory updatedb.yaml'
              }
          }
      }
      
      stage('Reboot Server') {
          steps {
              script {
                 
                  withEnv(["fpath=${env.fpath}"]) {
                  sh 'ansible-playbook -i inventory check_uptime.yaml'
                 }
                  
                  def prev_uptime=getUptime()
                 
                  sh 'ansible-playbook -i inventory reboot_server.yaml'
                 
                  withEnv(["fpath=${env.fpath}"]) {
                  sh 'ansible-playbook -i inventory check_uptime.yaml'
                 }
                 
                  def curr_uptime=getUptime()
                  def autoCancelled = false
                  
                  try {
                      if (curr_uptime < prev_uptime) {
                        echo "Asterisk Server Got Rebooted"
                       }else {
                           autoCancelled = true
                           error('Aborting the build as Asterisk Server didn\'t get rebooted')
                       }
                    }
                    
                  catch (e) {
                      if (autoCancelled) {
                          currentBuild.result = 'ABORTED'
                          //return here instead of throwing error to keep the build "green"
                          //return
                      }
                      throw e
                  }    
              }
          }
          
        }
        
      stage('Check DB Connectivity') {
          steps {
              withEnv(["dsn=${env.dsn}","username=${env.username}","password=${env.password}"]) {
                  sh 'ansible-playbook -i inventory db_connectivity.yaml'
              }
          }  
      }
      
  }
  
}  
