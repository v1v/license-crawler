# List licenses of a particular GitHub Organisations


Given a particular user, token and org this script will iterate through each
repository and search for the top maven POM file in order to search for the <licenses>
section, if it doesn't exist then it will generate the effective pom and search again
for the <licenses> section.

    CSV console output format:
    <REPO>|<groupId>:<artifactId>|(licensed|<license-name>?|<license-url>?|Warning|No pom file)

Where:

    * REPO is the relative path including the organisation name
    * groupId is one of the maven coordinates
    * artifactId is another maven coordinates
    * licensed is the hardcoded word to say it has been found
    * license-name represents the <licenses><license><name> tag
    * license-url represents the <licenses><license><url> tag
    * Warning is just something happened when queering this particular repo
    * No pom file is just no top pom file found in this repo.

    for instance:

        Repository:jenkinsci:gmaven|org.codehaus.gmaven:gmaven|licensed|ASLv2|http://www.apache.org/licenses/LICENSE-2.0.txt
        Repository:jenkinsci:core-js|org.jvnet.hudson:htmlunit-core-js|licensed|Mozilla Public License version 1.1|http://www.mozilla.org/MPL/MPL-1.1.html
        Repository:jenkinsci:jelly|org.jenkins-ci:commons-jelly
        Repository:jenkinsci:jexl|commons-jexl:commons-jexl|licensed|The Apache Software License, Version 2.0|/LICENSE.txt
        Repository:jenkinsci:json-lib|org.kohsuke.stapler:json-lib|licensed|The Apache Software License, Version 2.0|http://www.apache.org/licenses/LICENSE-2.0.txt
        Repository:jenkinsci:maven-hudson-dev-plugin|org.jenkins-ci.tools:maven-jenkins-dev-plugin|licensed|licensed|Apache Software License - Version 2.0|http://www.apache.org/licenses/LICENSE-2.0|Eclipse Public License - Version 1.0|http://www.eclipse.org/org/documents/epl-v10.php
        Repository:jenkinsci:netx|org.jvnet.hudson:netx



## Build docker image

    docker build -t license .

## Run docker image

    docker run --rm -ti -e GITHUB_TOKEN=XYZ -e GITHUB_USER=your -e GITHUB_ORG=jenkinsci license | tee licenses.txt

    # List all the repos without any license
    grep -v "|licensed"licenses.txt

    # List all the repos with a license section
    grep -v "|licensed"licenses.txt
