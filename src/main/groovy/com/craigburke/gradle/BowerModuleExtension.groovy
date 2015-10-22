package com.craigburke.gradle

class BowerModuleExtension {

    String installBase
    Map additional = [:]

    private DependencyBuilder builder = new DependencyBuilder()

    void dependencies(Closure closure) {
        Closure clonedClosure = closure.rehydrate(builder, this, this)
        clonedClosure()
    }

    def methodMissing(String name, args) {
        builder.methodMissing(name, args)
    }

    List<Dependency> getDependencies() {
        builder.dependencies
    }

    void setDependencies(List<Dependency> dependencies) {
        builder.dependencies = dependencies
    }

}