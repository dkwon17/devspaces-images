import groovy.json.JsonSlurper

def curlCMD = "https://raw.githubusercontent.com/redhat-developer/devspaces/devspaces-3-rhel-9/dependencies/job-config.json".toURL().text

def jsonSlurper = new JsonSlurper();
def config = jsonSlurper.parseText(curlCMD);

def JOB_BRANCHES = config."Management-Jobs"."operator-bundle"?.keySet()
for (JB in JOB_BRANCHES) {
    //check for jenkinsfile
    FILE_CHECK = false
    try {
        fileCheck = readFileFromWorkspace('jobs/DS_CI/template_'+JB+'.jenkinsfile')
        FILE_CHECK = true
    }
    catch(err) {
        println "No jenkins file found for " + JB
    }
    if (FILE_CHECK) {
        JOB_BRANCH=""+JB
        MIDSTM_BRANCH="devspaces-" + JOB_BRANCH.replaceAll(".x","") + "-rhel-9"
        jobPath="${FOLDER_PATH}/${ITEM_NAME}_" + JOB_BRANCH
        pipelineJob(jobPath){
            disabled(config."Management-Jobs"."operator-bundle"[JB].disabled) // on reload of job, disable to avoid churn
            UPSTM_NAME="che-operator"
            MIDSTM_NAME="operator-bundle"
            SOURCE_REPO="eclipse-che/" + UPSTM_NAME
            MIDSTM_REPO="redhat-developer/devspaces-images"

            def CMD_EVEN="git ls-remote --heads https://github.com/" + SOURCE_REPO + ".git " + config."Management-Jobs"."operator-bundle"[JB].upstream_branch[0]
            def CMD_ODD="git ls-remote --heads https://github.com/" + SOURCE_REPO + ".git " + config."Management-Jobs"."operator-bundle"[JB].upstream_branch[1]

            def BRANCH_CHECK_EVEN=CMD_EVEN.execute().text
            def BRANCH_CHECK_ODD=CMD_ODD.execute().text

            SOURCE_BRANCH="main"
            if (BRANCH_CHECK_EVEN) {
                SOURCE_BRANCH=""+config."Management-Jobs"."operator-bundle"[JB].upstream_branch[0]
            } else if (BRANCH_CHECK_ODD) {
                SOURCE_BRANCH=""+config."Management-Jobs"."operator-bundle"[JB].upstream_branch[1]
            }

            description('''
Artifact builder + sync job; triggers brew after syncing

<p>Operator / installer jobs:<br/>
1. <a href=../operator_''' + JOB_BRANCH + '''>operator_''' + JOB_BRANCH + '''</a>: go code<br/>
2. <a href=../operator-bundle_''' + JOB_BRANCH + '''>operator-bundle_''' + JOB_BRANCH + '''</a>: CRD, CSV<br/>
3. <a href=../dsc_''' + JOB_BRANCH + '''>dsc_''' + JOB_BRANCH + '''</a>: installer CLI
</p>

<ul>
<li>Upstream: <a href=https://github.com/''' + SOURCE_REPO + '''>''' + UPSTM_NAME + '''</a></li>
<li>Midstream: <a href=https://github.com/''' + MIDSTM_REPO + '''/tree/''' + MIDSTM_BRANCH + '''/devspaces-''' + MIDSTM_NAME + '''/>devspaces-''' + MIDSTM_NAME + '''</a></li>
<li>Downstream: <a href=https://pkgs.devel.redhat.com/cgit/containers/devspaces-''' + MIDSTM_NAME + '''?h=''' + MIDSTM_BRANCH + '''>''' + MIDSTM_NAME + '''</a></li>
</ul>

<p>If <b style="color:green">downstream job fires</b>, see 
<a href=../sync-to-downstream_''' + JOB_BRANCH + '''/>sync-to-downstream</a>, then
<a href=../get-sources-rhpkg-container-build_''' + JOB_BRANCH + '''/>get-sources-rhpkg-container-build</a>. See also <a href=../dsc_''' + JOB_BRANCH + '''>dsc</a>. <br/>
   If <b style="color:orange">job is yellow</b>, no changes found to push, so no container-build triggered. </p>

<p> If this job is ever disabled and you want to update the LATEST_IMAGES files yourself, run 
<a href=https://github.com/redhat-developer/devspaces/blob/''' + MIDSTM_BRANCH + '''/dependencies/LATEST_IMAGES.sh>LATEST_IMAGES.sh --commit</a>

<p>Results:<ul><li><a href=https://quay.io/devspaces/devspaces-'''+MIDSTM_NAME+'''>quay.io/devspaces/devspaces-'''+MIDSTM_NAME+'''</a></li></ul></p>
            ''')

            properties {
                githubProjectUrl("https://github.com/" + SOURCE_REPO)

                disableResumeJobProperty()
                disableConcurrentBuildsJobProperty()
            }

            quietPeriod(3600) // limit builds to 1 every 1h (in sec)

            logRotator {
                daysToKeep(5)
                numToKeep(5)
                artifactDaysToKeep(2)
                artifactNumToKeep(1)
            }

            parameters{
                stringParam("SOURCE_REPO", SOURCE_REPO)
                stringParam("SOURCE_BRANCH", SOURCE_BRANCH)
                stringParam("MIDSTM_REPO", MIDSTM_REPO)
                stringParam("MIDSTM_BRANCH", MIDSTM_BRANCH)
                stringParam("MIDSTM_NAME", MIDSTM_NAME)
                stringParam("CSV_VERSION", config.CSVs."operator-bundle"[JB].CSV_VERSION)
                stringParam("CSV_VERSION_PREV", config.CSVs."operator-bundle"[JB].CSV_VERSION_PREV)
                booleanParam("FORCE_BUILD", false, "If true, trigger a rebuild even if no changes were pushed to pkgs.devel")
                booleanParam("CLEAN_ON_FAILURE", true, "If false, don't clean up workspace after the build so it can be used for debugging.")
            }

            definition {
                cps{
                    sandbox(true)
                    script(readFileFromWorkspace('jobs/DS_CI/template_'+JOB_BRANCH+'.jenkinsfile'))
                }
            }
        }
    }
}