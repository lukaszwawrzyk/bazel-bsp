package org.jetbrains.bsp.bazel.server.sync;

import io.vavr.collection.Map;
import io.vavr.collection.Set;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;

public class Project {
  private final Set<String> rootTargets;
  private final Map<String, TargetInfo> targetInfoMap;

  public Project(Set<String> rootTargets, Map<String, TargetInfo> targetInfoMap) {
    this.rootTargets = rootTargets;
    this.targetInfoMap = targetInfoMap;
  }

  public Map<String, TargetInfo> getTargets() {
    return targetInfoMap;
  }

  public Set<TargetInfo> getRootTargets() {
    return rootTargets.map(label -> targetInfoMap.get(label).get());
  }
}
