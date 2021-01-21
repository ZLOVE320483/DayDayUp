package com.zlove.day.gradle.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class MyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println("project name --- ${project.name}")
        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(new MyTransform())
    }
}