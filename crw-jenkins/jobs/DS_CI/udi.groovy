import groovy.json.JsonSlurper
def curlCMD = "https://raw.githubusercontent.com/redhat-developer/devspaces/devspaces-3-rhel-9/dependencies/job-config.json".toURL().text

def jsonSlurper = new JsonSlurper();
def config = jsonSlurper.parseText(curlCMD);

def JOB_BRANCHES = config.Jobs.udi?.keySet()
for (JB in JOB_BRANCHES) {
    //check for jenkinsfile
    FILE_CHECK = false
    try {
        // TODO if we sync from Che UDI, change this to run template job instead
        fileCheck = readFileFromWorkspace('jobs/DS_CI/sync-to-downstream_'+JB+'.jenkinsfile')
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
            disabled(config.Jobs.udi[JB].disabled) // on reload of job, disable to avoid churn
            UPSTM_NAME="udi"
            MIDSTM_NAME="udi"
            // TODO when we can sync from Che UDI, change this
            // see https://github.com/eclipse/che/issues/20858 + https://github.com/eclipse/che/issues/20862
            SOURCE_REPO="redhat-developer/devspaces-images"
            MIDSTM_REPO="redhat-developer/devspaces-images"

            SOURCE_BRANCH=""+config.Jobs.udi[JB].upstream_branch[0];
            def CMD="git ls-remote --heads https://github.com/" + SOURCE_REPO + ".git " + SOURCE_BRANCH
            def BRANCH_CHECK=CMD.execute().text
            if (!BRANCH_CHECK) {
                //pluginregistry uses devspaces-3.yy-rhel-9 so if branch doesn't exist use devspaces-3-rhel-9
                SOURCE_BRANCH="devspaces-3-rhel-9"
            }

            description('''
UDI image builder (including assets); triggers brew

<ul>
<!-- TODO include upstream when we can sync from Che UDI <li>Upstream: <a href=https://github.com/''' + SOURCE_REPO + '''>''' + UPSTM_NAME + '''</a></li> -->
<li>Midstream: <a href=https://github.com/''' + MIDSTM_REPO + '''/tree/''' + MIDSTM_BRANCH + '''/devspaces-''' + MIDSTM_NAME + '''/>devspaces-''' + MIDSTM_NAME + '''</a></li>
<li>Downstream: <a href=https://pkgs.devel.redhat.com/cgit/containers/devspaces-''' + MIDSTM_NAME + '''?h=''' + MIDSTM_BRANCH + '''>''' + MIDSTM_NAME + '''</a></li>
</ul>

<p>If <b style="color:green">downstream job fires</b>, see 
<a href=../get-sources-rhpkg-container-build_''' + JOB_BRANCH + '''/>get-sources-rhpkg-container-build</a>. <br/>
   If <b style="color:orange">job is yellow</b>, no changes found to push, so no container-build triggered. </p>

<p>Results:<ul><li><a href=https://quay.io/devspaces/'''+MIDSTM_NAME+'''-rhel8>quay.io/devspaces/'''+MIDSTM_NAME+'''-rhel8</a></li></ul></p>
            ''')

            properties {
                githubProjectUrl("https://github.com/" + SOURCE_REPO)

                // for builds where the upstream repo is devspaces-images, must configure 
                // pipelineWebhookTrigger to prevent triggering on changes to container.yaml 
                // (avoid infinite builds)
                // only watch devspaces-udi folder, but ignore sources and container.yaml files
                JobSharedUtils.enableDefaultPipelineWebhookTrigger(delegate, SOURCE_BRANCH, SOURCE_REPO, 
                    '$ref $files $name', 
                    'refs/heads/' + MIDSTM_BRANCH + ' ' + 
                        '.*"(?!devspaces-' + MIDSTM_NAME + '/(sources|container.yaml))' + "devspaces-" + MIDSTM_NAME + '/[^"]+?".*' + ' ' + 
                        SOURCE_REPO
                )

                disableResumeJobProperty()
            }

            logRotator {
                daysToKeep(5)
                numToKeep(5)
                artifactDaysToKeep(2)
                artifactNumToKeep(1)
            }

            parameters{
                // TODO if we sync from Che UDI, change parameters to match what template job requires
                stringParam("REPOS", "devspaces-" + MIDSTM_NAME, "Do not change this value")
                stringParam("MIDSTM_BRANCH", MIDSTM_BRANCH)
                booleanParam("FORCE_BUILD", false, "If true, trigger a rebuild even if no changes were pushed to pkgs.devel")
                booleanParam("CLEAN_ON_FAILURE", true, "If false, don't clean up workspace after the build so it can be used for debugging.")
            }

            definition {
                cps{
                    sandbox(true)
                    // TODO if we sync from Che UDI, change this to run template job instead
                    script(readFileFromWorkspace('jobs/DS_CI/sync-to-downstream_'+JOB_BRANCH+'.jenkinsfile'))
                }
            }
        }
    }
}