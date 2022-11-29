package org.jetbrains.bsp.bazel.server.sync.languages

import org.jetbrains.bsp.bazel.info.BspTargetInfo
import org.jetbrains.bsp.bazel.server.sync.languages.cpp.CppLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.cpp.CppModule
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaModule
import org.jetbrains.bsp.bazel.server.sync.languages.scala.ScalaLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.scala.ScalaModule
import org.jetbrains.bsp.bazel.server.sync.languages.thrift.ThriftLanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.model.Label
import org.jetbrains.bsp.bazel.server.sync.model.Language
import org.jetbrains.bsp.bazel.server.sync.model.Module
import java.util.function.Supplier

class LanguagePluginsService(
    val scalaLanguagePlugin: ScalaLanguagePlugin,
    val javaLanguagePlugin: JavaLanguagePlugin,
    val cppLanguagePlugin: CppLanguagePlugin,
    private val thriftLanguagePlugin: ThriftLanguagePlugin
) {
    private val emptyLanguagePlugin: EmptyLanguagePlugin = EmptyLanguagePlugin()

    fun prepareSync(targetInfos: Sequence<BspTargetInfo.TargetInfo>) {
        scalaLanguagePlugin.prepareSync(targetInfos)
        javaLanguagePlugin.prepareSync(targetInfos)
        cppLanguagePlugin.prepareSync(targetInfos)
        thriftLanguagePlugin.prepareSync(targetInfos)
    }

    fun getPlugin(languages: Set<Language>): LanguagePlugin<*> =
        when {
            languages.contains(Language.SCALA) -> scalaLanguagePlugin
            (languages.contains(Language.JAVA) || languages.contains(Language.KOTLIN)) -> javaLanguagePlugin
            languages.contains(Language.CPP) -> cppLanguagePlugin
            languages.contains(Language.THRIFT) -> thriftLanguagePlugin
            else -> emptyLanguagePlugin
        }

    fun extractJavaModule(module: Module): JavaModule? =
        module.languageData?.let {
            when (it) {
                is JavaModule -> it
                is ScalaModule -> it.javaModule
                else -> null
            }
        }

    fun extractCppModule(module: Module): CppModule? =
        module.languageData?.let {
            when(it) {
                is CppModule -> it
                else -> null
            }
        }

    fun rewriteModules(modules: List<Module>): List<Module> {
        val context = RewriteContext(modules)
        modules.forEach { getPlugin(it.languages).rewriteService.populateContext(it, context) }
        return modules.map { getPlugin(it.languages).rewriteService.rewrite(it, context) }
    }
}

data class Key<T>(val name: String)
class RewriteContext(modules: List<Module>) {
    private val context = mutableMapOf<Label, MutableMap<Key<*>, Any>>()
    private val moduleMap = modules.associateBy { it.label }

    fun module(label: Label): Module? = moduleMap[label]

    fun <T : Any> put(module: Label, key: Key<T>, value: T) {
        if (!context.containsKey(module)) {
            context[module] = mutableMapOf()
        }

        context[module]!![key] = value
    }

    fun <T : Any> has(module: Label, key: Key<T>): Boolean {
        return context[module]?.containsKey(key) == true
    }

    fun <T : Any> get(module: Label, key: Key<T>): T? {
        return context[module]?.get(key)?.let { it as T }
    }

    fun <T : Any> getOrCompute(module: Label, key: Key<T>, f: Supplier<T>): T {
        val valueInContext = get(module, key)
        return if (valueInContext == null) {
            val computedValue = f.get()
            put(module, key, computedValue)
            computedValue
        } else {
            valueInContext
        }
    }
}

open class RewriteService {
    open fun populateContext(module: Module, context: RewriteContext): Unit {

    }

    open fun rewrite(module: Module, context: RewriteContext): Module {
        return module
    }
}