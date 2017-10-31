#!/usr/bin/env groovy

@Grab(group='org.kohsuke', module='github-api', version='1.90')
@Grab(group='commons-io', module='commons-io', version='2.5')

import org.kohsuke.github.GitHub
import groovy.xml.XmlUtil
import org.apache.commons.io.IOUtils
import groovy.transform.ToString
import groovy.json.JsonBuilder

@ToString(includeNames=true)
class MavenRepo {
    String fullname
    String pom = null
    String groupId = null
    String artifactId = null
	boolean pomLicense = false
	boolean ghLicense = false
	String pomLicenseName
	String pomLicenseUrl
	String ghLicenseName
	String status = 'WARN'
	String message

	def toCSV() {
       return "${status};" +
		 	  "${fullname};" +
		 	  "${pom};" +
		 	  "${groupId};" +
			  "${artifactId};" +
		 	  "${pomLicense};" +
		 	  "${ghLicense};" +
		 	  "${pomLicenseName};" +
		 	  "${pomLicenseUrl};" +
		 	  "${ghLicenseName};" +
		 	  "${message}"
    }
}

// parsing command line args
cli = new CliBuilder(usage: 'groovy licenses.groovy [options]\nReports all repositories and their licenses')
cli.u(longOpt: 'user', 'GH username (or use GITHUB_USER env variable)', required: false  , args: 1 )
cli.t(longOpt: 'token', 'personal access token of a GitHub (or use GITHUB_TOKEN env variable)', required: false  , args: 1 )
cli.o(longOpt: 'organisation', 'GH organisation to be query (or use GITHUB_ORG env variable)', required: false  , args: 1 )
cli.f(longOpt: 'format', 'Format to be used (csv,json)', required: false  , args: 1 )
cli.a(longOpt: 'all', 'Query also Licenses field from GitHub (or use GITHUB_ALL env variable)', required: false  , args: 0 )
cli.h(longOpt: 'help', 'Print this usage info', required: false  , args: 0 )


OptionAccessor opt = cli.parse(args)

token = opt.t?opt.t:System.getenv("GITHUB_TOKEN")
org = opt.o?opt.o:System.getenv("GITHUB_ORG")
user = opt.u?opt.u:System.getenv("GITHUB_USER")
all = opt.a?true:System.getenv("GITHUB_ALL")
format = opt.f?opt.f:'csv'

// bail out if help parameter was supplied or not sufficient input to proceed
if (opt.h || !token  || !user || !org ) {
	cli.usage()
	return
}

// Expand EffectivePom given a POM file
def expandEffectivePom(writer, data) {
	File file = File.createTempFile("temp",".xml")
	File effective = File.createTempFile("temp",".effective")
	FileWriter fw = new FileWriter(file)
	fw.write(writer.toString())
	fw.close()
	"mvn -B -f ${file.getPath()} help:effective-pom -Doutput=${effective.getPath()} ".execute().text
	projects = new XmlSlurper().parse(effective)
	projects['licenses']?.children().each {
		data.pomLicense = true
		if (it.name) { data.pomLicenseName = it.name.text() }
		if (it.url) { data.pomLicenseUrl = it.url.text() }
		data.message = data.message + "|pom-effective-license"
	}

	if (data.pomLicense ) {
		data.status = 'WARN'
	} else {
		data.status = 'INFO'
	}
}


try {

	// Connect using a particular user and password
	org = GitHub.connectUsingPassword(user, token).getOrganization(org)

	// For each repo in that particular organisation
	org.listRepositories().each { repo ->
		data = new MavenRepo(fullname: repo.getFullName())
		try {
			// Retrieve pom file from the root folder
			pom = repo.getFileContent("pom.xml")
			if (pom) {
				try {
					data.pom = pom.getName()

					// Manipulate files
					InputStream inputStream = pom.read()
					StringWriter writer = new StringWriter()
					IOUtils.copy(inputStream, writer, "UTF-8")

					// Parse xml file
					project = new XmlSlurper().parseText(writer.toString())

					// More info details
					data.groupId = project['groupId'].text()
					data.artifactId = project['artifactId'].text()

					// If there is a licenses tag then query
					project['licenses']?.children().each {
						data.pomLicense = true
						if (it.name) { data.pomLicenseName=it.name.text() }
						if (it.url) { data.pomLicenseUrl=it.url.text() }
						data.message = "pom-license"
					}

					// If no licenses then expand effective pom
					if (! data.pomLicense ) {
						expandEffectivePom(writer, data)
					} else {
						data.status = 'INFO'
					}
				} catch (IOException e) {
					data.status = 'WARN'
					data.message = e.getMessage()
				} catch (org.xml.sax.SAXParseException sax) {
					data.status = 'WARN'
					data.message = sax.getMessage()
				}
			} else {
				data.status = 'WARN'
				data.message = 'No POM file'
			}

			if (all) {
				license = repo?.getLicense()
				if (license) {
					data.ghLicense = true
					data.ghLicenseName = license.name
					data.message = data.message + "|github-license"
				} else {
					data.ghLicense = false
				}
			}
		} catch (IOException ioe) {
			data.status = 'WARN'
			data.message = ioe.getMessage()
		}
		if (format.equals('json')) {
			println new JsonBuilder( data ).toPrettyString()
		} else {
			println data.toCSV()
		}
	}

} catch (Exception e) {
	e.printStackTrace()
	println "An error occurred while fetching repositories ..."
}
