package com.craigburke.gradle

import groovy.json.JsonBuilder

class BowerModuleExtension {
    
    String installPath
    boolean debug = false
    
    Map dependencies
    Map install = [:]
    Map additional = [:]

    String getBowerJson() {
        if (!install.path) {
            install.path = installPath
        }
        
        Map properties = [name: 'gradle-bower-installer', 
                          dependencies: dependencies,
                          install: install ]
        
        additional.each{ properties[it.key] = it.value }

        def json = new JsonBuilder()
        json(properties)
        json.toString()
    }

}

