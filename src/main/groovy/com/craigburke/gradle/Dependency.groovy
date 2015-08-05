package com.craigburke.gradle

class Dependency {
    String name
    String version
    Map sources = [:]
    List<String> excludes = []
}
