package org.jetbrains.bsp.bazel.server.sync;

import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;

/** Project should be the internal model of the project.
 * Bazel/Aspect Model -> Project -> BSP Model
 * Currently Project exposes Bazel structures, but it will
 * eventually be changed with further refactorings. */
public class Project {
  private final Set<String> rootTargets;
  private final Map<String, TargetInfo> targets;
  private final Map<String, String> sourceToTarget;

  public Project(
      Set<String> rootTargets,
      Map<String, TargetInfo> targets,
      Map<String, String> sourceToTarget) {
    this.rootTargets = rootTargets;
    this.targets = targets;
    this.sourceToTarget = sourceToTarget;
  }

  public Map<String, TargetInfo> getTargets() {
    return targets;
  }

  public Set<TargetInfo> getRootTargets() {
    return rootTargets.map(label -> targets.get(label).get());
  }

  public Option<String> findTargetBySource(String documentUri) {
    return sourceToTarget.get(documentUri);
  }
}
