package com.craigburke.gradle

import com.moowork.gradle.node.task.NodeTask
import com.moowork.gradle.node.task.NpmTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class BowerInstallerPlugin implements Plugin<Project> {

    void apply(Project project) {
        File bowerFile = project.file('bower.json')
        File bowerExec = project.file('node_modules/bower-installer/node_modules/bower/bin/bower')
        File bowerInstallerExec = project.file('node_modules/bower-installer/bower-installer')
        
        if (!project.extensions.findByName('node')) {
            throw new GradleException('The Node plugin needs to be installed and applied.')
        }

        def bowerConfig = project.extensions.create('bower', BowerModuleExtension)
        boolean grailsPluginApplied = project.extensions.findByName('grails')
        bowerConfig.installPath = grailsPluginApplied ?  'grails-app/assets/libs/bower' : 'src/assets/bower'

        project.task('bowerDependencies', type: NpmTask) {
            args = ['install', 'bower-installer', '--silent']
            outputs.dir project.file('node_modules/bower-installer')
        }
        
        project.task('bowerGenerateFile', dependsOn: 'bowerDependencies') << {
            bowerFile.text = bowerConfig.bowerJson
            println bowerConfig.bowerJson
        }

        project.task('bowerInstall', type: NodeTask, dependsOn: 'bowerGenerateFile') {
            script = bowerInstallerExec
            inputs.file bowerFile
            outputs.dir bowerConfig.installPath
         }

         project.task('bowerCleanCache', type: NodeTask, dependsOn:'bowerDependencies') {
             script = bowerExec
             args = ['cache', 'clean']
         }

    }

}