package com.craigburke.gradle

import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class BowerJsonSpec extends Specification {
    
    @Shared BowerModuleExtension bowerConfig
    
    def setup() {
        bowerConfig = new BowerModuleExtension()
        bowerConfig.installBase = 'src/assets/bower'
    }
    
    @Unroll
    def "generate basic bower.json file with #count dependencies"() {
        given:
        bowerConfig.dependencies = generateDependencies(count)
        
        when:
        String jsonText = BowerJson.generateBasic(bowerConfig)
        def result = new JsonSlurper().parseText(jsonText)
        Map jsonDependencies = result.dependencies
        
        then:
        result.name == BowerJson.DEFAULT_APP_NAME
        
        and:
        jsonDependencies.size() == count
        
        and:
        matchesDependencies(jsonDependencies)
        
        where:
        count << [0, 1, 10, 20, 30]
    }

    @Unroll
    def "copy file #source with the default destination" () {
        given:
        dependencySource = ["${source}":'']

        when:
        def result = BowerJson.generateFinal(bowerConfig, projectRoot, bowerRoot).content

        then:
        def mapping = result.install.sources['foo'].mapping[0]
        mapping["bower_components/foo/${source}"] == destination
        
        where:
        source                 || destination
        'css/foo.css'          || 'foo.css'
        'js/foo.js'            || 'foo.js'
        'js/foo.min.js'        || 'foo.min.js'
        'js/modules/module.js' || 'module.js'
    }
    
    @Unroll
    def "generate final bower.json with simple mapping #source to #destination"() {
        given:
        dependencySource = ["${source}":"${destination}"]
     
        when:
        def result = BowerJson.generateFinal(bowerConfig, projectRoot, bowerRoot).content

        then:
        def mapping = result.install.sources['foo'].mapping[0]
        mapping["bower_components/foo/${source}"] == destination

        where:
        source          | destination
        'css/foo.css'   | 'css/foobar.css'
        'js/foo.js'     | 'js/foo.js'
        'js/foo.min.js' | 'js/foo.js'
    }

    @Unroll("copy #source to #destination")
    def "generate final bower.json with #source being copied to a subfolder"() {
        dependencySource = ["**/*.js" : "subfolder/"]

        when:
        def result = BowerJson.generateFinal(bowerConfig, projectRoot, bowerRoot).content

        then:
        def mapping = result.install.sources['foo'].mapping.find { it[source] }
        mapping[source] == destination

        where:
        source                                      | destination
        'bower_components/foo/blah.js'              | 'subfolder/blah.js'
        'bower_components/foo/js/foo.min.js'        | 'subfolder/js/foo.min.js'
        'bower_components/foo/js/modules/module.js' | 'subfolder/js/modules/module.js'
    }

    @Unroll("copy #source to #destination")
    def "generate final bower.json copying an entire folders with subfolders"() {
        dependencySource = ["./js/" : "./scripts/"]

        when:
        def result = BowerJson.generateFinal(bowerConfig, projectRoot, bowerRoot).content

        then:
        def mapping = result.install.sources['foo'].mapping.find { it[source] }
        mapping[source] == destination

        where:
        source                                      | destination
        'bower_components/foo/js/foo.js'            | 'scripts/foo.js'
        'bower_components/foo/js/foo.min.js'        | 'scripts/foo.min.js'
        'bower_components/foo/js/modules/module.js' | 'scripts/modules/module.js'
    }

    @Unroll("copy #source to #destination")
    def "generate final bower.json copying files to an absolute path"() {
        dependencySource = ["./js/" : "/scripts/"]

        when:
        def result = BowerJson.generateFinal(bowerConfig, projectRoot, bowerRoot).content

        then:
        def mapping = result.install.sources['foo'].mapping.find { it[source] }
        mapping[source] == destination

        where:
        source                                      | destination
        'bower_components/foo/js/foo.js'            | '../scripts/foo.js'
        'bower_components/foo/js/foo.min.js'        | '../scripts/foo.min.js'
        'bower_components/foo/js/modules/module.js' | '../scripts/modules/module.js'
    }

    private File getProjectRoot() {
        new File(this.getClass().classLoader.getResource('project').path)
    }
    
    private FileTree getBowerRoot() {
        File bowerFolder = new File("${projectRoot.path}/bower_components")
        Project project = ProjectBuilder.builder().build()
        project.fileTree(bowerFolder.path)
    }
    
    private void setDependencySource(Map source) {
        Dependency dependency = new Dependency(name: 'foo', version: '1.0.0')
        dependency.sources = source
        bowerConfig.dependencies = [dependency]
    }
    
    private List<Dependency> generateDependencies(int count) {
        def dependencies = []
        
        if (count) {
            dependencies = (1..count).collect {
                new Dependency(name: "dependency${it}", version: "1.1.${it}")
            }
        }

        dependencies
    }
    
    private boolean matchesDependencies(Map jsonDependencies) {
        Map configDependencies = bowerConfig.dependencies.collectEntries { [ "${it.name}" : it.version ] }
        configDependencies == jsonDependencies.collectEntries { [ "${it.key}": it.value ] }
    }
    

}
