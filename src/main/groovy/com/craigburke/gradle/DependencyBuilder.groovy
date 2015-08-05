package com.craigburke.gradle

class DependencyBuilder {

    List<Dependency> dependencies = []
    Dependency currentDependency

    class SourceCategory {
        static Map rightShift(String source, String path) {
            ["${source}": path]
        }
    }

    def methodMissing(String name, args) {
        String version
        String dependencyName = name

        if (dependencyName.contains(':')) {
            (dependencyName, version) = dependencyName.tokenize(':')
        }
        else if (args instanceof String) {
            version = args
        }
        else if (args) {
            version = args.first() instanceof String ? args.first() : 'latest'
        }
        else {
            version = 'latest'
        }

        currentDependency = new Dependency(name: dependencyName, version: version)
        dependencies += currentDependency

        if (args && args.last() instanceof Closure) {
            Closure clonedClosure = args.last().rehydrate(this, this, this)
            use (SourceCategory) {
                clonedClosure()
            }
        }
    }

    void source(String sourceValue) {
        source "${sourceValue}": ''
    }

    void source(Map sourceValue) {
        currentDependency.sources += sourceValue
    }

    void excludes(String exclude) {
        currentDependency.excludes << exclude
    }

    void excludes(List exclude) {
        currentDependency.excludes += exclude
    }
}
