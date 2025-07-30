pipeline {
    agent any

    parameters {
        choice(
            name: 'BUILD_TYPE',
            choices: ['incremental', 'full'],
            description: 'Select build type: incremental (only changed tests), or full (all tests and static analysis)'
        )
    }

    environment {
        CHECKSTYLE_VERSION = '10.12.2'
        PMD_VERSION = '6.55.0'
        SPOTBUGS_VERSION = '4.8.3'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Detect Changed Files') {
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
                    env.CHANGED_FILES = changed.join(',')
                }
            }
        }

        stage('Incremental Test Selection') {
            when {
                expression { params.BUILD_TYPE == 'incremental' }
            }
            steps {
                script {
                    def changed = env.CHANGED_FILES?.split(',')?.findAll { it }
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

        stage('Incremental Static Analysis') {
            when {
                expression { params.BUILD_TYPE == 'incremental' }
            }
            steps {
                script {
                    def changed = env.CHANGED_FILES?.split(',')?.findAll { it }
                    if (changed && changed.size() > 0) {
                        // Download Checkstyle jar
                        bat """
                            if not exist checkstyle.jar curl -L -o checkstyle.jar https://github.com/checkstyle/checkstyle/releases/download/checkstyle-${env.CHECKSTYLE_VERSION}/checkstyle-${env.CHECKSTYLE_VERSION}-all.jar
                        """
                        // Download PMD zip and extract pmd.bat
                        bat """
                            if not exist pmd-bin.zip curl -L -o pmd-bin.zip https://github.com/pmd/pmd/releases/download/pmd_releases%2F${env.PMD_VERSION}/pmd-bin-${env.PMD_VERSION}.zip
                            if not exist pmd-bin-${env.PMD_VERSION} mkdir pmd-bin-${env.PMD_VERSION}
                            if not exist pmd-bin-${env.PMD_VERSION}\\bin\\pmd.bat tar -xf pmd-bin.zip
                        """
                        // Download SpotBugs jar
                        bat """
                            if not exist spotbugs.zip curl -L -o spotbugs.zip https://github.com/spotbugs/spotbugs/releases/download/${env.SPOTBUGS_VERSION}/spotbugs-${env.SPOTBUGS_VERSION}.zip
                            if not exist spotbugs-${env.SPOTBUGS_VERSION} mkdir spotbugs-${env.SPOTBUGS_VERSION}
                            if not exist spotbugs-${env.SPOTBUGS_VERSION}\\lib\\spotbugs.jar tar -xf spotbugs.zip
                        """
                        // Run Checkstyle on changed files
                        def filesArg = changed.collect { "\"${it}\"" }.join(' ')
                        bat "java -jar checkstyle.jar -c checkstyle.xml -f xml -o checkstyle-incremental.xml ${filesArg}"
                        // Run PMD on changed files
                        bat "pmd-bin-${env.PMD_VERSION}\\bin\\pmd.bat -d ${changed.join(',')} -R pmd.xml -f xml -r pmd-incremental.xml"
                        // Compile changed files for SpotBugs
                        bat "mvn compile"
                        // Map source files to class files for SpotBugs
                        def classFiles = changed.collect { it.replace('src/main/java/', 'target/classes/').replace('.java', '.class') }
                        def classFilesArg = classFiles.collect { "\"${it}\"" }.join(' ')
                        bat "java -jar spotbugs-${env.SPOTBUGS_VERSION}\\lib\\spotbugs.jar -textui -xml -output spotbugs-incremental.xml ${classFilesArg}"
                        archiveArtifacts artifacts: '*.xml', onlyIfSuccessful: true
                    } else {
                        echo "No changed files to analyze."
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
            script {
                if (fileExists('target/jacoco.exec')) {
                    jacoco execPattern: '**/target/jacoco.exec', classPattern: '**/target/classes', sourcePattern: '**/src/main/java'
                }
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
                } else if (params.BUILD_TYPE == 'incremental') {
                    if (fileExists('checkstyle-incremental.xml')) {
                        recordIssues enabledForFailure: false, tool: checkStyle(pattern: 'checkstyle-incremental.xml')
                    }
                    if (fileExists('pmd-incremental.xml')) {
                        recordIssues enabledForFailure: false, tool: pmdParser(pattern: 'pmd-incremental.xml')
                    }
                    if (fileExists('spotbugs-incremental.xml')) {
                        recordIssues enabledForFailure: false, tool: spotBugs(pattern: 'spotbugs-incremental.xml')
                    }
                }
            }
        }
    }
}