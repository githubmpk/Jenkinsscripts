job('pathprovider-build') {
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/pathProviderBackend.git')
                name('master')
                branch('develop')
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

job ('pathprovider-deploy') {
    blockOn('/.*-deploy/')
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/pathProviderBackend.git')
                name('master')
                branch('develop')
                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
            }
        }
    }
    triggers {
        upstream('pathprovider-build', 'SUCCESS')
    }
    steps {

        shell('#!/bin/sh\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" -X POST http://springboot-dev:8080/actuator/shutdown`\n' +
                'sleep 60\n' +
                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
                'echo server not shutdown\n' +
                'fi\n')
        shell('rm -f build/libs/pathproviderV3-0.0.1-SNAPSHOT.jar')
        copyArtifact {
            projectName('pathprovider-build')
            filter('build/libs/*.jar')

        }

//        gradle {
//            gradleName('Gradle Local')
//            tasks('flywayClean')
//            useWrapper(false)
//        }
        shell('scp build/libs/pathproviderV3-0.0.1-SNAPSHOT.jar jenkins@springboot-dev:/home/jenkins/pathproviderV3-0.0.1-SNAPSHOT.jar')
        shell('BUILD_ID=dontKillMe\nssh jenkins@springboot-dev java -jar /home/jenkins/pathproviderV3-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev &')
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

job ('pathprovider-functionalTest') {
    blockOn('/.*-deploy/')
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/pathProviderBackend.git')
                name('master')
                branch('develop')
                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
            }
        }
    }
    triggers {
        upstream('pathprovider-deploy', 'SUCCESS')
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


listView('Path Provider') {
    description('Path Provider')
    jobs {
        regex('pathprovider-.+')
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
