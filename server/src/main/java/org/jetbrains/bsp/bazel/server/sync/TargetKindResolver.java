package org.jetbrains.bsp.bazel.server.sync;

import org.jetbrains.bsp.bazel.commons.Constants;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;

public class TargetKindResolver {
  public boolean isTestTarget(TargetInfo targetInfo) {
    return targetInfo.getKind().endsWith("_" + Constants.TEST_RULE_TYPE);
  }

  public boolean isRunnableTarget(TargetInfo targetInfo) {
    return targetInfo.getKind().endsWith("_" + Constants.BINARY_RULE_TYPE);
  }
}
