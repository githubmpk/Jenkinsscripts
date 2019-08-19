//job('crystalexporter-build') {
//    scm {
//        git {
//            remote {
//                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/crystalExporter.git')
//                name('master')
//                branch('develop')
//                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
//            }
//        }
//    }
//    steps {
//        gradle {
//            gradleName('Gradle Local')
//            tasks('clean')
//            tasks('build')
//            useWrapper(false)
//        }
//    }
//    publishers {
//        artifactArchiver {
//            artifacts('**/*.jar')
//            onlyIfSuccessful(true)
//        }
//        extendedEmail {
//            recipientList('$DEFAULT_RECIPIENTS')
//            defaultSubject('$DEFAULT_SUBJECT')
//            defaultContent('$DEFAULT_CONTENT')
//            contentType('text/html')
//            triggers {
//                always() {
//                    sendTo() {
//                        recipientList()
//                    }
//                }
//            }
//        }
//    }
//}
//
//job ('crystalexporter-deploy') {
//    blockOn('/.*-deploy/')
//    scm {
//        git {
//            remote {
//                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/crystalExporter.git')
//                name('master')
//                branch('develop')
//                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
//            }
//        }
//    }
//    triggers {
//        upstream('crystalExporter-build', 'SUCCESS')
//    }
//    steps {
//
//        shell('#!/bin/sh\n' +
//                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" -X POST http://172.17.7.129:8080/crystal/actuator/shutdown`\n' +
//                'sleep 40\n' +
//                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
//                'echo server not shutdown\n' +
//                'fi\n')
//        shell('rm -f Jenkins@172.17.7.129:C:/Users/Jenkins/CrystalExporter/crystalexporter-0.0.1-SNAPSHOT.jar')
//        copyArtifact {
//            projectName('crystalexporter-build')
//            filter('build/libs/*.jar')
//
//        }
//        shell('scp build/libs/crystalexporter-0.0.1-SNAPSHOT.jar Jenkins@172.17.7.129:C:/Users/Jenkins/CrystalExporter/crystalexporter-0.0.1-SNAPSHOT.jar')
//        shell('BUILD_ID=dontKillMe\nssh Jenkins@172.17.7.129 java -jar C:/Users/Jenkins/CrystalExporter/crystalexporter-0.0.1-SNAPSHOT.jar  &')
//        shell('#!/bin/sh\n' +
//                'sleep 60\n' +
//                'STATUSCODE=`curl --silent --fail --write-out "%{http_code}" http://172.17.7.129:8080/crystal/actuator/health`\n' +
//                'if [  ${STATUSCODE} -ne 200 ]; then\n' +
//                'echo server did not start up correctly\n' +
//                'exit -1\n' +
//                'fi\n')
//    }
//    publishers {
//        artifactArchiver {
//            artifacts('**/*.jar')
//            onlyIfSuccessful(true)
//        }
//        extendedEmail {
//            recipientList('$DEFAULT_RECIPIENTS')
//            defaultSubject('$DEFAULT_SUBJECT')
//            defaultContent('$DEFAULT_CONTENT')
//            contentType('text/html')
//            triggers {
//                always() {
//                    sendTo() {
//                        recipientList()
//                    }
//                }
//            }
//        }
//    }
//}
//
//
//
//
//listView('Crystal Exporter') {
//    description('Crystal Exporter')
//    jobs {
//        regex('crystalexporter-.+')
//    }
//    columns {
//        status()
//        weather()
//        name()
//        lastSuccess()
//        lastFailure()
//        lastDuration()
//        buildButton()
//    }
//}
