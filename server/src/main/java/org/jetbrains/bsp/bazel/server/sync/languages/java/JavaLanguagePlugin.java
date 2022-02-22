package org.jetbrains.bsp.bazel.server.sync.languages.java;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetDataKind;
import ch.epfl.scala.bsp4j.JvmBuildTarget;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import java.net.URI;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;
import org.jetbrains.bsp.bazel.server.sync.BazelPathsResolver;
import org.jetbrains.bsp.bazel.server.sync.languages.LanguagePlugin;

public class JavaLanguagePlugin extends LanguagePlugin<JavaModule> {
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

  @Override
  public Set<URI> dependencySources(TargetInfo targetInfo) {
    if (!targetInfo.hasJavaTargetInfo()) {
      return HashSet.empty();
    }

    var allSourceJars =
        Stream.concat(
                targetInfo.getJavaTargetInfo().getJarsList().stream(),
                targetInfo.getJavaTargetInfo().getGeneratedJarsList().stream())
            .flatMap(outputs -> outputs.getSourceJarsList().stream());

    return HashSet.ofAll(allSourceJars).map(bazelPathsResolver::resolve).map(Path::toUri);
  }

  @Override
  protected void applyModuleData(JavaModule javaModule, BuildTarget buildTarget) {
    JvmBuildTarget jvmBuildTarget = toJvmBuildTarget(javaModule);
    buildTarget.setDataKind(BuildTargetDataKind.JVM);
    buildTarget.setData(jvmBuildTarget);
  }

  public JvmBuildTarget toJvmBuildTarget(JavaModule javaModule) {
    var jdk = javaModule.jdk();
    var javaHome = jdk.javaHome().map(URI::toString).getOrNull();
    return new JvmBuildTarget(javaHome, jdk.javaVersion());
  }
}
