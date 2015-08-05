package com.craigburke.gradle

class BowerModuleExtension {

    String installBase

    List<Dependency> dependencies = []
    Map additional = [:]
    
    void dependencies(Closure closure) {
        def builder = new DependencyBuilder()
        Closure clonedClosure = closure.rehydrate(builder, this, this)
        clonedClosure()
        dependencies = builder.dependencies
    }
    
}

