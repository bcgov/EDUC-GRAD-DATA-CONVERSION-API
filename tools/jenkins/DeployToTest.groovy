pipeline{
    agent {
        label 'maven'
    }
    environment{
        OCP_PROJECT = '77c02f-dev'
        IMAGE_PROJECT = '77c02f-tools'
        IMAGE_TAG = 'latest'
        APP_SUBDOMAIN_SUFFIX = '77c02f-test'
        APP_DOMAIN = 'apps.silver.devops.gov.bc.ca'
        TAG = 'test'
        REPO_NAME = 'educ-grad-algorithm-api'
        ORG = 'bcgov'
        BRANCH = 'main'
        SOURCE_REPO_URL = 'https://github.com/${ORG}/${REPO_NAME}'
        SOURCE_REPO_URL_RAW = 'https://raw.githubusercontent.com/${ORG}/${REPO_NAME}'
    }
    options {
        buildDiscarder(logRotator(daysToKeepStr: '', numToKeepStr: '5'))
    }
    parameters {
        choice( name: 'IMAGE_TAG', choices: ['main', 'release/1.0.0', 'dev' ] )
    }
    stages{
        stage('Deploy to TEST') {
            steps{
                script {
                    openshift.withCluster() {
                        openshift.withProject(OCP_PROJECT) {
                            openshift.apply(
                                    openshift.process("-f", "${SOURCE_REPO_URL_RAW}/${BRANCH}/tools/openshift/api.dc.yaml",
                                            "REPO_NAME=${REPO_NAME}", "HOST_ROUTE=${REPO_NAME}-${APP_SUBDOMAIN_SUFFIX}.${APP_DOMAIN}",
                                            "TAG_NAME=${params.IMAGE_TAG}")
                            )
                            openshift.selector("dc", "${REPO_NAME}-dc").rollout().latest()
                            timeout (time: 10, unit: 'MINUTES') {
                                openshift.selector("dc", "${REPO_NAME}-dc").rollout().status()
                            }
                        }
                    }
                }
            }
            post{
                success {
                    echo "${REPO_NAME} successfully deployed to TEST"
                    script {
                        openshift.withCluster() {
                            openshift.withProject(IMAGE_PROJECT) {
                                echo "Tagging image"
                                openshift.tag("${IMAGE_PROJECT}/${REPO_NAME}:latest", "${REPO_NAME}:${TAG}")
                            }
                        }
                    }
                }
                failure {
                    echo "${REPO_NAME} deployment to TEST Failed!"
                }
            }
        }
    }
}
