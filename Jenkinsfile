// CI/CD pipeline for the Library Management service (Multibranch Pipeline job).
// Every branch: build, static analysis, unit/integration/system tests, coverage, mutation testing, archive.
// dev  -> Docker image + deploy to DEV.
// main -> Docker image + deploy to DEV, then to PROD after manual approval.
// DEV and PROD are Compose projects (app-dev, app-prod) on this Jenkins machine.

pipeline {
    agent any

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
            when {
                allOf {
                    expression { currentBuild.currentResult == 'SUCCESS' }
                    anyOf { branch 'dev'; branch 'main' }
                }
            }
            environment {
                // BUILD_NUMBER is per branch in a Multibranch job: the branch keeps dev #5 and main #5 apart
                IMAGE_TAG = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
            }
            stages {
                stage('Image') {
                    steps { sh 'docker build -t psoft-g1:$IMAGE_TAG psoft-project-2024-g1' }
                }
                stage('DEV') {
                    steps { deploy('dev', 8081) }
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
                    steps { deploy('prod', 8082) }
                }
            }
        }
    }
}

// Starts image psoft-g1:$IMAGE_TAG in the Compose project app-<env> and checks /actuator/health.
// If the check fails, the image that was running before is started again and the build fails.
def deploy(String appEnv, int port) {
    withEnv(["PROJECT=app-${appEnv}", "SPRING_PROFILE=${appEnv}", "APP_PORT=${port}"]) {
        sh '''
            COMPOSE="docker compose -p $PROJECT -f psoft-project-2024-g1/docker-compose.yml"
            # Empty on the first deploy: there is nothing to roll back to
            PREVIOUS=$($COMPOSE ps -q app | xargs -r docker inspect --format '{{.Config.Image}}')
            $COMPOSE up -d
            if ! curl -fsS --retry 30 --retry-delay 2 --retry-all-errors -o /dev/null "http://localhost:$APP_PORT/actuator/health"; then
                $COMPOSE logs --tail 50 app
                if [ -n "$PREVIOUS" ]; then
                    echo "Health check failed: rolling back to $PREVIOUS"
                    IMAGE_TAG="${PREVIOUS##*:}" $COMPOSE up -d app
                fi
                exit 1
            fi
        '''
    }
}
