package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetCapabilities;
import ch.epfl.scala.bsp4j.BuildTargetDataKind;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.BuildTargetTag;
import ch.epfl.scala.bsp4j.CppBuildTarget;
import ch.epfl.scala.bsp4j.DependencySourcesItem;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.InverseSourcesParams;
import ch.epfl.scala.bsp4j.InverseSourcesResult;
import ch.epfl.scala.bsp4j.JvmBuildTarget;
import ch.epfl.scala.bsp4j.ResourcesItem;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.ScalaBuildTarget;
import ch.epfl.scala.bsp4j.ScalaPlatform;
import ch.epfl.scala.bsp4j.SourceItem;
import ch.epfl.scala.bsp4j.SourceItemKind;
import ch.epfl.scala.bsp4j.SourcesItem;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.bsp.bazel.info.BspTargetInfo;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.FileLocation;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;
import org.jetbrains.bsp.bazel.server.bsp.utils.SourceRootGuesser;
import org.jetbrains.bsp.bazel.server.sync.model.Label;
import org.jetbrains.bsp.bazel.server.sync.model.Language;
import org.jetbrains.bsp.bazel.server.sync.model.Module;
import org.jetbrains.bsp.bazel.server.sync.model.Project;
import org.jetbrains.bsp.bazel.server.sync.model.Tag;

public class BspProjectMapper {

  private final BazelPathsResolver bazelPathsResolver;

  public BspProjectMapper(BazelPathsResolver bazelPathsResolver) {
    this.bazelPathsResolver = bazelPathsResolver;
  }

  public WorkspaceBuildTargetsResult workspaceTargets(Project project) {
    var buildTargets = project.modules().map(this::toBuildTarget);
    return new WorkspaceBuildTargetsResult(buildTargets.toJavaList());
  }

  private BuildTarget toBuildTarget(Module module) {
    var label = toBsp(module.label());
    var dependencies = module.directDependencies().map(this::toBsp);
    var languages = module.languages().flatMap(Language::getAllNames);
    var capabilities = inferCapabilities(module);
    var tags = module.tags().map(this::toBsp);
    var baseDirectory = module.baseDirectory();

    var buildTarget =
        new BuildTarget(
            label,
            tags.toJavaList(),
            languages.toJavaList(),
            dependencies.toJavaList(),
            capabilities);
    buildTarget.setDisplayName(label.getUri());
    buildTarget.setBaseDirectory(toBspUri(baseDirectory));

    if (languages.contains(Language.SCALA.getName())) {
      buildTarget.setDataKind(BuildTargetDataKind.SCALA);
      // TODO resolve from aspect
      var scalaBuildTarget =
          new ScalaBuildTarget(
              "org.scala-lang",
              "2.12.8",
              "2.12",
              ScalaPlatform.JVM,
              ImmutableList.of(
                  "__main__/external/io_bazel_rules_scala_scala_compiler/scala-compiler-2.12.8.jar",
                  "__main__/external/io_bazel_rules_scala_scala_library/scala-library-2.12.8.jar",
                  "__main__/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.8.jar"));
      extractJvmBuildTarget(module).forEach(scalaBuildTarget::setJvmBuildTarget);
      buildTarget.setData(scalaBuildTarget);
    } else if (languages.contains(Language.JAVA.getName())
        || Language.KOTLIN.getAllNames().exists(languages::contains)) {
      buildTarget.setDataKind(BuildTargetDataKind.JVM);
      extractJvmBuildTarget(module).forEach(buildTarget::setData);
    } else if (languages.contains(Language.CPP.getName())) {
      buildTarget.setDataKind(BuildTargetDataKind.CPP);
      // TODO resolve from aspect
      var cppBuildTarget = new CppBuildTarget(null, "", "", "");
      buildTarget.setData(cppBuildTarget);
    }

    return buildTarget;
  }

  private BuildTargetIdentifier toBsp(Label label) {
    return new BuildTargetIdentifier(label.getValue());
  }

  private String toBsp(Tag tag) {
    switch (tag) {
      case APPLICATION:
        return BuildTargetTag.APPLICATION;
      case TEST:
        return BuildTargetTag.TEST;
      case LIBRARY:
        return BuildTargetTag.LIBRARY;
      case NO_IDE:
        return BuildTargetTag.NO_IDE;
    }
  }

  private BuildTargetCapabilities inferCapabilities(Module module) {
    return new BuildTargetCapabilities(
        true, module.tags().contains(Tag.TEST), module.tags().contains(Tag.APPLICATION));
  }

  private Option<JvmBuildTarget> extractJvmBuildTarget(TargetInfo targetInfo) {
    if (!targetInfo.hasJavaTargetInfo()) {
      return Option.none();
    }

    var toolchainInfo = targetInfo.getJavaToolchainInfo();
    var javaHome = toolchainInfo.hasJavaHome() ? toBspUri(toolchainInfo.getJavaHome()) : null;
    var buildTarget = new JvmBuildTarget(javaHome, toolchainInfo.getSourceVersion());
    return Option.some(buildTarget);
  }

  public SourcesResult sources(Project project, SourcesParams sourcesParams) {
    // TODO cover `bepServer.getBuildTargetsSources().put(label, srcs)` line from original
    // implementation
    // TODO handle generated sources. google's plugin doesn't ever mark source root as generated
    // we need a use case with some generated files and then figure out how to handle it
    var labels = toLabels(sourcesParams.getTargets());
    var targets = project.getTargets();
    var sourcesItems = labels.map(label -> getSourcesItem(targets, label));
    return new SourcesResult(sourcesItems.toJavaList());
  }

  private SourcesItem getSourcesItem(Map<String, TargetInfo> targets, String label) {
    var target = targets.get(label);
    var sourceItems = target.map(this::getSourceItems).getOrElse(List.of());
    var sourcesItem = new SourcesItem(new BuildTargetIdentifier(label), sourceItems.toJavaList());
    var roots = inferSourceRoots(sourceItems);
    sourcesItem.setRoots(roots.asJava());
    return sourcesItem;
  }

  private List<SourceItem> getSourceItems(TargetInfo info) {
    return List.ofAll(info.getSourcesList())
        .map(file -> new SourceItem(toBspUri(file), SourceItemKind.FILE, false));
  }

  private List<String> inferSourceRoots(List<SourceItem> items) {
    return items.map(this::inferSourceRoot).distinct();
  }

  public ResourcesResult resources(Project project, ResourcesParams resourcesParams) {
    var labels = toLabels(resourcesParams.getTargets());
    var targets = project.getTargets();
    var resourcesItems = labels.map(label -> getResourcesItem(targets, label));
    return new ResourcesResult(resourcesItems.toJavaList());
  }

  private ResourcesItem getResourcesItem(Map<String, TargetInfo> targets, String label) {
    var target = targets.get(label);
    var resourceItems =
        target
            .map(info -> List.ofAll(info.getResourcesList()).map(this::toBspUri))
            .getOrElse(List.of());

    return new ResourcesItem(new BuildTargetIdentifier(label), resourceItems.toJavaList());
  }

  private String inferSourceRoot(SourceItem item) {
    var path = Paths.get(URI.create(item.getUri()));
    var rootPath = SourceRootGuesser.getSourcesRoot(path);
    return rootPath.toUri().toString();
  }

  public InverseSourcesResult inverseSources(
      Project project, InverseSourcesParams inverseSourcesParams) {
    var documentUri = inverseSourcesParams.getTextDocument().getUri();
    var targets = project.findTargetBySource(documentUri).toList();
    return new InverseSourcesResult(targets.map(BuildTargetIdentifier::new).toJavaList());
  }

  public DependencySourcesResult dependencySources(
      Project project, DependencySourcesParams dependencySourcesParams) {
    var labels = toLabels(dependencySourcesParams.getTargets());
    var items = labels.map(label -> getDependencySourcesItem(project, label));
    return new DependencySourcesResult(items.toJavaList());
  }

  private DependencySourcesItem getDependencySourcesItem(Project project, String label) {
    var target = project.getTargets().get(label);
    var sources =
        target
            .map(
                t -> {
                  // do not return source jars of imported
                  // modules, neither source jars that other
                  // imported modules will report
                  var deps =
                      project.getTransitiveDependencies(
                          t, dep -> !project.getRootTargetLabels().contains(dep.getId()));
                  return deps.flatMap(
                      info -> {
                        if (info.hasJavaTargetInfo()) {
                          return HashSet.ofAll(
                                  Iterables.concat(
                                      info.getJavaTargetInfo().getJarsList(),
                                      info.getJavaTargetInfo().getGeneratedJarsList()))
                              .flatMap(BspTargetInfo.JvmOutputs::getSourceJarsList)
                              .map(this::toBspUri);
                        } else {
                          return HashSet.of();
                        }
                      });
                })
            .getOrElse(HashSet.of());
    return new DependencySourcesItem(new BuildTargetIdentifier(label), sources.toJavaList());
  }

  private String toBspUri(FileLocation file) {
    return toBspUri(bazelPathsResolver.resolve(file));
  }

  private String toBspUri(Path path) {
    return path.toUri().toString();
  }

  private String toBspUri(URI uri) {
    return uri.toString();
  }

  private Set<String> toLabels(java.util.List<BuildTargetIdentifier> targets) {
    return HashSet.ofAll(targets).map(BuildTargetIdentifier::getUri);
  }
}
