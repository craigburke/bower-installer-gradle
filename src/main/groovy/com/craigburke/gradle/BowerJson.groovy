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

            dependency.sources.each { String key, String value ->
                String includeExpression = getIncludeExpression(dependency.name, key)
                FileTree moduleFiles = bowerFiles.matching { include includeExpression }

                moduleFiles.each { File file ->
                    String relativePath = normalizePath(file.absolutePath) - "${projectRoot.absolutePath}/"
                    String destination = getDestinationPath(dependency.name, relativePath, key, value)

                    sources[dependency.name].mapping << [ (relativePath) : destination ]
                }
            }
        }

        bowerConfig.additional.each{ bowerJson[it.key] = it.value }

        def json = new JsonBuilder()
        json(bowerJson)
        json
    }
    
    static String getIncludeExpression(String moduleName, String expression) {
        boolean hasSearchTokens = expression.contains('*')
        boolean isFile = expression.contains('.')
        String includeExpression = "${moduleName}/${expression}"
        (hasSearchTokens || isFile) ? includeExpression : "${includeExpression}/**"
    }
    
    static String getDestinationPath(String moduleName, String relativePath, String source, String destination) {
        boolean sourceIsFolder = !source.contains('.') && !source.contains('*')
        boolean destinationIsFolder = !destination.contains('.')
        boolean absolutePath = destination.startsWith('/')
        String fileName = relativePath.tokenize('/').last()
        
        String path = absolutePath ? "..${destination}" : destination
        
        if (!destination) {
            path = "${fileName}"
        }
        else if (destinationIsFolder) {
            if (sourceIsFolder) {
                String pathFromModuleRoot = relativePath - "bower_components/${moduleName}/" - "${source}/"
                path += "/${pathFromModuleRoot}"
            }
            else {
                path += "/${fileName}"
            }
        }

        path
    }
}
