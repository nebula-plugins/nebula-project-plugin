package nebula.plugin.responsible

import nebula.test.dependencies.DependencyGraph
import nebula.test.dependencies.DependencyGraphNode
import nebula.test.dependencies.GradleDependencyGenerator
import org.gradle.testkit.runner.GradleRunner

class MavenRepoFixture {
    private final File baseDir
    private final File mavenRepoDir

    MavenRepoFixture(File baseDir) {
        this.baseDir = baseDir
        mavenRepoDir = new File(baseDir, 'mavenrepo')
    }

    File getMavenRepoDir() {
        return mavenRepoDir
    }

    void generateMavenRepoDependencies(List<String> dependencyCoordinates) {
        def graph = new DependencyGraph(dependencyCoordinates)
        // The generator defaults to Java 8, which is not installed by the CI matrix.
        graph.nodes = graph.nodes.collect { node ->
            new DependencyGraphNode(coordinate: node.coordinate, dependencies: node.dependencies,
                status: node.status, targetCompatibility: 17)
        }
        new GradleDependencyGenerator(graph, baseDir.canonicalPath)
        // build() surfaces publication failures instead of leaving an incomplete repository.
        GradleRunner.create()
            .withProjectDir(baseDir)
            .withArguments('publishMavenPublicationToMavenRepository')
            .build()
    }
}
