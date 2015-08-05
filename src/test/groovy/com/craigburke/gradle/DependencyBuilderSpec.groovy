package com.craigburke.gradle

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class DependencyBuilderSpec extends Specification {

    @Shared @Subject DependencyBuilder builder
    
    def setup() {
        builder = new DependencyBuilder()
    }
    
    @Unroll
    def "add simple dependency #name version: #version"() {
        when:
        builder."${name}"(version)

        then:
        builder.dependencies.size() == 1
        
        and:
        Dependency dependency = builder.dependencies.first()
        
        and:
        dependency.name == name
        
        and:
        dependency.version == version
        
        where:
        name        | version
        'angular'   | '1.4.x'
        'bootstrap' | '3.3.x'
        'foo'       | '1.0.0'
    }
    
    @Unroll
    def "add simple dependency with no version"() {
        when:
        builder."${name}"()

        then:
        builder.dependencies.size() == 1

        and:
        Dependency dependency = builder.dependencies.first()

        and:
        dependency.name == name

        and:
        dependency.version == 'latest'

        where:
        name << ['angular', 'jquery', 'foo', 'bar']
        
    }
    
    @Unroll
    def "add simple dependency with combined name and version #name:#version"()  {
        when:
        builder."${name}:${version}"()

        then:
        builder.dependencies.size() == 1

        and:
        Dependency dependency = builder.dependencies.first()

        and:
        dependency.name == name

        and:
        dependency.version == version

        where:
        name        | version
        'angular'   | '1.4.x'
        'bootstrap' | '3.3.x'
        'foo'       | '1.0.0'
    }
    
    @Unroll
    def "add dependency with simple exclusion: #exclude"() {
        when:
        builder."foo:1.0.0" {
            excludes exclude
        }

        then:
        Dependency dependency = builder.dependencies.first()

        and:
        dependency.excludes == [exclude]

        where:
        exclude << ['jquery', 'bar', 'bootstrap']
    }

    @Unroll
    def "add dependency with exclusion list: #exclusionList"() {
        when:
        builder."foo:1.0.0" {
            excludes exclusionList
        }

        then:
        Dependency dependency = builder.dependencies.first()

        and:
        dependency.excludes == exclusionList

        where:
        exclusionList << [['jquery', 'bar', 'bootstrap'], ['jquery'], ['bar', 'foobar']]
    }
    
    @Unroll
    def "add simple source file - #sourceFile"() {
        when:
        builder."foo:1.1.0" {
            source sourceFile
        }

        then:
        Dependency dependency = builder.dependencies.first()

        and:
        dependency.sources == ["${sourceFile}": '']

        where:
        sourceFile << ['angular.min.js', 'foo.js', 'bar.css']
    }
    
    @Unroll
    def "add source file #sourceFile with custom destination #destination"() {
        when:
        builder."foo:1.1.0" {
            source sourceFile >> destination
        }

        then:
        Dependency dependency = builder.dependencies.first()

        and:
        dependency.sources == ["${sourceFile}": destination]

        where:
        sourceFile          | destination
        'angular.min.js'    | 'angular.js'
        'foo/**'            | 'bar'
        'bar.css'           | '/css/bar.css'
    }

    
}
