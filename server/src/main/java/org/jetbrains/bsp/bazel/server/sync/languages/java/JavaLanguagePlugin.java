package org.jetbrains.bsp.bazel.server.sync.languages.java;

import io.vavr.control.Option;
import java.net.URI;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;
import org.jetbrains.bsp.bazel.server.sync.BazelPathsResolver;
import org.jetbrains.bsp.bazel.server.sync.languages.LanguagePlugin;

public class JavaLanguagePlugin implements LanguagePlugin<JavaModule> {
  private final BazelPathsResolver bazelPathsResolver;

  public JavaLanguagePlugin(BazelPathsResolver bazelPathsResolver) {
    this.bazelPathsResolver = bazelPathsResolver;
  }

  @Override
  public Option<JavaModule> resolveModule(TargetInfo targetInfo) {
    if (!targetInfo.hasJavaTargetInfo()) {
      return Option.none();
    }

    var toolchainInfo = targetInfo.getJavaToolchainInfo();
    Option<URI> javaHome =
        toolchainInfo.hasJavaHome()
            ? Option.some(bazelPathsResolver.resolve(toolchainInfo.getJavaHome()).toUri())
            : Option.none();
    var version = toolchainInfo.getSourceVersion();
    var jdk = new Jdk(version, javaHome);
    return Option.some(new JavaModule(jdk));
  }
}
