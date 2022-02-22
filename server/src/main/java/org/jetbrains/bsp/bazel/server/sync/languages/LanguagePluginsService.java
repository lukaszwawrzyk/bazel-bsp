package org.jetbrains.bsp.bazel.server.sync.languages;

import io.vavr.collection.Set;
import org.jetbrains.bsp.bazel.server.sync.languages.cpp.CppLanguagePlugin;
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaLanguagePlugin;
import org.jetbrains.bsp.bazel.server.sync.languages.scala.ScalaLanguagePlugin;
import org.jetbrains.bsp.bazel.server.sync.model.Language;

public class LanguagePluginsService {
  private final ScalaLanguagePlugin scalaLanguagePlugin;
  private final JavaLanguagePlugin javaLanguagePlugin;
  private final CppLanguagePlugin cppLanguagePlugin;
  private final EmptyLanguagePlugin emptyLanguagePlugin;

  public LanguagePluginsService(
      ScalaLanguagePlugin scalaLanguagePlugin,
      JavaLanguagePlugin javaLanguagePlugin,
      CppLanguagePlugin cppLanguagePlugin) {
    this.scalaLanguagePlugin = scalaLanguagePlugin;
    this.javaLanguagePlugin = javaLanguagePlugin;
    this.cppLanguagePlugin = cppLanguagePlugin;
    this.emptyLanguagePlugin = new EmptyLanguagePlugin();
  }

  public LanguagePlugin<?> getPlugin(Set<Language> languages) {
    if (languages.contains(Language.SCALA)) {
      return scalaLanguagePlugin;
    } else if (languages.contains(Language.JAVA) || languages.contains(Language.KOTLIN)) {
      return javaLanguagePlugin;
    } else if (languages.contains(Language.CPP)) {
      return cppLanguagePlugin;
    } else {
      return emptyLanguagePlugin;
    }
  }
}
