package com.craigburke.gradle

import groovy.json.JsonBuilder

class BowerModuleExtension {

    Map dependencies
    Map install = [
       base: '',
       path : [
           js: '{name}/js',
           css: '{name}/css',
           eot: '{name}/fonts',
           svg: '{name}/fonts',
           ttf: '{name}/fonts',
           woff: '{name}/fonts'
       ]
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
        Map properties = [name: 'gradle-bower-installer', 
                          dependencies: dependencies,
                          install: install ]
        
        additional.each{ properties[it.key] = it.value }

        def json = new JsonBuilder()
        json(properties)
        json.toString()
    }

}

