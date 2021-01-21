package com.zlove.day.gradle.plugin

import com.android.build.gradle.AppExtension
import com.zlove.day.gradle.plugin.utils.eachFileRecurse
import groovy.io.FileType
import javassist.ClassPool
import org.gradle.api.Project
import java.io.File

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2021/1/21.
 * PS: Not easy to write code, please indicate.
 */
object SearchAnnotationJavassist {

    private val mClassPool = ClassPool.getDefault()

    fun searchClassWithAnnotation(path: String, project: Project) {

        println("path --- $path")

        val android = project.extensions.getByType(AppExtension::class.java)
        mClassPool.appendClassPath(path)
        mClassPool.appendClassPath(android.bootClasspath[0].toString())

        val dir = File(path)
        if (dir.isDirectory) {
            dir.eachFileRecurse(fileType = FileType.ANY, action = { file ->
                if (!file.isDirectory && file.name.endsWith(".class")) {
                    val relativePath = file.path.replace("$path/", "")
                    val classPath = relativePath.replace(".class", "")
                    val className = classPath.replace("/", ".");

                    val ctClass = mClassPool.getCtClass(className)
                    val deprecated = ctClass.getAnnotation(Deprecated::class.java) as? Deprecated
                    deprecated?.run {
                        println("class --- $className")
                        println("message --- $message")
                    }
                }
            })
        }
    }

}