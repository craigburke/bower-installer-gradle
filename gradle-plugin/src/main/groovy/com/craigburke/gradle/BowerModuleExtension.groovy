package com.craigburke.gradle

import groovy.json.JsonBuilder

class BowerModuleExtension {
    
    String installPath
    boolean debug = false
    
    String name
    String version
    String description
    def license
    List authors
    Map dependencies
    Map install
    List<String> ignore
    Map additional = [:]

    String getBowerJson() {
        if (install != null && !install.path) {
            install.path = installPath
        }

        List bowerProperties = ['name', 'version', 'dependencies', 'description',
                                'authors', 'ignore', 'install']

        Map properties = [:]

        bowerProperties.findAll { owner[it] != null }
                .each { properties[it] = owner[it] }

        additional.findAll { it.value != null }
                .each { properties[it.key] = it.value }

        def json = new JsonBuilder()
        json(properties)
        json.toString()
    }

}

