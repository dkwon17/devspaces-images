import groovy.json.JsonSlurper

def curlCMD = "https://raw.githubusercontent.com/redhat-developer/devspaces/devspaces-3-rhel-9/dependencies/job-config.json".toURL().text

///// ///// ///// ///// ///// ///// ///// ///// ///// ///// ///// ///// ///// ///// 
///// @since 3.1
///// ///// ///// ///// ///// ///// ///// ///// ///// ///// ///// ///// ///// ///// 

def jsonSlurper = new JsonSlurper();
def config = jsonSlurper.parseText(curlCMD);

def JOB_BRANCHES = config.Jobs.code?.keySet()
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
            disabled(config.Jobs.code[JB].disabled) // on reload of job, disable to avoid churn
            UPSTM_NAME="che-code"
            MIDSTM_NAME="code"
            SOURCE_REPO="che-incubator/" + UPSTM_NAME
            MIDSTM_REPO="redhat-developer/devspaces-images"

            def CMD_EVEN="git ls-remote --heads https://github.com/" + SOURCE_REPO + ".git " + config.Jobs.configbump[JB].upstream_branch[0]
            def CMD_ODD="git ls-remote --heads https://github.com/" + SOURCE_REPO + ".git " + config.Jobs.configbump[JB].upstream_branch[1]

            def BRANCH_CHECK_EVEN=CMD_EVEN.execute().text
            def BRANCH_CHECK_ODD=CMD_ODD.execute().text

            SOURCE_BRANCH="main"
            if (BRANCH_CHECK_EVEN) {
                SOURCE_BRANCH=""+config.Jobs.configbump[JB].upstream_branch[0]
            } else if (BRANCH_CHECK_ODD) {
                SOURCE_BRANCH=""+config.Jobs.configbump[JB].upstream_branch[1]
            }

            description('''
Artifact builder + sync job; triggers brew after syncing

<ul>
<li>Upstream: <a href=https://github.com/''' + SOURCE_REPO + '''>''' + UPSTM_NAME + '''</a></li>
<li>Midstream: <a href=https://github.com/''' + MIDSTM_REPO + '''/tree/''' + MIDSTM_BRANCH + '''/devspaces-''' + MIDSTM_NAME + '''/>devspaces-''' + MIDSTM_NAME + '''</a></li>
<li>Downstream: <a href=https://pkgs.devel.redhat.com/cgit/containers/devspaces-''' + MIDSTM_NAME + '''?h=''' + MIDSTM_BRANCH + '''>''' + MIDSTM_NAME + '''</a></li>
</ul>

<p>If <b style="color:green">downstream job fires</b>, see 
<a href=../sync-to-downstream_''' + JOB_BRANCH + '''/>sync-to-downstream</a>, then
<a href=../get-sources-rhpkg-container-build_''' + JOB_BRANCH + '''/>get-sources-rhpkg-container-build</a>. <br/>
   If <b style="color:orange">job is yellow</b>, no changes found to push, so no container-build triggered. </p>
<p>Results:<ul><li><a href=https://quay.io/devspaces/'''+MIDSTM_NAME+'''-rhel8>quay.io/devspaces/'''+MIDSTM_NAME+'''-rhel8</a></li></ul></p>
            ''')

            properties {
                githubProjectUrl("https://github.com/" + SOURCE_REPO)
                
                JobSharedUtils.enableDefaultPipelineWebhookTrigger(delegate, SOURCE_BRANCH, SOURCE_REPO) 
                
                disableResumeJobProperty()
            }

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
                booleanParam("FORCE_BUILD", false, "If true, trigger a rebuild even if no changes were pushed to pkgs.devel")
                stringParam("TIMEOUT", "600", "Override default timeout (in minutes) for this and some descendant jobs.")
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
