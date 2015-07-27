package com.craigburke.gradle

import groovy.json.JsonBuilder

class BowerModuleExtension {

    String installBase
    Map dependencies = [:]
    Map<String, List> mappings = [
        js : ['js'],
        css: ['css'],
        less: ['less'],
        sass: ['sass', 'scss'],
        fonts: ['ttf', 'woff', 'woff2', 'eot', 'otf', 'svg']
    ]
    Map<String, List> sources = [:]
    List excludes = []
    Map additional = [:]
    
    String getBowerJson() {
        Map install = [base: installBase, path: [:], sources: [:]]
        install.excludes = excludes

        mappings.each {
            install.path["/(${it.value.join('|')})\$/"] = "{name}/${it.key}"
        }

        sources.each { String moduleName, List includes ->
            install.sources[moduleName] = includes.collect { "bower_components/${moduleName}/${it}" }
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

