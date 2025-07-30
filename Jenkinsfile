pipeline {
    agent any

    triggers {
        // Poll SCM every 1 minute for new commits
        pollSCM('* * * * *')
        // Also trigger a full build every 15 minutes
        cron('H/15 * * * *')
    }

    environment {
        // Add any environment variables if needed
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Incremental Test Selection') {
            when {
                // Only run incremental tests if this is an SCM-triggered build (not timer or manual)
                triggeredBy 'SCMTrigger'
            }
            steps {
                script {
                    // Find changed Java files in the last commit (defensive for first commit)
                    def changed = sh(
                        script: '''
                            if git rev-parse HEAD~1 >/dev/null 2>&1; then
                                git diff --name-only HEAD~1 HEAD | grep '^src/main/java/com/thealgorithms/.*\\.java$' || true
                            else
                                git ls-files | grep '^src/main/java/com/thealgorithms/.*\\.java$' || true
                            fi
                        ''',
                        returnStdout: true
                    ).trim().split('\n').findAll { it }

                    // Map to corresponding test classes
                    def testClasses = []
                    for (f in changed) {
                        def base = f.replace('src/main/java/', '').replace('.java', '')
                        def testPath = "src/test/java/${base}Test.java"
                        if (fileExists(testPath)) {
                            testClasses << base.replace('/', '.') + 'Test'
                        }
                    }
                    if (testClasses) {
                        echo "Running tests: ${testClasses.join(',')}"
                        sh "mvn -B -Dtest=${testClasses.join(',')} test"
                    } else {
                        echo "No changed classes with matching tests found, skipping selective tests."
                    }
                }
            }
        }

        stage('Full Build and Site') {
            when {
                anyOf {
                    // Run full build if triggered by timer (cron) or manually
                    triggeredBy 'TimerTrigger'
                    triggeredBy 'UserIdCause'
                }
            }
            steps {
                sh 'mvn clean verify site'
            }
        }
    }

    post {
        always {
            // These steps publish reports if you have the plugins installed
            junit '**/target/surefire-reports/*.xml'
            jacoco execPattern: '**/target/jacoco.exec', classPattern: '**/target/classes', sourcePattern: '**/src/main/java'
            recordIssues enabledForFailure: true, tool: checkStyle(pattern: '**/target/checkstyle-result.xml')
            recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')
            recordIssues enabledForFailure: true, tool: spotBugs(pattern: '**/target/spotbugsXml.xml')
            publishHTML(target: [
                reportName: 'JaCoCo Coverage Report',
                reportDir: 'target/site/jacoco',
                reportFiles: 'index.html'
            ])
            publishHTML(target: [
                reportName: 'CheckStyle Report',
                reportDir: 'target/reports',
                reportFiles: 'checkstyle.html'
            ])
            publishHTML(target: [
                reportName: 'PMD Report',
                reportDir: 'target/reports',
                reportFiles: 'pmd.html'
            ])
            publishHTML(target: [
                reportName: 'SpotBugs Report',
                reportDir: 'target/site',
                reportFiles: 'spotbugs.html'
            ])
            publishHTML(target: [
                reportName: 'Test Report',
                reportDir: 'target/reports',
                reportFiles: 'surefire.html'
            ])
        }
    }
}