package com.craigburke.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Unroll

class BowerPluginSpec extends Specification {

    def "throw error when node plugin isn't present"() {
        given:
        Project project = ProjectBuilder.builder().build()
        
        when:
        project.pluginManager.apply('com.craigburke.bower-installer')
        
        then:
        thrown(Exception)
    }
    
    @Unroll
    def "#taskName added to project when applied"() {
        given:
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply('com.moowork.node')
        
        when:
        project.pluginManager.apply('com.craigburke.bower-installer')

        then:
        project.tasks[taskName]
        
        where:
        taskName << ['bowerDependencies', 'bowerComponents', 'bowerInstall', 'bowerClean', 'bowerRefresh']
    }
    
    @Unroll
    def "#taskName is created with correct dependsOn"() {
        given:
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply('com.moowork.node')

        when:
        project.pluginManager.apply('com.craigburke.bower-installer')

        then:
        project.tasks[taskName].dependsOn.contains(dependencies)
        
        where:
        taskName            || dependencies
        'bowerComponents'   || 'bowerDependencies'
        'bowerInstall'      || 'bowerComponents'
        'bowerRefresh'      || ['bowerClean', 'bowerInstall']
        'bowerClean'        || 'bowerDependencies'
    }
    
    def "bower module extension is added"() {
        given:
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply('com.moowork.node')

        when:
        project.pluginManager.apply('com.craigburke.bower-installer')
        
        then:
        project.extensions.findByName('bower')
    }

}
