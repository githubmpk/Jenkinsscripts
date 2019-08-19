job('pp3Frontend-build') {
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/pathprovider_v3/frontend.git')
                name('master')
                branch('master')
                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
            }
        }
    }
    steps {
        shell('cd src/main/frontend\n' +
                'npm install\n' +
                'npm run build-prod')
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

job ('pp3Frontend-deploy') {
    blockOn('/.*-deploy/')
    scm {
        git {
            remote {
                url('ssh://jenkins@git.pathcare.co.za/srv/gitrepo/pathprovider_v3/frontend.git')
                name('master')
                branch('master')
                credentials('b0a4ea13-5be3-44c9-9279-154244ad2f35')
            }
        }
    }
    triggers {
        upstream('pp3Frontend-build', 'SUCCESS')
    }
    steps {

        shell('ssh jenkins@springboot-test rm -rf /srv/www/htdocs/pathprovider/*')
        shell('cd ..\n' + 'cd pp3Frontend-build\n' + 'scp -r src/main/resources/static jenkins@springboot-test:/srv/www/htdocs/pathprovider')

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
