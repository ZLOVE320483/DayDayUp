package com.zlove.day.gradle.plugin

import com.android.build.gradle.AppExtension
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.util.*

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2021/1/21.
 * PS: Not easy to write code, please indicate.
 */
class SearchAnnotationPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val localProperties = Properties()
        localProperties.load(FileUtils.openInputStream(File("${project.rootDir.path}/local.properties")))

        val gradleProperties = project.properties
        val needOutputRouter = gradleProperties?.containsKey("outputRouter")?: false
        if (localProperties["outputRouter"] == true || needOutputRouter) {
            val android = project.extensions.getByType(AppExtension::class.java)
            android.registerTransform(SearchAnnotationTransform(project))
        }
    }
}