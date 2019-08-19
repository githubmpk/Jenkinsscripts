job('sqlreporter-build') {
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/sqlreporter.git')
                name('master')
                branch('develop')
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

job ('sqlreporter-deploy') {
    blockOn('/.*-deploy/')
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/sqlreporter.git')
                name('master')
                branch('develop')
                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
            }
        }
    }
    triggers {
        upstream('sqlreporter-build', 'SUCCESS')
    }
    steps {

        shell('#!/bin/sh\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" -X POST http://springboot-dev:8081/actuator/shutdown`\n' +
                'sleep 60\n' +
                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
                'echo server not shutdown\n' +
                'fi\n')
        shell('rm -f build/libs/sqlreporter-0.0.1-SNAPSHOT.jar')
        copyArtifact {
            projectName('sqlreporter-build')
            filter('build/libs/*.jar')

        }
        shell('scp build/libs/sqlreporter-0.0.1-SNAPSHOT.jar jenkins@springboot-dev:/home/jenkins/sqlreporter-0.0.1-SNAPSHOT.jar')
        shell('BUILD_ID=dontKillMe\nssh jenkins@springboot-dev java -jar /home/jenkins/sqlreporter-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev &')
        shell('#!/bin/sh\n' +
                'sleep 60\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" http://springboot-dev:8081/actuator/health`\n' +
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
