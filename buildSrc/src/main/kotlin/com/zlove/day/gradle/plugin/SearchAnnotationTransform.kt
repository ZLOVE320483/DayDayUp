package com.zlove.day.gradle.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * Author by zlove, Email zlove.zhang@bytedance.com, Date on 2021/1/21.
 * PS: Not easy to write code, please indicate.
 */
class SearchAnnotationTransform(private val project: Project): Transform() {

    override fun getName(): String {
        return "SearchAnnotationTransform"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun transform(
        context: Context?,
        inputs: MutableCollection<TransformInput>?,
        referencedInputs: MutableCollection<TransformInput>?,
        outputProvider: TransformOutputProvider?,
        isIncremental: Boolean
    ) {
        println("--- search transform start ---")
        inputs?.forEach { input ->
            val directoryInputs = input.directoryInputs
            directoryInputs?.forEach { directoryInput ->
                SearchAnnotationJavassist.searchClassWithAnnotation(directoryInput.file)
                val dest = outputProvider?.getContentLocation(directoryInput.name,
                    directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            val jarInPuts = input.jarInputs
            jarInPuts?.forEach { jarInput ->
                SearchAnnotationJavassist.searchJarWithAnnotation(jarInput.file)
                val md5Name = DigestUtils.md5Hex(jarInput.file.absolutePath)
                val dest = outputProvider?.getContentLocation(jarInput.name + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
        println("--- search transform end ---")
    }
}