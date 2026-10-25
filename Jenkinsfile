// CI/CD pipeline for the Library Management service (Multibranch Pipeline job).
// Every branch: build, static analysis, unit/integration/system tests, coverage, mutation testing, archive.
// dev  -> deploy to DEV.
// main -> deploy to STAGING, then to PROD after manual approval.

pipeline {
    agent any

    tools {
        maven 'maven-3.9'
        jdk 'jdk-17'
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '5'))
    }

    environment {
        MVN = 'mvn -B -ntp -f psoft-project-2024-g1'
    }

    stages {
        stage('Build') {
            steps {
                sh '$MVN clean compile'
            }
        }

        // PMD/SpotBugs only read src/main and target/classes, and write their own files, so they run alongside the tests.
        // JaCoCo is not a separate step: its agent runs inside the test JVMs, so it stays in the Tests branch.
        stage('Analysis & Tests') {
            parallel {
                stage('Static Analysis') {
                    steps {
                        sh '$MVN pmd:pmd spotbugs:spotbugs'
                        recordIssues(
                            tools: [pmdParser(pattern: '**/target/pmd.xml'), spotBugs(pattern: '**/target/spotbugsXml.xml')],
                            qualityGates: [[threshold: 1, type: 'NEW', criticality: 'UNSTABLE']])
                    }
                }

                stage('Tests') {
                    stages {
                        stage('Unit Tests') {
                            steps {
                                sh '$MVN test'
                            }
                            post {
                                always {
                                    junit '**/target/surefire-reports/TEST-*.xml'
                                }
                            }
                        }

                        stage('Integration & System Tests') {
                            steps {
                                // Unit tests already ran (-Dtest=None skips Surefire only); PMD/SpotBugs run in the parallel branch.
                                // jacoco.exec is appended, so the report covers all test levels.
                                sh '$MVN verify -Dtest=None -Dsurefire.failIfNoSpecifiedTests=false -Dpmd.skip -Dspotbugs.skip'
                                // Thresholds sit just below the 2026-10-24 baseline (line 72%, branch 21%): raise them as tests improve.
                                recordCoverage(
                                    id: 'jacoco', name: 'Code Coverage',
                                    tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']],
                                    qualityGates: [
                                        [metric: 'LINE', threshold: 70.0, criticality: 'UNSTABLE'],
                                        [metric: 'BRANCH', threshold: 20.0, criticality: 'UNSTABLE']])
                            }
                            post {
                                always {
                                    junit '**/target/failsafe-reports/TEST-*.xml'
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Mutation Tests') {
            steps {
                sh '$MVN pitest:mutationCoverage'
                // Baseline 2026-10-24: 13% of mutations killed.
                recordCoverage(
                    id: 'pit', name: 'Mutation Coverage',
                    tools: [[parser: 'PIT', pattern: '**/target/pit-reports/mutations.xml']],
                    qualityGates: [[metric: 'MUTATION', threshold: 12.0, criticality: 'UNSTABLE']])
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'psoft-project-2024-g1/target/psoft-g1-*.jar', fingerprint: true
            }
        }

        stage('Deploy') {
            // A failed quality gate leaves the build UNSTABLE: nothing gets deployed.
            when { expression { currentBuild.currentResult == 'SUCCESS' } }
            stages {
                stage('DEV') {
                    when { branch 'dev' }
                    steps { deploy('dev', 'vm-dev', 8080) }
                }
                stage('STAGING') {
                    when { branch 'main' }
                    steps { deploy('staging', 'vm-dev', 8081) }
                }
                stage('PROD') {
                    when {
                        beforeInput true
                        branch 'main'
                    }
                    // ponytail: input holds the executor while waiting; move to agent none + stash if executors run short
                    options { timeout(time: 1, unit: 'DAYS') }
                    input {
                        message 'Deploy this build to production?'
                        ok 'Deploy'
                    }
                    steps { deploy('prod', 'vm-prod', 8080) }
                }
            }
        }
    }
}

// Copies the jar built by this run to a VM, restarts it there and smoke-tests the API.
// VM requirements: Java 17+, and an SSH user whose private key is the Jenkins credential 'vm-ssh'.
// The extra Spring profile (dev/staging/prod) picks up an application-<env>.properties when one exists.
def deploy(String appEnv, String host, int port) {
    withEnv(["APP_ENV=${appEnv}", "HOST=${host}", "PORT=${port}"]) {
        withCredentials([sshUserPrivateKey(credentialsId: 'vm-ssh', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER')]) {
            sh '''
                OPTS="-i $SSH_KEY -o StrictHostKeyChecking=accept-new"
                DIR="psoft-g1-$APP_ENV"
                ssh $OPTS "$SSH_USER@$HOST" "mkdir -p $DIR"
                scp $OPTS psoft-project-2024-g1/target/psoft-g1-*.jar "$SSH_USER@$HOST:$DIR/app.jar.new"
                # Stop the running instance, swap the jar, start the new one detached (EOF must stay at column 0)
                ssh $OPTS "$SSH_USER@$HOST" sh -s -- "$DIR" "$PORT" "$APP_ENV" <<'EOF'
cd "$1"
pkill -f "$PWD/app.jar" && while pgrep -f "$PWD/app.jar" > /dev/null; do sleep 1; done
mv app.jar.new app.jar
nohup java -jar "$PWD/app.jar" --server.port="$2" --spring.profiles.active="bootstrap,$3" > app.log 2>&1 < /dev/null &
EOF
                curl -fsS --retry 30 --retry-delay 2 --retry-all-errors -o /dev/null "http://$HOST:$PORT/api-docs" \
                    || { ssh $OPTS "$SSH_USER@$HOST" "tail -50 $DIR/app.log"; exit 1; }
            '''
        }
    }
}
