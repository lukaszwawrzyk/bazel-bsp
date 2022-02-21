package org.jetbrains.bsp.bazel.server.sync.languages;

import io.vavr.collection.Set;
import io.vavr.control.Option;
import org.jetbrains.bsp.bazel.server.sync.languages.cpp.CppLanguagePlugin;
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaLanguagePlugin;
import org.jetbrains.bsp.bazel.server.sync.languages.scala.ScalaLanguagePlugin;
import org.jetbrains.bsp.bazel.server.sync.model.Language;

public class LanguageHub {
  private final ScalaLanguagePlugin scalaLanguagePlugin;
  private final JavaLanguagePlugin javaLanguagePlugin;
  private final CppLanguagePlugin cppLanguagePlugin;

  public LanguageHub(
      ScalaLanguagePlugin scalaLanguagePlugin,
      JavaLanguagePlugin javaLanguagePlugin,
      CppLanguagePlugin cppLanguagePlugin) {
    this.scalaLanguagePlugin = scalaLanguagePlugin;
    this.javaLanguagePlugin = javaLanguagePlugin;
    this.cppLanguagePlugin = cppLanguagePlugin;
  }

  public Option<LanguagePlugin<?>> getPlugin(Set<Language> languages) {
    if (languages.contains(Language.SCALA)) {
      return Option.some(scalaLanguagePlugin);
    } else if (languages.contains(Language.JAVA) || languages.contains(Language.KOTLIN)) {
      return Option.some(javaLanguagePlugin);
    } else if (languages.contains(Language.CPP)) {
      return Option.some(cppLanguagePlugin);
    } else {
      return Option.none();
    }
  }
}
