import groovy.json.JsonSlurper

def curlCMD = "https://raw.githubusercontent.com/redhat-developer/devspaces/devspaces-3-rhel-9/dependencies/job-config.json".toURL().text

def jsonSlurper = new JsonSlurper();
def config = jsonSlurper.parseText(curlCMD);

// for 2.yy, return 2.yy-1
def computePreviousVersion(String ver){
    verBits=ver.tokenize(".")
    int vb1=Integer.parseInt(verBits[1])
    if (vb1==0) {
        return verBits[0] + ".0"
    } else {
        return verBits[0] + "." + (vb1-1)
    }
}

def JOB_BRANCHES = [computePreviousVersion(config.Version+"")] // only one release at a time, previous stable one (2.yy-1)
for (String JOB_BRANCH : JOB_BRANCHES) {
    pipelineJob("${FOLDER_PATH}/${ITEM_NAME}"){
        // keep job disabled until we explicitly need it
        disabled(true)

        MIDSTM_BRANCH="devspaces-" + JOB_BRANCH.replaceAll(".x","") + "-rhel-9"

        description('''
Collect sources from pkgs.devel and vsix files and rsync to spmm-util so they can be published as part of a GA release. 
<p><blockquote>
    If the <b>stage-mw-release</b> command fails or was omitted, you can re-run it locally without having to re-run this whole job:
    <p><pre>
        kinit kinit -k -t /path/to/devspaces-build-keytab devspaces-build@IPA.REDHAT.COM
        REMOTE_USER_AND_HOST="devspaces-build@spmm-util.engineering.redhat.com"
        ssh "${REMOTE_USER_AND_HOST}" "stage-mw-release devspaces-3.yy.z.yyyy-mm-dd"
        Staged devspaces-3.yy.z.2023-03-21 in 0:04:30.158899
    </pre></p>
</blockquote></p>
''')

        logRotator {
            daysToKeep(5)
            numToKeep(5)
            artifactDaysToKeep(5)
            artifactNumToKeep(2)
        }

        parameters{
            stringParam("MIDSTM_BRANCH",MIDSTM_BRANCH,"redhat-developer/devspaces branch to use")
            stringParam("CSV_VERSION", config.CSVs."operator-bundle"[JOB_BRANCH].CSV_VERSION)
            booleanParam("PUBLISH", true, "if true, rsync sources to spmm-util; run <tt>stage-mw-release devspaces-3.yy.z.yyyy-mm-dd</tt>")
        }

        definition {
            cps{
                sandbox(true)
                script(readFileFromWorkspace('jobs/DS_CI/Releng/' + ITEM_NAME + '.jenkinsfile'))
            }
        }
    }
}
