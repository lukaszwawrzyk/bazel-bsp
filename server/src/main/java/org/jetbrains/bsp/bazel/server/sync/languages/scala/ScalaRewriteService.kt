package org.jetbrains.bsp.bazel.server.sync.languages.scala

import org.jetbrains.bsp.bazel.server.sync.languages.RewriteContext
import org.jetbrains.bsp.bazel.server.sync.languages.RewriteService
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaRewriteService
import org.jetbrains.bsp.bazel.server.sync.model.Module

class ScalaRewriteService(private val javaRewriteService: JavaRewriteService) : RewriteService() {

    override fun populateContext(module: Module, context: RewriteContext) {
        (module.languageData as ScalaModule?)?.javaModule?.let {
            javaRewriteService.cleanupClasspaths(module, context, it)
        }
    }

    override fun rewrite(module: Module, context: RewriteContext): Module {
        val sourceDependencies =
                context.get(module.label, JavaRewriteService.SOURCE_DEPENDENCIES)?.toSet() ?: module.sourceDependencies
        val scalaModule = module.languageData as ScalaModule
        val newScalaModule = context.get(module.label, JavaRewriteService.IDE_CLASSPATH)?.let { cp ->
            scalaModule.javaModule?.let { scalaModule.copy(javaModule = it.copy(ideClasspath = cp)) }
        } ?: scalaModule
        return module.copy(
                sourceDependencies = sourceDependencies,
                languageData = newScalaModule
        )
    }
}