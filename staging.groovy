// Deploying pathprovider to staging from development
job ('pathprovider3-(staging)') {
    blockOn('/.*-deploy/')

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

job ('pathproviderV3-(staging)') {
    blockOn('/.*-deploy/')

    steps {

        shell('#!/bin/sh\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" -X POST http://springboot-staging:8080/actuator/shutdown`\n' +
                'sleep 60\n' +
                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
                'echo staging server not shutdown\n' +
                'fi\n')

        shell('rm -f build/libs/pathproviderV3-0.0.1-SNAPSHOT.jar')

        shell('scp jenkins@springboot-dev:/home/jenkins/pathproviderV3-0.0.1-SNAPSHOT.jar jenkins@springboot-staging:/home/jenkins/pathproviderV3-0.0.1-SNAPSHOT.jar')
        shell('BUILD_ID=dontKillMe\nssh jenkins@springboot-staging java -jar /home/jenkins/pathproviderV3-0.0.1-SNAPSHOT.jar --spring.profiles.active=stage &')
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

// Deploying sql reporter to staging from development
job ('sqlreporter-(staging)') {
    blockOn('/.*-deploy/')
    steps {

        shell('#!/bin/sh\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" -X POST http://springboot-staging:8081/sql/actuator/shutdown`\n' +
                'sleep 60\n' +
                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
                'echo server not shutdown\n' +
                'fi\n')

        shell('rm -f build/libs/sqlreporter-0.0.1-SNAPSHOT.jar')

        copyArtifact {
            projectName('sqlreporter-build')
            filter('build/libs/*.jar')

        }
        shell('scp jenkins@springboot-dev:/home/jenkins/sqlreporter-0.0.1-SNAPSHOT.jar jenkins@springboot-staging:/home/jenkins/sqlreporter-0.0.1-SNAPSHOT.jar')
        shell('BUILD_ID=dontKillMe\nssh jenkins@springboot-staging java -jar /home/jenkins/sqlreporter-0.0.1-SNAPSHOT.jar --spring.profiles.active=stage &')
        shell('#!/bin/sh\n' +
                'sleep 60\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" http://springboot-staging:8081/sql/actuator/health`\n' +
                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
                'echo server did not start up correctly\n' +
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



listView('Staging') {
    description('Staging')
    jobs {
        regex('.*-.(staging).*')
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
