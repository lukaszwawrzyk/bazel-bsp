package org.jetbrains.bsp.bazel.server.sync;

import java.util.Map;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;

public class Project {
    private final Map<String, TargetInfo> targetInfoMap;

    public Project(Map<String, TargetInfo> targetInfoMap) {
        this.targetInfoMap = targetInfoMap;
    }

    public Map<String, TargetInfo> getTargets() {
        return targetInfoMap;
    }
}
