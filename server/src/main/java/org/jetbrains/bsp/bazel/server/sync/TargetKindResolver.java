package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.BuildTargetTag;
import java.util.Map;
import org.jetbrains.bsp.bazel.commons.Constants;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;

public class TargetKindResolver {
  private Map<String, String> ruleSuffixToTargetType =
      Map.of(
          Constants.LIBRARY_RULE_TYPE, BuildTargetTag.LIBRARY,
          Constants.BINARY_RULE_TYPE, BuildTargetTag.APPLICATION,
          Constants.TEST_RULE_TYPE, BuildTargetTag.TEST);

  public boolean isTestTarget(TargetInfo targetInfo) {
    return targetInfo.getKind().endsWith("_" + Constants.TEST_RULE_TYPE);
  }

  public boolean isRunnableTarget(TargetInfo targetInfo) {
    return targetInfo.getKind().endsWith("_" + Constants.BINARY_RULE_TYPE);
  }

  public String resolveBuildTargetTag(TargetInfo targetInfo) {
    return ruleSuffixToTargetType.entrySet().stream()
        .filter(entry -> targetInfo.getKind().contains(entry.getKey()))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(BuildTargetTag.NO_IDE);
  }
}
