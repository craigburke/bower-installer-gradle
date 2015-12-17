package com.craigburke.gradle

import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.NodePlugin
import com.moowork.gradle.node.task.NodeTask
import com.moowork.gradle.node.task.NpmTask
import groovy.io.FileType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree

class BowerInstallerPlugin implements Plugin<Project> {

    static final String NPM_OUTPUT_PATH = 'node_modules'
    static final String DEFAULT_NODE_VERSION = '4.2.3'

    void apply(Project project) {
        setupNode(project)

        final File BOWER_INSTALLER_EXEC = project.file("${NPM_OUTPUT_PATH}/bower-installer/bower-installer")
        final File BOWER_FILE = project.file('bower.json')

        BowerModuleExtension bowerConfig = project.extensions.create('bower', BowerModuleExtension)
        boolean bowerDebug = project.hasProperty('bowerDebug') ? project.property('bowerDebug') : false

        def deleteTempFiles = {
            project.delete 'bower_components'
            BOWER_FILE.delete()
        }

        def nodeExecOverrides = {
            if (!bowerDebug) {
                it.standardOutput = new ByteArrayOutputStream()
            }
        }

        project.task('bowerInit', group: null,
                description: 'Sets up folder structure needed for the bower_installer plugin') {
            project.file(NPM_OUTPUT_PATH).mkdirs()
        }

        project.task('bowerDependencies', type: NpmTask, dependsOn: 'bowerInit', group: null,
                description: 'Installs dependencies needed for the bower_installer.') {
            args = ['install', 'bower-installer']
            if (!bowerDebug) {
                args += ['--silent']
            }
            outputs.dir project.file(NPM_OUTPUT_PATH)
            execOverrides nodeExecOverrides
        }

        project.task('bowerConfig', dependsOn: 'bowerDependencies', group: null) {
            doLast {
                setBowerExec(project)
            }
        }

        project.task('bowerClean', type: NodeTask, dependsOn: 'bowerConfig', group: 'Bower',
                description: 'Clears bower cache and removes all installed bower dependencies') {
            doFirst {
                deleteTempFiles()
                project.delete bowerConfig.installBase
            }

            args = ['cache', 'clean']
            execOverrides nodeExecOverrides
        }

        project.task('bowerComponents', type: NodeTask, dependsOn: 'bowerConfig', group: null) {
            doFirst {
                BOWER_FILE.text = BowerJson.generateBasic(bowerConfig).toString()
            }

            args = ['install']
            execOverrides nodeExecOverrides
            outputs.upToDateWhen {
                project.file(bowerConfig.installBase).exists()
            }
        }

        project.task('bowerInstall', type: NodeTask, dependsOn: 'bowerComponents', group: 'Bower',
                description: 'Installs bower dependencies') {

            doFirst {
                FileTree bowerRoot = project.fileTree('bower_components')
                def bowerJson = BowerJson.generateFinal(bowerConfig, project.projectDir, bowerRoot)
                createSourceFolders(project, bowerConfig.installBase, bowerJson.content.install.sources)
                BOWER_FILE.text = bowerJson.toString()
            }

            script = BOWER_INSTALLER_EXEC
            execOverrides nodeExecOverrides
            outputs.upToDateWhen {
                project.file(bowerConfig.installBase).exists()
            }

            doLast {
                deleteEmptyDirectories(project.file(bowerConfig.installBase))
                if (!bowerDebug) {
                    deleteTempFiles()
                }
            }
        }

        project.task('bowerRefresh',
                dependsOn: ['bowerClean', 'bowerInstall'], group: 'Bower',
                description: 'Clears bower cache and refreshes dependencies')

        project.afterEvaluate {
            setDefaultInstallBase(project, bowerConfig)
            setTaskDependencies(project)
        }
    }

    private static void setupNode(Project project) {
        project.plugins.apply NodePlugin
        NodeExtension nodeConfig = project.extensions.findByName('node') as NodeExtension
        nodeConfig.download = true
        nodeConfig.version = DEFAULT_NODE_VERSION
    }

    private static void deleteEmptyDirectories(File directory) {
        directory.eachFile(FileType.DIRECTORIES) {
            if (!it.list()) {
                it.deleteDir()
            }
        }
    }

    private static void createSourceFolders(Project project, String installBase, sources) {
        sources.each { String moduleName, config ->
            config.mapping.each {
                String destination = it.find().value
                String path = destination.startsWith('../') ? destination.substring(3) : "${moduleName}/${destination}"
                project.file("${installBase}/${path}").parentFile.mkdirs()
            }
        }
    }

    private static void setDefaultInstallBase(Project project, BowerModuleExtension config) {
        if (config.installBase == null) {
            boolean grailsPluginApplied = project.extensions.findByName('grails')
            config.installBase = grailsPluginApplied ? 'grails-app/assets/bower' : 'src/assets/bower'
        }
    }

    private static void setTaskDependencies(Project project) {
        ['run', 'bootRun', 'assetCompile', 'karmaRun', 'karmaWatch'].each { String taskName ->
            def task = project.tasks.findByName(taskName)
            if (task) {
                task.dependsOn 'bowerInstall'
            }
        }
    }

    private static void setBowerExec(Project project) {
        String bowerExecPath

        ['bower-installer/node_modules/bower/bin/bower', 'bower/bin/bower'].each { String path ->
            String fullPath = "${NPM_OUTPUT_PATH}/${path}"
            if (project.file(fullPath).exists()) {
                bowerExecPath = fullPath
            }
        }

        ['bowerComponents', 'bowerClean'].each { String taskName ->
            def task = project.tasks.findByName(taskName)
            task.configure {
                script = project.file(bowerExecPath)
            }
        }
    }


}