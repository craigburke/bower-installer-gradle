package com.craigburke.gradle

import groovy.json.JsonBuilder

class BowerModuleExtension {

    Map dependencies = [:]
    Map install = [:]

    Map<String, List> mappings = [
        js : ['js'],
        css: ['css'],
        less: ['less'],
        sass: ['sass', 'scss'],
        fonts: ['ttf', 'woff', 'woff2', 'eot', 'otf', 'svg']
    ]
    Map additional = [:]

    void setInstall(Map value) {
        def retain = ['base', 'path']
        retain.each {
            if (!value[it]) {
                value[it] = install[it]
            }
        }
        install = value
    }
    
    String getBowerJson() {
        Map bowerInstallMap = install?.clone() ?: [:]
        
        Map properties = [name: 'gradle-bower-installer', 
                          dependencies: dependencies,
                          install: bowerInstallMap ]
        
        additional.each{ properties[it.key] = it.value }
        properties.install.path = [:]
        
        mappings.each {
            bowerInstallMap.path["/(${it.value.join('|')})\$/"] = "{name}/${it.key}"
        }
        if (install.path) {
            bowerInstallMap.path << install.path
        }

        def json = new JsonBuilder()
        json(properties)
        json.toString()
    }

}

