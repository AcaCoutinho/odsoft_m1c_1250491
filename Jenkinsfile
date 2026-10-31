// CI/CD pipeline for the Library Management service (Multibranch Pipeline job).
// Every branch: build, static analysis, unit/integration/system tests, coverage, mutation testing, archive.
// dev  -> Docker image + smoke tests + deploy to DEV.
// main -> Docker image + smoke tests + deploy to DEV, then to PROD after manual approval.
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
        // Timeouts are per stage: a pipeline-wide one would also count the PROD approval wait.
        stage('Build') {
            options { timeout(time: 10, unit: 'MINUTES') }
            steps {
                sh '$MVN clean compile'
            }
        }

        // PMD/SpotBugs only read src/main and target/classes, and write their own files, so they run alongside the tests.
        // JaCoCo is not a separate step: its agent runs inside the test JVMs, so it stays in the Tests branch.
        stage('Analysis & Tests') {
            options { timeout(time: 30, unit: 'MINUTES') }
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
            options { timeout(time: 30, unit: 'MINUTES') }
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
                // Provenance: which branch, build and commit produced this jar and its image (the image only exists for dev/main)
                writeFile file: 'release.properties',
                    text: "BRANCH=${env.BRANCH_NAME}\nBUILD=${env.BUILD_NUMBER}\nGIT_COMMIT=${env.GIT_COMMIT}\nIMAGE_TAG=${env.BRANCH_NAME}-${env.BUILD_NUMBER}\n"
                archiveArtifacts artifacts: 'psoft-project-2024-g1/target/psoft-g1-*.jar,release.properties', fingerprint: true
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
                IMAGE_TAG = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
            }
            stages {
                stage('Image') {
                    options { timeout(time: 10, unit: 'MINUTES') }
                    // The commit goes into the image so smoke tests and deploys can check which build is running.
                    steps { sh 'docker build --build-arg GIT_COMMIT=$GIT_COMMIT -t psoft-g1:$IMAGE_TAG psoft-project-2024-g1' }
                }
                stage('Smoke Tests') {
                    // The new image runs in a throwaway container (random host port) once per profile, before any environment gets it:
                    // it must start, report healthy, run with that profile and this build's commit, and serve Swagger UI and the OpenAPI document.
                    steps {
                        sh '''
                            for PROFILE in dev prod; do
                                CID=$(docker run -d -P -e SPRING_PROFILES_ACTIVE=$PROFILE psoft-g1:$IMAGE_TAG)
                                URL="http://localhost:$(docker port "$CID" 8080 | head -1 | sed 's/.*://')"
                                if ! { curl -fsS --retry 30 --retry-delay 2 --retry-all-errors -o /dev/null "$URL/actuator/health" &&
                                       curl -fsS "$URL/api/public/environment" | grep -qw "$PROFILE" &&
                                       curl -fsS "$URL/api/public/environment" | grep -q "\\"commit\\":\\"$GIT_COMMIT\\"" &&
                                       curl -fsSL -o /dev/null "$URL/swagger-ui" &&
                                       curl -fsS "$URL/api-docs" | grep -q '"openapi"'; }; then
                                    echo "Smoke tests failed for profile $PROFILE"
                                    docker logs --tail 50 "$CID"
                                    docker rm -f "$CID"
                                    exit 1
                                fi
                                docker rm -f "$CID"
                            done
                        '''
                    }
                }
                stage('DEV') {
                    steps { deploy('dev', 8081) }
                }
                stage('PROD') {
                    when {
                        beforeInput true
                        branch 'main'
                    }
                    options { timeout(time: 1, unit: 'DAYS') }
                    input {
                        message 'Deploy this build to production?'
                        ok 'Deploy'
                        // To restrict who can approve, add: submitter '<Jenkins user or group>' (admins can always approve)
                        submitterParameter 'APPROVER'
                    }
                    steps {
                        script { currentBuild.description = "PROD approved by ${env.APPROVER}" }
                        deploy('prod', 8082)
                    }
                }
            }
        }
    }

    post {
        failure { echo "Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}. See ${env.BUILD_URL}" }
        aborted { echo "Aborted (manually, by a timeout or by an expired PROD approval): ${env.JOB_NAME} #${env.BUILD_NUMBER}" }
    }
}

// Starts image psoft-g1:$IMAGE_TAG in the Compose project app-<env>, checks /actuator/health and that the running app reports this build's commit.
// If the check fails, the image that was running before is started again and the build fails.
// The lock serialises deploys to one environment across branches (disableConcurrentBuilds only covers one branch).
def deploy(String appEnv, int port) {
    lock(resource: "psoft-g1-${appEnv}") {
        withEnv(["PROJECT=app-${appEnv}", "SPRING_PROFILE=${appEnv}", "APP_PORT=${port}"]) {
            sh '''
                COMPOSE="docker compose -p $PROJECT -f psoft-project-2024-g1/docker-compose.yml"
                # Empty on the first deploy: there is nothing to roll back to
                PREVIOUS=$($COMPOSE ps -q app | xargs -r docker inspect --format '{{.Config.Image}}')
                $COMPOSE up -d
                if ! { curl -fsS --retry 30 --retry-delay 2 --retry-all-errors -o /dev/null "http://localhost:$APP_PORT/actuator/health" &&
                       curl -fsS "http://localhost:$APP_PORT/api/public/environment" | grep -q "\\"commit\\":\\"$GIT_COMMIT\\""; }; then
                    $COMPOSE logs --tail 50 app
                    if [ -n "$PREVIOUS" ]; then
                        echo "Health or commit check failed: rolling back to $PREVIOUS"
                        IMAGE_TAG="${PREVIOUS##*:}" $COMPOSE up -d app
                    fi
                    exit 1
                fi
            '''
        }
    }
}
