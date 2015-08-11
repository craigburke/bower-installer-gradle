package com.craigburke.gradle

import com.moowork.gradle.node.NodePlugin
import com.moowork.gradle.node.task.NodeTask
import com.moowork.gradle.node.task.NpmTask
import groovy.io.FileType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree

class BowerInstallerPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.plugins.apply NodePlugin
        def nodeConfig = project.extensions.findByName('node')
        
        final String NPM_OUTPUT_PATH = project.file(nodeConfig.nodeModulesDir).absolutePath.replace(File.separator, '/') + '/node_modules/'
        final File BOWER_EXEC = project.file(NPM_OUTPUT_PATH + '/bower-installer/node_modules/bower/bin/bower')
        final File BOWER_INSTALLER_EXEC = project.file(NPM_OUTPUT_PATH + '/bower-installer/bower-installer')

        final File BOWER_FILE = project.file('bower.json')

        def bowerConfig = project.extensions.create('bower', BowerModuleExtension)
        
        boolean grailsPluginApplied = project.extensions.findByName('grails')
        bowerConfig.installBase = grailsPluginApplied ? 'grails-app/assets/bower' : 'src/assets/bower'
        boolean bowerDebug = project.hasProperty('bowerDebug') ? project.property('bowerDebug') : false

        def deleteTempFiles = {
            if (!bowerDebug) {
                project.delete 'bower_components'
                BOWER_FILE.delete()
            }
        }
        
        project.task('bowerDependencies', type: NpmTask, group: 'Bower',
                description: 'Installs dependencies needed for the bower_installer.') {
            args = ['install', 'bower-installer', '--silent']
            outputs.dir project.file(NPM_OUTPUT_PATH + 'bower-installer')
        }
        
        project.task('bowerClean', type: NodeTask, dependsOn: 'bowerDependencies', group: 'Bower',
                 description: 'Clears bower cache and removes all installed bower dependencies') {
            doFirst {
                deleteTempFiles()
                project.delete bowerConfig.installBase
            }
            script = BOWER_EXEC
            args = ['cache', 'clean']
        }
        
        project.task('bowerComponents', type: NodeTask, dependsOn: 'bowerDependencies') {
            doFirst {
                BOWER_FILE.text = BowerJson.generateBasic(bowerConfig).toString()
            }
            script = BOWER_EXEC
            args = ['install']
        }

        project.task('bowerInstall', type: NodeTask, dependsOn: 'bowerComponents', group: 'Bower',
                description: 'Installs bower dependencies') {
            doFirst {
                FileTree bowerRoot = project.fileTree('bower_components')
                def bowerJson = BowerJson.generateFinal(bowerConfig, project.rootDir, bowerRoot)
                
                // Make sure containing folder exists
                bowerJson.content.install.sources.each { String moduleName, config ->
                    config.mapping.each {
                        String destination = it.find().value
                        String path = destination.startsWith('../') ? destination.substring(3) : "${moduleName}/${destination}"
                        project.file("${bowerConfig.installBase}/${path}").parentFile.mkdirs()
                    }
                }
                
                BOWER_FILE.text = bowerJson.toString()
            }
            
            script = BOWER_INSTALLER_EXEC

            outputs.upToDateWhen {
                project.file(bowerConfig.installBase).exists()
            }
            
            doLast {
                project.file(bowerConfig.installBase).eachFile(FileType.DIRECTORIES) {
                    if (!it.list()) {
                        it.deleteDir()
                    }
                }
                deleteTempFiles()
            }
        }

        project.task('bowerRefresh',
                dependsOn: ['bowerClean', 'bowerInstall'], group: 'Bower',
                description: 'Clears bower cache and refreshes dependencies')
    }
    
}