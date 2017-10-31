#!/usr/bin/env groovy

@Grab(group='org.kohsuke', module='github-api', version='1.75')
@Grab(group='commons-io', module='commons-io', version='2.5')

import org.kohsuke.github.GitHub
import groovy.xml.XmlUtil
import org.apache.commons.io.IOUtils


// parsing command line args
cli = new CliBuilder(usage: 'groovy licenses.groovy [options]\nReports all repositories and their licenses')
cli.u(longOpt: 'user', 'GH username (or use GITHUB_USER env variable)', required: false  , args: 1 )
cli.t(longOpt: 'token', 'personal access token of a GitHub (or use GITHUB_TOKEN env variable)', required: false  , args: 1 )
cli.o(longOpt: 'organisation', 'GH organisation to be query (or use GITHUB_ORG env variable)', required: false  , args: 1 )
cli.h(longOpt: 'help', 'Print this usage info', required: false  , args: 0 )

OptionAccessor opt = cli.parse(args)

token = opt.t?opt.t:System.getenv("GITHUB_TOKEN")
org = opt.o?opt.o:System.getenv("GITHUB_ORG")
user = opt.u?opt.u:System.getenv("GITHUB_USER")
listOnly = opt.l

// bail out if help parameter was supplied or not sufficient input to proceed
if (opt.h || !token  || !user || !org ) {
	cli.usage()
	return
}


// Expand EffectivePom given a POM file
def expandEffectivePom(writer) {
    output = ""
    File file = File.createTempFile("temp",".xml")
    File effective = File.createTempFile("temp",".effective")
    FileWriter fw = new FileWriter(file)
    fw.write(writer.toString())
    fw.close()
    "mvn -B -f ${file.getPath()} help:effective-pom -Doutput=${effective.getPath()} ".execute().text
    projects = new XmlSlurper().parse(effective)
    projects['licenses']?.children().each {
        line <<= "|licensed"
        if (it.name) { output <<= "|${it.name.text()}" }
        if (it.url) { output <<= "|${it.url.text()}" }
    }
    return output
}


try {

    // Connect using a particular user and password
	org = GitHub.connectUsingPassword(user, token).getOrganization(org)

    // For each repo in that particular organisation
    org.listRepositories().each { repo ->
        line = "${repo}"
        found = false       // in order to know whether to fetch effective poms
        try {
            // Retrieve pom file from the root folder
            pom = repo.getFileContent("pom.xml")
            if (pom) {
                try {
                    // Manipulate files
                    InputStream inputStream = pom.read();
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(inputStream, writer, "UTF-8");

                    // Parse xml file
                    project = new XmlSlurper().parseText(writer.toString())

                    // More info details
                    line <<= "|" + project['groupId'].text() + ":" + project['artifactId'].text()

                    // If there is a licenses tag then query
                    project['licenses']?.children().each {
                        line <<= "|licensed"
                        found = true
                        if (it.name) { line <<= "|${it.name.text()}" }
                        if (it.url) {  line <<= "|${it.url.text()}" }
                    }

                    // If no licenses then expand effective pom
                    if (! found) {
                        line <<= expandEffectivePom(writer)
                    }
                } catch (IOException e) {
                    line <<= "|Warning:" + e.getMessage()
                } catch (org.xml.sax.SAXParseException sax) {
                    line <<= "|Warning: " +sax.getMessage()
                }

            } else {
                line <<= "|No pom file"
            }
        } catch (IOException ioe) {
            line <<= "|Warning: " + ioe.getMessage()
        }
        println line
    }

} catch (Exception e) {
	e.printStackTrace()
	println "An error occurred while fetching repositories ..."
}
