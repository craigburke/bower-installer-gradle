package com.craigburke.gradle

import com.moowork.gradle.node.task.NodeTask
import com.moowork.gradle.node.task.NpmTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class BowerInstallerPlugin implements Plugin<Project> {

    void apply(Project project) {
        
        def nodeConfig = project.extensions.findByName('node')
        if (!nodeConfig) {
            throw new GradleException('The Node plugin needs to be installed and applied.')
        }

        final String NPM_OUTPUT_PATH = project.file(nodeConfig.nodeModulesDir).absolutePath + '/node_modules/'
        final File BOWER_FILE = project.file('bower.json')
        final File BOWER_EXEC = project.file(NPM_OUTPUT_PATH + '/bower-installer/node_modules/bower/bin/bower')
        final File BOWER_INSTALLER_EXEC = project.file(NPM_OUTPUT_PATH + '/bower-installer/bower-installer')
        
        def bowerConfig = project.extensions.create('bower', BowerModuleExtension)
        boolean grailsPluginApplied = project.extensions.findByName('grails')
        bowerConfig.installPath = grailsPluginApplied ? 'grails-app/assets/libs/bower' : 'src/assets/bower'

        def deleteTempFiles = {
            if (!bowerConfig.debug) {
                project.delete 'bower_components'
                BOWER_FILE.delete()
            }
        }
        
        Task bowerDependencies = project.task('bowerDependencies',
                type: NpmTask,
                group: 'Bower',
                description: 'Installs dependencies needed for the bower_installer.')
        
        bowerDependencies.configure {
            args = ['install', 'bower-installer', '--silent']
            outputs.dir project.file(NPM_OUTPUT_PATH + 'bower-installer')
        }

        Task bowerClean = project.task('bowerClean',
                 type: NodeTask, dependsOn: 'bowerDependencies', group: 'Bower',
                 description: 'Clears bower cache and removes all installed bower dependencies')
        bowerClean.doFirst {
            deleteTempFiles()
            project.delete bowerConfig.installPath
        }
        bowerClean.configure {
            script = BOWER_EXEC
            args = ['cache', 'clean']
        }

        Task bowerInstall = project.task('bowerInstall',
                type: NodeTask,
                dependsOn: 'bowerDependencies',
                group: 'Bower',
                description: 'Installs bower dependencies')

        bowerInstall.doFirst {
            BOWER_FILE.text = bowerConfig.bowerJson
        }
        bowerInstall.configure {
            script = BOWER_INSTALLER_EXEC
            outputs.dir bowerConfig.installPath
        }
        bowerInstall.doLast deleteTempFiles
        bowerInstall.shouldRunAfter bowerClean
        
        project.task('bowerRefresh',
                dependsOn: ['bowerClean', 'bowerInstall'], group: 'Bower',
                description: 'Clears bower cache and refreshes dependencies')
    }

}