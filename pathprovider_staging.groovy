job('pathprovider-build-staging') {
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/pathProviderBackend.git')
                name('master')
                branch('staging')
                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
            }
        }
    }
    steps {
        shell ('cd src/main/frontend\n' +
                'npm install\n' +
                'npm run build-prod')
        gradle {
            gradleName('Gradle Local')
            tasks('clean')
            tasks('build')
            useWrapper(false)
        }
    }
    publishers {
        artifactArchiver {
            artifacts('**/*.jar')
            onlyIfSuccessful(true)
        }
        extendedEmail {
            recipientList('$DEFAULT_RECIPIENTS')
            defaultSubject('$DEFAULT_SUBJECT')
            defaultContent('$DEFAULT_CONTENT')
            contentType('text/html')
            triggers {
                always() {
                    sendTo() {
                        recipientList()
                    }
                }
            }
        }
    }
}

job ('pathprovider-deploy-staging') {
    blockOn('/.*-deploy/')
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/pathProviderBackend.git')
                name('master')
                branch('staging')
                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
            }
        }
    }
    triggers {
        upstream('pathprovider-build-staging', 'SUCCESS')
    }
    steps {

        shell('#!/bin/sh\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" -X POST http://springboot-dev:8080/actuator/shutdown`\n' +
                'sleep 60\n' +
                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
                'echo server not shutdown\n' +
                'fi\n')
        shell('rm -f build/libs/pathprovider-0.0.1-SNAPSHOT.jar')
        copyArtifact {
            projectName('pathprovider-build-staging')
            filter('build/libs/*.jar')

        }

//        gradle {
//            gradleName('Gradle Local')
//            tasks('flywayClean')
//            useWrapper(false)
//        }
        shell('scp build/libs/pathprovider-0.0.1-SNAPSHOT.jar jenkins@springboot-dev:/home/jenkins/pathprovider-0.0.1-SNAPSHOT.jar')
        shell('BUILD_ID=dontKillMe\nssh jenkins@springboot-dev java -jar /home/jenkins/pathprovider-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev &')
        shell('#!/bin/sh\n' +
                'sleep 60\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" http://springboot-dev:8080/actuator/health`\n' +
                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
                'echo server did not start up correctly\n' +
                'exit -1\n' +
                'fi\n')
    }
    publishers {
        artifactArchiver {
            artifacts('**/*.jar')
            onlyIfSuccessful(true)
        }
        extendedEmail {
            recipientList('$DEFAULT_RECIPIENTS')
            defaultSubject('$DEFAULT_SUBJECT')
            defaultContent('$DEFAULT_CONTENT')
            contentType('text/html')
            triggers {
                always() {
                    sendTo() {
                        recipientList()
                    }
                }
            }
        }
    }
}

job ('pathprovider-functionalTest-staging') {
    blockOn('/.*-deploy/')
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/pathProviderBackend.git')
                name('master')
                branch('staging')
                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
            }
        }
    }
    triggers {
        upstream('pathprovider-deploy-staging', 'SUCCESS')
    }
    steps {
        gradle {
            gradleName('Gradle Local')
            tasks('flywayMigrate')
            tasks('integrationTests')
            useWrapper(false)
        }
    }
    publishers {

        extendedEmail {
            recipientList('$DEFAULT_RECIPIENTS')
            defaultSubject('$DEFAULT_SUBJECT')
            defaultContent('$DEFAULT_CONTENT')
            contentType('text/html')
            triggers {
                always() {
                    sendTo() {
                        recipientList()
                    }
                }
            }
        }
    }
}

// Deploying pathprovider to staging from development
job ('pathprovider-move-staging') {
    blockOn('/.*-deploy/')

    triggers {
        upstream('pathprovider-functionalTest-staging', 'SUCCESS')
    }

    steps {

        shell('#!/bin/sh\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" -X POST http://springboot-staging:8080/actuator/shutdown`\n' +
                'sleep 60\n' +
                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
                'echo staging server not shutdown\n' +
                'fi\n')

        shell('rm -f build/libs/pathprovider-0.0.1-SNAPSHOT.jar')

        shell('scp jenkins@springboot-dev:/home/jenkins/pathprovider-0.0.1-SNAPSHOT.jar jenkins@springboot-staging:/home/jenkins/pathprovider-0.0.1-SNAPSHOT.jar')
        shell('BUILD_ID=dontKillMe\nssh jenkins@springboot-staging java -jar /home/jenkins/pathprovider-0.0.1-SNAPSHOT.jar --spring.profiles.active=stage &')
        shell('#!/bin/sh\n' +
                'sleep 60\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" http://springboot-staging:8080/actuator/health`\n' +
                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
                'echo staging server did not start up correctly\n' +
                'exit -1\n' +
                'fi\n')

    }
    publishers {
        extendedEmail {
            recipientList('$DEFAULT_RECIPIENTS')
            defaultSubject('$DEFAULT_SUBJECT')
            defaultContent('$DEFAULT_CONTENT')
            contentType('text/html')
            triggers {
                always() {
                    sendTo() {
                        recipientList()
                    }
                }
            }
        }
    }
}


listView('Path Provider (Staging)') {
    description('Path Provider')
    jobs {
        regex('pathprovider-.+.-staging.*')
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}
