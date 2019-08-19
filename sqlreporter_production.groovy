job('sqlreporter-build-production') {
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/sqlreporter.git')
                name('master')
                branch('master')
                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
            }
        }
    }
    steps {
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

job ('sqlreporter-deploy-production') {
    blockOn('/.*-deploy/')
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/sqlreporter.git')
                name('master')
                branch('master')
                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
            }
        }
    }
    triggers {
        upstream('sqlreporter-build-production', 'SUCCESS')
    }
    steps {

        shell('#!/bin/sh\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" -X POST http://springboot-dev:8081/sql/actuator/shutdown`\n' +
                'sleep 60\n' +
                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
                'echo server not shutdown\n' +
                'fi\n')
        shell('rm -f build/libs/sqlreporter-0.0.1-SNAPSHOT.jar')
        copyArtifact {
            projectName('sqlreporter-build-staging')
            filter('build/libs/*.jar')

        }
        shell('scp build/libs/sqlreporter-0.0.1-SNAPSHOT.jar jenkins@springboot-dev:/home/jenkins/sqlreporter-0.0.1-SNAPSHOT.jar')
        shell('BUILD_ID=dontKillMe\nssh jenkins@springboot-dev java -jar /home/jenkins/sqlreporter-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev &')
        shell('#!/bin/sh\n' +
                'sleep 60\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" http://springboot-dev:8081/sql/actuator/health`\n' +
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

// Deploying sql reporter to staging from development
job ('sqlreporter-move-production') {
    blockOn('/.*-deploy/')
    steps {

        triggers {
            upstream('sqlreporter-deploy-production', 'SUCCESS')
        }

        shell('#!/bin/sh\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" -X POST http://springboot-staging:8081/sql/actuator/shutdown`\n' +
                'sleep 60\n' +
                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
                'echo server not shutdown\n' +
                'fi\n')

        shell('rm -f build/libs/sqlreporter-0.0.1-SNAPSHOT.jar')

        copyArtifact {
            projectName('sqlreporter-build-staging')
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


listView('Sql Reporter') {
    description('Sql Reporter')
    jobs {
        regex('sqlreporter-.+')
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
