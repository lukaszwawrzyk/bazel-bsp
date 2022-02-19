package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetCapabilities;
import ch.epfl.scala.bsp4j.BuildTargetDataKind;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CppBuildTarget;
import ch.epfl.scala.bsp4j.JvmBuildTarget;
import ch.epfl.scala.bsp4j.ResourcesItem;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.ScalaBuildTarget;
import ch.epfl.scala.bsp4j.ScalaPlatform;
import ch.epfl.scala.bsp4j.SourceItem;
import ch.epfl.scala.bsp4j.SourceItemKind;
import ch.epfl.scala.bsp4j.SourcesItem;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.jetbrains.bsp.bazel.bazelrunner.data.BazelData;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.FileLocation;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;
import org.jetbrains.bsp.bazel.server.bsp.utils.SourceRootGuesser;

public class BspProjectMapper {

  private final TargetKindResolver targetKindResolver = new TargetKindResolver();
  private final BazelPathsResolver bazelPathsResolver;

  public BspProjectMapper(BazelData bazelData) {
    this.bazelPathsResolver = new BazelPathsResolver(bazelData);
  }

  public WorkspaceBuildTargetsResult workspaceTargets(Project project) {
    var buildTargets = project.getRootTargets().map(this::toBuildTarget);
    return new WorkspaceBuildTargetsResult(buildTargets.toJavaList());
  }

  private BuildTarget toBuildTarget(TargetInfo targetInfo) {
    var label = new BuildTargetIdentifier(targetInfo.getId());
    var dependencies = prepareDependencies(targetInfo);
    var languages = inferLanguages(targetInfo);
    var capabilities = inferCapabilities(targetInfo);
    var baseDirectory = bazelPathsResolver.labelToDirectory(label.getUri());
    var tag = targetKindResolver.resolveBuildTargetTag(targetInfo);

    var buildTarget =
        new BuildTarget(
            label,
            Collections.emptyList(),
            languages.toJavaList(),
            dependencies.toJavaList(),
            capabilities);
    buildTarget.setDisplayName(label.getUri());
    buildTarget.setBaseDirectory(toBspUri(baseDirectory));
    buildTarget.setTags(List.of(tag).toJavaList());

    if (languages.contains(Language.SCALA.getName())) {
      buildTarget.setDataKind(BuildTargetDataKind.SCALA);
      // TODO resolve from aspect
      var scalaBuildTarget =
          new ScalaBuildTarget(
              "org.scala-lang",
              "2.13.2",
              "2.12",
              ScalaPlatform.JVM,
              List.<String>of().toJavaList());
      extractJvmBuildTarget(targetInfo).forEach(scalaBuildTarget::setJvmBuildTarget);
    } else if (languages.contains(Language.JAVA.getName())
        || Language.KOTLIN.getAllNames().exists(languages::contains)) {
      buildTarget.setDataKind(BuildTargetDataKind.JVM);
      extractJvmBuildTarget(targetInfo).forEach(buildTarget::setData);
    } else if (languages.contains(Language.CPP.getName())) {
      buildTarget.setDataKind(BuildTargetDataKind.CPP);
      // TODO resolve from aspect
      var cppBuildTarget = new CppBuildTarget(null, "", "", "");
      buildTarget.setData(cppBuildTarget);
    }

    return buildTarget;
  }

  private BuildTargetCapabilities inferCapabilities(TargetInfo targetInfo) {
    return new BuildTargetCapabilities(
        true,
        targetKindResolver.isTestTarget(targetInfo),
        targetKindResolver.isRunnableTarget(targetInfo));
  }

  private List<BuildTargetIdentifier> prepareDependencies(TargetInfo targetInfo) {
    return List.ofAll(targetInfo.getDependenciesList())
        .map(d -> new BuildTargetIdentifier(d.getId()));
  }

  private HashSet<String> inferLanguages(TargetInfo targetInfo) {
    return HashSet.ofAll(targetInfo.getSourcesList())
        .flatMap(source -> Language.all().flatMap(lang -> languageFromFile(source, lang)));
  }

  private Set<String> languageFromFile(FileLocation file, Language language) {
    if (language.getExtensions().exists(ext -> file.getRelativePath().endsWith(ext))) {
      return language.getAllNames();
    } else {
      return HashSet.empty();
    }
  }

  private Option<JvmBuildTarget> extractJvmBuildTarget(TargetInfo targetInfo) {
    if (targetInfo.getJavaToolchainInfo() == null) {
      return Option.none();
    }

    var toolchainInfo = targetInfo.getJavaToolchainInfo();
    var javaHome = toBspUri(toolchainInfo.getJavaHome());
    var buildTarget = new JvmBuildTarget(javaHome, toolchainInfo.getSourceVersion());
    return Option.some(buildTarget);
  }

  public SourcesResult sources(Project project, Set<String> labels) {
    // TODO cover `bepServer.getBuildTargetsSources().put(label, srcs)` line from original
    // implementation
    // TODO handle generated sources. google's plugin doesn't ever mark source root as generated
    // we need a use case with some generated files and then figure out how to handle it
    var targets = project.getTargets();
    var sourcesItems =
        labels.map(
            label -> {
              var target = targets.get(label);
              var sourceItems =
                  target
                      .map(
                          info ->
                              List.ofAll(info.getSourcesList())
                                  .map(
                                      file ->
                                          new SourceItem(
                                              toBspUri(file), SourceItemKind.FILE, false)))
                      .getOrElse(List.of());

              var sourcesItem =
                  new SourcesItem(new BuildTargetIdentifier(label), sourceItems.toJavaList());
              var roots = inferSourceRoots(sourceItems);
              sourcesItem.setRoots(roots.asJava());
              return sourcesItem;
            });

    return new SourcesResult(sourcesItems.toJavaList());
  }

  public ResourcesResult resources(Project project, Set<String> labels) {
    var targets = project.getTargets();
    var resourcesItems =
        labels.map(
            label -> {
              var resourceItems =
                  targets
                      .get(label)
                      .map(info -> List.ofAll(info.getResourcesList()).map(this::toBspUri))
                      .getOrElse(List.of());

              return new ResourcesItem(
                  new BuildTargetIdentifier(label), resourceItems.toJavaList());
            });
    return new ResourcesResult(resourcesItems.toJavaList());
  }

  private String toBspUri(FileLocation file) {
    return toBspUri(bazelPathsResolver.resolve(file));
  }

  private String toBspUri(Path path) {
    return path.toUri().toString();
  }

  private List<String> inferSourceRoots(List<SourceItem> items) {
    return items
        .map(SourceItem::getUri)
        .map(Paths::get)
        .map(SourceRootGuesser::getSourcesRoot)
        .map(Path::toUri)
        .map(URI::toString)
        .distinct();
  }
}
