package nebula.plugin.responsible

import nebula.test.dependencies.DependencyGraph
import nebula.test.dependencies.GradleDependencyGenerator

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
        def generator = new GradleDependencyGenerator(new DependencyGraph(dependencyCoordinates), baseDir.canonicalPath)
        generator.generateTestMavenRepo()
    }
}
