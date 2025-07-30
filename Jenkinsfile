pipeline {
    agent any

    parameters {
        choice(
            name: 'BUILD_TYPE',
            choices: ['incremental', 'full'],
            description: 'Select build type: incremental (only changed tests), or full (all tests and static analysis)'
        )
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Incremental Test Selection') {
            when {
                expression { params.BUILD_TYPE == 'incremental' }
            }
            steps {
                script {
                    // Find changed Java files in the last commit (defensive for first commit)
                    def changed = bat(
                        script: '''
                            @echo off
                            setlocal enabledelayedexpansion
                            git rev-parse HEAD~1 >nul 2>&1
                            if %ERRORLEVEL% EQU 0 (
                                git diff --name-only HEAD~1 HEAD > files.txt
                            ) else (
                                git ls-files > files.txt
                            )
                            for /f "usebackq delims=" %%F in ("files.txt") do (
                                echo %%F
                            )
                            del files.txt
                        ''',
                        returnStdout: true
                    ).trim().split('\r?\n').findAll {
                        it =~ /^src[\\/]+main[\\/]+java[\\/]+com[\\/]+thealgorithms[\\/]+.*\.java$/
                    }

                    // Map to corresponding test classes
                    def testClasses = []
                    for (f in changed) {
                        def base = f.replaceAll('^src[\\\\/]+main[\\\\/]+java[\\\\/]+', '').replace('.java', '')
                        def testPath = "src/test/java/${base}Test.java"
                        if (fileExists(testPath)) {
                            testClasses << base.replaceAll(/[\\\\/]/, '.') + 'Test'
                        }
                    }
                    if (testClasses) {
                        echo "Running tests: ${testClasses.join(',')}"
                        bat "mvn clean -B -Dtest=${testClasses.join(',')} test"
                    } else {
                        echo "No changed classes with matching tests found, skipping selective tests."
                    }
                }
            }
        }

        stage('Full Build and Site') {
            when {
                expression { params.BUILD_TYPE == 'full' }
            }
            steps {
                bat 'mvn clean verify site'
            }
        }
    }

    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            // Only try to publish coverage if file exists
            script {
                if (fileExists('target/jacoco.exec')) {
                    jacoco execPattern: '**/target/jacoco.exec', classPattern: '**/target/classes', sourcePattern: '**/src/main/java'
                }
                // Only run static analysis/parsing and publish reports for full builds
                if (params.BUILD_TYPE == 'full') {
                    if (fileExists('target/checkstyle-result.xml')) {
                        recordIssues enabledForFailure: true, tool: checkStyle(pattern: '**/target/checkstyle-result.xml')
                    }
                    if (fileExists('target/pmd.xml')) {
                        recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')
                    }
                    if (fileExists('target/spotbugsXml.xml')) {
                        recordIssues enabledForFailure: true, tool: spotBugs(pattern: '**/target/spotbugsXml.xml')
                    }
                    if (fileExists('target/site/jacoco/index.html')) {
                        publishHTML(target: [
                            reportName: 'JaCoCo Coverage Report',
                            reportDir: 'target/site/jacoco',
                            reportFiles: 'index.html'
                        ])
                    }
                    if (fileExists('target/reports/checkstyle.html')) {
                        publishHTML(target: [
                            reportName: 'CheckStyle Report',
                            reportDir: 'target/reports',
                            reportFiles: 'checkstyle.html'
                        ])
                    }
                    if (fileExists('target/reports/pmd.html')) {
                        publishHTML(target: [
                            reportName: 'PMD Report',
                            reportDir: 'target/reports',
                            reportFiles: 'pmd.html'
                        ])
                    }
                    if (fileExists('target/site/spotbugs.html')) {
                        publishHTML(target: [
                            reportName: 'SpotBugs Report',
                            reportDir: 'target/site',
                            reportFiles: 'spotbugs.html'
                        ])
                    }
                    if (fileExists('target/reports/surefire.html')) {
                        publishHTML(target: [
                            reportName: 'Test Report',
                            reportDir: 'target/reports',
                            reportFiles: 'surefire.html'
                        ])
                    }
                }
            }
        }
    }
}