package com.zlove.day.gradle.plugin

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
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

    override fun transform(transformInvocation: TransformInvocation?) {
        super.transform(transformInvocation)

        println("--- search transform start ---")

        val inputs = transformInvocation?.inputs
        inputs?.forEach { input ->
            val directoryInputs = input.directoryInputs
            directoryInputs?.forEach { directoryInput ->
                SearchAnnotationJavassist.searchClassWithAnnotation(directoryInput.file.absolutePath, project)
            }
        }
        println("--- search transform end ---")
    }
}