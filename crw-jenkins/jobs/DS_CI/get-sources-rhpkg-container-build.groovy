import groovy.json.JsonSlurper

def curlCMD = "https://raw.githubusercontent.com/redhat-developer/devspaces/devspaces-3-rhel-9/dependencies/job-config.json".toURL().text

def jsonSlurper = new JsonSlurper();
def config = jsonSlurper.parseText(curlCMD);

def JOB_BRANCHES = config."Management-Jobs"."get-sources-rhpkg-container-build"?.keySet()
for (JB in JOB_BRANCHES) {
    //check for jenkinsfile
    FILE_CHECK = false
    try {
        fileCheck = readFileFromWorkspace('jobs/DS_CI/get-sources-rhpkg-container-build_'+JB+'.jenkinsfile')
        FILE_CHECK = true
    }
    catch(err) {
        println "No jenkins file found for " + JB
    }
    if (FILE_CHECK) {
        JOB_BRANCH=""+JB
        MIDSTM_BRANCH="devspaces-" + JOB_BRANCH.replaceAll(".x","") + "-rhel-9"
        FLOATING_QUAY_TAGS="" + config.Other."FLOATING_QUAY_TAGS"[JB]
        jobPath="${FOLDER_PATH}/${ITEM_NAME}_" + JOB_BRANCH
        pipelineJob(jobPath){
            disabled(config."Management-Jobs"."get-sources-rhpkg-container-build"[JB].disabled) // on reload of job, disable to avoid churn
            description('''
<li>Pull latest tarballs from the upstream builds and sync jobs, and build in Brew.

<li>push those to pkgs.devel repo w/ rhpkg, and 
<li>trigger a new <a 
href=https://brewweb.engineering.redhat.com/brew/tasks?state=all&owner=devspaces-build&view=flat&method=buildContainer&order=-id>
OSBS build</a>
<li>Then <a href=../push-latest-container-to-quay_''' + JOB_BRANCH + '''/>push the latest container to quay</a>

To rebuild all the containers, see <a href="../Releng/job/build-all-images_''' + JOB_BRANCH + '''/">build-all-images_''' + JOB_BRANCH + '''</a>
            ''')

            properties {
                disableResumeJobProperty()
            }

            throttleConcurrentBuilds {
                maxPerNode(2)
                maxTotal(10)
            }

            quietPeriod(120) // limit builds to 1 every 2 mins (in sec)

            logRotator {
                daysToKeep(45)
                numToKeep(90)
                artifactDaysToKeep(2)
                artifactNumToKeep(1)
            }

            // NOTE: send email notification to culprits(), developers(), requestor() for failure - use util.notifyBuildFailed() in .jenkinsfile

            parameters{
                stringParam("MIDSTM_BRANCH", MIDSTM_BRANCH, "")
                stringParam("DWNSTM_BRANCH", MIDSTM_BRANCH, "Default to same value as midstream; or, use a pkgs.devel private-username-topic branch for scratch builds")
                stringParam("GIT_PATHs", "containers/devspaces", '''git path to clone from ssh://devspaces-build@pkgs.devel.redhat.com/GIT_PATHs, <br/>
update sources, and run rhpkg container-build: <br/>
* containers/devspaces-server, <br/>
* containers/devspaces-operator, <br/>
* containers/devspaces-udi, etc.<br/>
''')
                stringParam("QUAY_REPO_PATHs", "", '''If blank, do not push to Quay.<br/>
If set, push to quay.io path, <br/>
eg., one or more of these (space-separated values): <br/>
* devspaces-rhel8-operator, devspaces-operator-bundle, udi-rhel8, etc.<br/>
---<br/>
See complete list at <a href=../push-latest-container-to-quay_''' + JOB_BRANCH + '''>push-latest-container-to-quay</a>''')
                stringParam("UPDATE_BASE_IMAGES_FLAGS", "", "Pass additional flags to updateBaseImages, eg., '--tag 1.13'")
                stringParam("nodeVersion", "", "Leave blank if not needed")
                stringParam("yarnVersion", "", "Leave blank if not needed")
                stringParam("FLOATING_QUAY_TAGS", FLOATING_QUAY_TAGS, "Update :" + FLOATING_QUAY_TAGS + " tag in addition to latest (3.y-zz) and base (3.y) tags.")
                booleanParam("SCRATCH", true, "By default make a scratch build. Set to false to NOT do a scratch build.")
                booleanParam("FORCE_BUILD", false, "If true, trigger a rebuild even if no changes were pushed to pkgs.devel")
                booleanParam("CLEAN_ON_FAILURE", true, "If false, don't clean up workspace after the build so it can be used for debugging.")
            }

            definition {
                cps{
                    sandbox(true)
                    script(readFileFromWorkspace('jobs/DS_CI/get-sources-rhpkg-container-build_'+JOB_BRANCH+'.jenkinsfile'))
                }
            }
        }
    }
}