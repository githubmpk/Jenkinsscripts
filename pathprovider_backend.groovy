

job('pp3Backend-build') {
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/pathprovider_v3/backend.git')
                name('master')
                branch('master')
                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
            }
        }
    }
    steps {
        gradle {
            gradleName('Gradle Local')

            tasks('docker')
            useWrapper(false)
        }
        shell('#!/bin/sh\n' +
                'docker tag pathproviderv3 springboot-test:5000/pathcare/pathproviderv3\n' +
                'docker push springboot-test:5000/pathcare/pathproviderv3')
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

job ('pp3Backend-deploy') {
    blockOn('/.*-deploy/')

    triggers {
        upstream('pp3Backend-build', 'SUCCESS')
    }
    steps {

        shell('#!/bin/sh\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" -X POST http://springboot-test:8080/actuator/shutdown`\n' +
                'sleep 60\n' +
                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
                'echo server not shutdown\n' +
                'fi\n')
        shell('ssh jenkins@springboot-test docker pull springboot-test:5000/pathcare/pathproviderv3:latest')
        shell('#!/bin/sh\n' +
                'ssh jenkins@springboot-test docker run --network=pathprovider_network --env environement=dev --volume=/var/log/pathproviderv3:/log -p 8080:8080 springboot-test:5000/pathcare/pathproviderv3:latest\n' +
                'sleep 60\n' +
                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" http://springboot-test:8080/actuator/health`\n' +
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

job ('pp3Backend-functionalTest') {
    blockOn('/.*-deploy/')
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/pathprovider_v3/backend.git')
                name('master')
                branch('master')
                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
            }
        }
    }
    triggers {
        upstream('pp3Backend-deploy', 'SUCCESS')
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


listView('PP3 Test') {
    description('PP3 Test')
    jobs {
        regex('pp3.*-.+')
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
