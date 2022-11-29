package org.jetbrains.bsp.bazel.server.sync.languages.java

import org.jetbrains.bsp.bazel.server.sync.languages.Key
import org.jetbrains.bsp.bazel.server.sync.languages.RewriteContext
import org.jetbrains.bsp.bazel.server.sync.languages.RewriteService
import org.jetbrains.bsp.bazel.server.sync.model.Module
import java.net.URI

class JavaRewriteService : RewriteService() {
    companion object {
        val IDE_CLASSPATH = Key<List<URI>>("jvm.ide-classpath")
        val SOURCE_DEPENDENCIES = Key<List<URI>>("jvm.source-classpath")
    }

    override fun populateContext(module: Module, context: RewriteContext) {
        (module.languageData as JavaModule?)?.let {
            cleanupClasspaths(module, context, it)
        }
    }

    fun cleanupClasspaths(module: Module, context: RewriteContext, javaModule: JavaModule) {
        if (context.has(module.label, IDE_CLASSPATH) && context.has(module.label, SOURCE_DEPENDENCIES)) {
            return
        }
        module.directDependencies.forEach { context.module(it)?.let { m -> populateContext(m, context) } }

        cleanupClasspath(module, context, javaModule.ideClasspath, IDE_CLASSPATH)
        cleanupClasspath(module, context, javaModule.sourcesClasspath, SOURCE_DEPENDENCIES)
    }

    private fun cleanupClasspath(module: Module, context: RewriteContext, currentClasspath: List<URI>, key: Key<List<URI>>) {
        val dependencyClasspath = module.directDependencies.mapNotNull { context.get(it, key) }.flatten().toSet()
        val newClasspath = currentClasspath.filterNot { dependencyClasspath.contains(it) }
        context.put(module.label, key, newClasspath)
    }

    override fun rewrite(module: Module, context: RewriteContext): Module {
        val sourceDependencies = context.get(module.label, SOURCE_DEPENDENCIES)?.toSet() ?: module.sourceDependencies
        val javaModule = module.languageData as JavaModule?
        val newJavaModule = javaModule?.let {
            val classpath = context.get(module.label, IDE_CLASSPATH) ?: it.ideClasspath
            it.copy(ideClasspath = classpath)
        }
        return module.copy(
                sourceDependencies = sourceDependencies,
                languageData = newJavaModule
        )
    }
}