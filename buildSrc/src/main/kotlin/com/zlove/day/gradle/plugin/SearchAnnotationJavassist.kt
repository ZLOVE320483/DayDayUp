package com.zlove.day.gradle.plugin

import com.zlove.day.gradle.plugin.utils.eachFileRecurse
import groovy.io.FileType
import javassist.ClassPool
import java.io.File
import java.util.jar.JarFile

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2021/1/21.
 * PS: Not easy to write code, please indicate.
 */
object SearchAnnotationJavassist {

    private val mClassPool = ClassPool.getDefault()

    fun searchClassWithAnnotation(dirFile: File) {

        mClassPool.appendClassPath(dirFile.absolutePath)

        if (dirFile.isDirectory) {
            dirFile.eachFileRecurse(fileType = FileType.ANY, action = { file ->
                if (!file.isDirectory && file.name.endsWith(".class")) {
                    val relativePath = file.path.replace("${dirFile.absolutePath}/", "")
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

    fun searchJarWithAnnotation(file: File) {
        val jarFile = JarFile(file)
        val enumeration = jarFile.entries()
        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement()
            val entryName = entry.name?: ""
            val className = entryName.replace("/", ".")
            val ctClassName = className.replace(".class", "")

            if (ctClassName.contains("zlove")) {

                try {
                    mClassPool.insertClassPath(file.absolutePath)
                    val ctClass = mClassPool.getCtClass(ctClassName)
                    val deprecated = ctClass.getAnnotation(Deprecated::class.java) as? Deprecated
                    deprecated?.run {
                        println("class --- $ctClassName")
                        println("message --- $message")
                    }
                } catch (e: Exception) {
                    println(e.toString())
                }
            }


//            if (!ctClassName.contains("zlove")) {
//                return
//            }
//            println("ctClass --- $ctClassName")
//            mClassPool.insertClassPath(file.absolutePath)
//            val ctClass = mClassPool.getCtClass(ctClassName)
//            val deprecated = ctClass.getAnnotation(Deprecated::class.java) as? Deprecated
//            deprecated?.run {
//                println("class --- $className")
//                println("message --- $message")
//            }
        }
    }

}