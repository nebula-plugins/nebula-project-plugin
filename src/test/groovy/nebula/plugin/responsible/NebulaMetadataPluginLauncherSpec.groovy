package nebula.plugin.responsible

import nebula.test.IntegrationSpec

/**
 * @author J. Michael McGarr
 */
class NebulaMetadataPluginLauncherSpec extends IntegrationSpec {

    String mavenLocal = "${System.env['HOME']}/.m2/repository"

    def 'published pom contains a collected property'() {
        given: 'a java project applying the metadata plugin'
        settingsFile = new File(projectDir, 'settings.gradle')
        settingsFile.text = "rootProject.name='world'"
        writeHelloWorld('nebula.hello')
        buildFile << '''
            apply plugin: 'java'
            apply plugin: 'info'
            apply plugin: 'nebula-publishing'
            apply plugin: 'nebula-metadata'

            group = 'nebula.hello'
            version = '1.0'

            repositories { jcenter() }

            dependencies {
                compile 'asm:asm:3.1'
            }
        '''.stripIndent()

        when: 'the artifacts are built and published'
        def results = runTasksSuccessfully('publishToMavenLocal')

        then: 'the build was successful'
        results.failure == null

        and: 'publishes a pom file'
        def pomFile = new File("$mavenLocal/nebula/hello/world/1.0/world-1.0.pom")
        pomFile.exists()

        and: 'the published pom contains a collected value'
        def pom = new XmlSlurper().parseText(pomFile.text)
        pom.properties.'nebula.Implementation-Version' == '1.0'
        pom.properties.'nebula.Implementation-Title' == 'nebula.hello#world;1.0'
    }
}
