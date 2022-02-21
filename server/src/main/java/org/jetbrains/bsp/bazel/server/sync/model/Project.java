package org.jetbrains.bsp.bazel.server.sync.model;

import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import java.util.function.Predicate;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;

/**
 * Project should be the internal model of the project. Bazel/Aspect Model -> Project -> BSP Model
 */
public class Project {
  private final Set<String> rootTargets;
  private final Map<String, TargetInfo> targets;
  private final Map<String, String> sourceToTarget;
  private final List<Module> modules;

  public Project(
      Set<String> rootTargets,
      Map<String, TargetInfo> targets,
      Map<String, String> sourceToTarget,
      List<Module> modules) {
    this.rootTargets = rootTargets;
    this.targets = targets;
    this.sourceToTarget = sourceToTarget;
    this.modules = modules;
  }

  public List<Module> modules() {
    return modules;
  }

  public Map<String, TargetInfo> getTargets() {
    return targets;
  }

  public Set<String> getRootTargetLabels() {
    return rootTargets;
  }

  public Option<String> findTargetBySource(String documentUri) {
    return sourceToTarget.get(documentUri);
  }

  public Set<TargetInfo> getTransitiveDependencies(
      TargetInfo target, Predicate<TargetInfo> filter) {
    var directFilteredDeps =
        HashSet.ofAll(target.getDependenciesList())
            .flatMap(dep -> targets.get(dep.getId()).toSet())
            .filter(filter);
    var transitiveDeps = directFilteredDeps.flatMap(dep -> getTransitiveDependencies(dep, filter));
    return directFilteredDeps.addAll(transitiveDeps);
  }
}
