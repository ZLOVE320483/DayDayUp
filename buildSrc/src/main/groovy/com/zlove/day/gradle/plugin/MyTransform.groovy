package com.zlove.day.gradle.plugin

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager

class MyTransform extends Transform {

    @Override
    String getName() {
        return "MyTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)

        println('--- Transform Start ---')

        transformInvocation.inputs.each { input ->
            input.directoryInputs.each { directoryInput ->
                String path = directoryInput.file.absolutePath
                println(path)

                File dir = new File(path)
                if (dir.isDirectory()) {
                    dir.eachFileRecurse { file ->
                        if (!file.isDirectory() && file.name.endsWith('.class')) {
                            println(file.name)
                        }
                    }
                }
            }
        }

        println('--- Transform End ---')
    }
}