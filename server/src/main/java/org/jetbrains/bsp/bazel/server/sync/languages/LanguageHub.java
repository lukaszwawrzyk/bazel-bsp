package org.jetbrains.bsp.bazel.server.sync.languages;

import io.vavr.collection.Set;
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaLanguagePlugin;
import org.jetbrains.bsp.bazel.server.sync.languages.scala.ScalaLanguagePlugin;
import org.jetbrains.bsp.bazel.server.sync.model.Language;

public class LanguageHub {
    private final ScalaLanguagePlugin scalaLanguagePlugin;
    private final JavaLanguagePlugin javaLanguagePlugin;

    public LanguageHub(ScalaLanguagePlugin scalaLanguagePlugin, JavaLanguagePlugin javaLanguagePlugin) {
        this.scalaLanguagePlugin = scalaLanguagePlugin;
        this.javaLanguagePlugin = javaLanguagePlugin;
    }

    private LanguagePlugin<?> selectPlugin(Set<Language> languages) {
        if ()
    }
}
