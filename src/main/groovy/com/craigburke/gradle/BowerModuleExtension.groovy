package com.craigburke.gradle

import groovy.json.JsonBuilder

class BowerModuleExtension {
    
    boolean debug = false
    
    Map dependencies
    Map install = [:]
    Map additional = [:]

    void setInstall(Map value) {
        if (!value.path) {
            value.path = install.path
        }
        install = value
    }
    
    String getBowerJson() {
        Map properties = [name: 'gradle-bower-installer', 
                          dependencies: dependencies,
                          install: install ]
        
        additional.each{ properties[it.key] = it.value }

        def json = new JsonBuilder()
        json(properties)
        json.toString()
    }

}

