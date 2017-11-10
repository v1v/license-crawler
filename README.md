# List licenses of a particular GitHub Organisations


Given a particular user, token and org this script will iterate through each
repository and search for the top maven POM file in order to search for the <licenses>
section, if it doesn't exist then it will generate the effective pom and search again
for the <licenses> section.

    CSV console output format:

    status;fullname;pom;groupId;artifactId;pomLicense;ghLicense;pomLicenseName;pomLicenseUrl;ghLicenseName;message


Where:

    * Status whether any errors/warnings or already found it.
    * fullname the repo full name (org/reponame)
    * pom the name of the pom file
    * groupId is another maven coordinates
    * artifactId is another maven coordinates
    * pomLicense whether the pom contains any <licenses><license><name> tag
    * ghLicense whether the GH api will query the License section.
    * pomLicenseName represents <licenses><license><name> tag
    * pomLicenseUrl represents <licenses><license><name> tag
    * ghLicenseName represents the name of the license in the rest api.
    * message contains further details in case of errors/warnings

    for instance:

        INFO;jenkinsci/gmaven;pom.xml;org.codehaus.gmaven;gmaven;true;true;ASLv2;http://www.apache.org/licenses/LICENSE-2.0.txt;Apache License 2.0;pom-license|github-license
        INFO;jenkinsci/core-js;pom.xml;org.jvnet.hudson;htmlunit-core-js;true;true;Mozilla Public License version 1.1;http://www.mozilla.org/MPL/MPL-1.1.html;Other;pom-license|github-license
        INFO;jenkinsci/jelly;pom.xml;org.jenkins-ci;commons-jelly;false;true;null;null;Apache License 2.0;null|github-license
        INFO;jenkinsci/jexl;pom.xml;commons-jexl;commons-jexl;true;true;The Apache Software License, Version 2.0;/LICENSE.txt;Apache License 2.0;pom-license|github-license


    JSON console output format:

        {
            "message": "github-license",
            "status": "INFO",
            "pomLicenseUrl": null,
            "pomLicense": false,
            "ghLicense": true,
            "fullname": "jenkinsci/extended-choice-parameter-plugin",
            "groupId": "",
            "pomLicenseName": null,
            "ghLicenseName": "MIT License",
            "artifactId": "extended-choice-parameter",
            "pom": "pom.xml"
        }

## Usage

    usage: groovy licenses.groovy [options]
                  Reports all repositories and their licenses
     -a,--all                  Query also Licenses field from GitHub (or use
                               GITHUB_ALL env variable)
     -f,--format <arg>         Format to be used (csv,json)
     -h,--help                 Print this usage info
     -o,--organisation <arg>   GH organisation to be query (or use GITHUB_ORG
                               env variable)
     -t,--token <arg>          personal access token of a GitHub (or use
                               GITHUB_TOKEN env variable)
     -u,--user <arg>           GH username (or use GITHUB_USER env variable)

## Build docker image

    docker build -t license .

## Run docker image

    ## Be patient since groovy docker container is a bit slow :)

    # Default flags and redirecting output to a file
    docker run --rm -ti -e GITHUB_TOKEN=XYZ -e GITHUB_USER=your -e GITHUB_ORG=jenkinsci license | tee licenses.txt

    # Query POM and also GH license RestAPI
    docker run --rm -ti -e GITHUB_TOKEN=XYZ -e GITHUB_USER=your -e GITHUB_ORG=jenkinsci license groovy licenses.groovy --all

    # Run help
    docker run --rm -v "$PWD":/home/groovy/scripts -w /home/groovy/scripts license groovy licenses.groovy -h

