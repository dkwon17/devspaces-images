def JOB_BRANCHES = ["3.x"] // only one job
for (String JOB_BRANCH : JOB_BRANCHES) {
    MIDSTM_BRANCH="devspaces-3-rhel-9"
    jobPath="${FOLDER_PATH}/${ITEM_NAME}"
    pipelineJob(jobPath){
        disabled(false)
        description('''
Sync DS_CI jobs in gitlab repo to github.
        ''')

        properties {
            pipelineTriggers {
                triggers{
                    cron {
                        spec ('H H/12 * * *') // every 12 hrs
                    }
                }
            }
        }

        logRotator {
            daysToKeep(5)
            numToKeep(5)
            artifactDaysToKeep(5)
            artifactNumToKeep(2)
        }


        throttleConcurrentBuilds {
            maxPerNode(1)
            maxTotal(1)
        }

        parameters{
            // gitlab repo uses master: do not change. Only sync to main branch crw-2, not the 2.y sub-branches
            stringParam("GITLAB_BRANCH", "master", "branch of https://gitlab.cee.redhat.com/codeready-workspaces/crw-jenkins from which to sync")
            stringParam("MIDSTM_BRANCH", MIDSTM_BRANCH,"branch of https://github.com/redhat-developer/devspaces-images to which to sync")
            booleanParam("CLEAN_ON_FAILURE", true, "If false, don't clean up workspace after the build so it can be used for debugging.")
        }

        definition {
            cps{
                sandbox(true)
                script(readFileFromWorkspace('jobs/DS_CI/Releng/sync-jenkins-gitlab-to-github.jenkinsfile'))
            }
        }
    }
}
