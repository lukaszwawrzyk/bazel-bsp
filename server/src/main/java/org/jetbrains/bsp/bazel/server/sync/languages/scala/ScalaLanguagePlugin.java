package org.jetbrains.bsp.bazel.server.sync.languages.scala;

import io.vavr.collection.HashSet;
import io.vavr.control.Option;
import java.net.URI;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;
import org.jetbrains.bsp.bazel.server.sync.BazelPathsResolver;
import org.jetbrains.bsp.bazel.server.sync.languages.LanguagePlugin;
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaLanguagePlugin;

public class ScalaLanguagePlugin implements LanguagePlugin<ScalaModule> {
  private final JavaLanguagePlugin javaLanguagePlugin;
  private final BazelPathsResolver bazelPathsResolver;

  public ScalaLanguagePlugin(
      JavaLanguagePlugin javaLanguagePlugin, BazelPathsResolver bazelPathsResolver) {
    this.javaLanguagePlugin = javaLanguagePlugin;
    this.bazelPathsResolver = bazelPathsResolver;
  }

  @Override
  public Option<ScalaModule> resolveModule(TargetInfo targetInfo) {
    // TODO resolve from aspect
    var compilerJars =
        HashSet.of(
                "file:///__main__/external/io_bazel_rules_scala_scala_compiler/scala-compiler-2.12.8.jar",
                "file:///__main__/external/io_bazel_rules_scala_scala_library/scala-library-2.12.8.jar",
                "file:///__main__/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.8.jar")
            .map(URI::create);
    var sdk = new ScalaSdk("org.scala-lang", "2.12.8", "2.12", compilerJars);
    var module = new ScalaModule(sdk, javaLanguagePlugin.resolveModule(targetInfo));
    return Option.some(module);
  }
}
