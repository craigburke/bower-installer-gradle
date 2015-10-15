package com.craigburke.gradle

import groovy.json.JsonBuilder
import org.gradle.api.file.FileTree

class BowerJson {
    
    static final String DEFAULT_APP_NAME = 'gradle-bower-installer'

    static private Map getDependenciesMap(List<Dependency> dependencies) {
        Map bowerDependencies = [:]
        dependencies.each { bowerDependencies[it.name] = it.version }
        bowerDependencies
    }
    
    static JsonBuilder generateBasic(BowerModuleExtension bowerConfig) {
        Map bowerJson = [ name: DEFAULT_APP_NAME, dependencies: getDependenciesMap(bowerConfig.dependencies) ]
        
        def json = new JsonBuilder()
        json(bowerJson)
        json
    }

    // returns UNIX style path
    static String normalizePath(String path) {
        path.replace('\\', '/')
    }

    static JsonBuilder generateFinal(BowerModuleExtension bowerConfig, File projectRoot, FileTree bowerFiles) {
        Map bowerJson = [ name: DEFAULT_APP_NAME, dependencies: getDependenciesMap(bowerConfig.dependencies)]
        Map sources = [:]
        bowerJson.install = [ path: bowerConfig.installBase, ignore: [], sources: sources ]

        bowerConfig.dependencies.each { Dependency dependency ->
            bowerJson.install.ignore += dependency.excludes

            if (dependency.sources) {
                sources[dependency.name] = [mapping: []]
            }

            dependency.sources.each { String sourceExpression, String destinationExpression ->
                sourceExpression = normalizeExpression(sourceExpression)
                destinationExpression = normalizeExpression(destinationExpression)

                String includeExpression = getIncludeExpression(dependency.name, sourceExpression)
                FileTree moduleFiles = bowerFiles.matching { include includeExpression }

                moduleFiles.each { File file ->
                    String fullRelativePath = normalizePath(file.absolutePath) - "${normalizePath(projectRoot.absolutePath)}/"
                    String relativePath = getRelativePath(fullRelativePath, sourceExpression, dependency.name)
                    String destination = getDestinationPath(relativePath, sourceExpression, destinationExpression)

                    sources[dependency.name].mapping << [ (fullRelativePath) : destination ]
                }
            }
        }

        bowerConfig.additional.each{ bowerJson[it.key] = it.value }

        def json = new JsonBuilder()
        json(bowerJson)
        json
    }

    static String getRelativePath(String fullRelativePath, String sourceExpression, String moduleName) {
        String sourceFolder
        String lastPart = sourceExpression.contains('/') ? sourceExpression.tokenize('/').last() : sourceExpression

        if (sourceExpression.endsWith('**')) {
            sourceFolder = sourceExpression - "/${lastPart}"
        }
        else if (sourceExpression.endsWith('/') || sourceExpression.contains('*')) {
            sourceFolder = sourceExpression
        }
        else if (sourceExpression.contains('/')) {
            sourceFolder = sourceExpression - "/${lastPart}"
        }
        else {
            sourceFolder = ''
        }

        fullRelativePath - sourceFolder - "bower_components/${moduleName}/"
    }

    static String normalizeExpression(String expression) {
        expression?.startsWith('./') ? expression.substring(2) : expression
    }

    static String getIncludeExpression(String moduleName, String expression) {
        boolean isFile = !expression.endsWith('/')
        String includeExpression = "${moduleName}/${expression}"
        isFile ? includeExpression : "${includeExpression}**"
    }
    
    static String getDestinationPath(String relativePath, String source, String destination) {
        boolean sourceIsFolder = !source.contains('.') && !source.contains('*')
        boolean maintainPath = source.contains('**')
        boolean destinationIsFolder = destination?.endsWith('/')
        boolean absolutePath = destination?.startsWith('/')

        String fileName = relativePath.contains('/') ? relativePath.tokenize('/').last() : relativePath
        String path = absolutePath ? "..${destination}" : destination
        
        if (!destination) {
            path = maintainPath ? relativePath : fileName
        }
        else if (destinationIsFolder) {
            if (sourceIsFolder) {
                String pathFromModuleRoot = relativePath - "${source}/"
                path += pathFromModuleRoot
            }
            else {
                path += maintainPath ? relativePath : fileName
            }
        }

        path
    }
}
