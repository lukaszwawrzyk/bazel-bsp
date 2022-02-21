package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetCapabilities;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.BuildTargetTag;
import ch.epfl.scala.bsp4j.DependencySourcesItem;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.InverseSourcesParams;
import ch.epfl.scala.bsp4j.InverseSourcesResult;
import ch.epfl.scala.bsp4j.ResourcesItem;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.SourceItem;
import ch.epfl.scala.bsp4j.SourceItemKind;
import ch.epfl.scala.bsp4j.SourcesItem;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import com.google.common.collect.Iterables;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import org.jetbrains.bsp.bazel.info.BspTargetInfo;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.FileLocation;
import org.jetbrains.bsp.bazel.server.sync.languages.LanguageHub;
import org.jetbrains.bsp.bazel.server.sync.model.Label;
import org.jetbrains.bsp.bazel.server.sync.model.Language;
import org.jetbrains.bsp.bazel.server.sync.model.Module;
import org.jetbrains.bsp.bazel.server.sync.model.Project;
import org.jetbrains.bsp.bazel.server.sync.model.Tag;

public class BspProjectMapper {

  private final BazelPathsResolver bazelPathsResolver;
  private final LanguageHub languageHub;

  public BspProjectMapper(BazelPathsResolver bazelPathsResolver, LanguageHub languageHub) {
    this.bazelPathsResolver = bazelPathsResolver;
    this.languageHub = languageHub;
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

    languageHub
        .getPlugin(module.languages())
        .forEach(
            plugin ->
                module.languageData().forEach(data -> plugin.setModuleData(data, buildTarget)));

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
      default:
        throw new RuntimeException("Unexpected tag: " + tag);
    }
  }

  private BuildTargetCapabilities inferCapabilities(Module module) {
    return new BuildTargetCapabilities(
        true, module.tags().contains(Tag.TEST), module.tags().contains(Tag.APPLICATION));
  }

  public SourcesResult sources(Project project, SourcesParams sourcesParams) {
    // TODO cover `bepServer.getBuildTargetsSources().put(label, srcs)` line from original
    // implementation
    // TODO handle generated sources. google's plugin doesn't ever mark source root as generated
    // we need a use case with some generated files and then figure out how to handle it
    var labels = toLabels(sourcesParams.getTargets());
    var sourcesItems =
        labels.map(
            label ->
                project
                    .findModule(label)
                    .map(this::toSourcesItem)
                    .getOrElse(() -> emptySourcesItem(label)));
    return new SourcesResult(sourcesItems.toJavaList());
  }

  private SourcesItem toSourcesItem(Module module) {
    var sourceSet = module.sourceSet();
    var sourceItems =
        sourceSet
            .sources()
            .map(source -> new SourceItem(toBspUri(source), SourceItemKind.FILE, false));
    var sourceRoots = sourceSet.sourceRoots().map(this::toBspUri);

    var sourcesItem = new SourcesItem(toBsp(module.label()), sourceItems.toJavaList());
    sourcesItem.setRoots(sourceRoots.toJavaList());
    return sourcesItem;
  }

  private SourcesItem emptySourcesItem(Label label) {
    return new SourcesItem(toBsp(label), Collections.emptyList());
  }

  public ResourcesResult resources(Project project, ResourcesParams resourcesParams) {
    var labels = toLabels(resourcesParams.getTargets());
    var resourcesItems =
        labels.map(
            label ->
                project
                    .findModule(label)
                    .map(this::toResourcesItem)
                    .getOrElse(() -> emptyResourcesItem(label)));
    return new ResourcesResult(resourcesItems.toJavaList());
  }

  private ResourcesItem toResourcesItem(Module module) {
    var resources = module.resources().map(this::toBspUri);
    return new ResourcesItem(toBsp(module.label()), resources.toJavaList());
  }

  private ResourcesItem emptyResourcesItem(Label label) {
    return new ResourcesItem(toBsp(label), Collections.emptyList());
  }

  public InverseSourcesResult inverseSources(
      Project project, InverseSourcesParams inverseSourcesParams) {
    var documentUri = URI.create(inverseSourcesParams.getTextDocument().getUri());
    var targets = project.findTargetBySource(documentUri).map(this::toBsp).toList();
    return new InverseSourcesResult(targets.toJavaList());
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

  private Set<Label> toLabels(java.util.List<BuildTargetIdentifier> targets) {
    return HashSet.ofAll(targets).map(BuildTargetIdentifier::getUri).map(Label::from);
  }
}
