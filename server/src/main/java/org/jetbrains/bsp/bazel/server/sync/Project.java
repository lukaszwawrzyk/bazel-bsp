package org.jetbrains.bsp.bazel.server.sync;

import io.vavr.collection.Map;
import java.util.List;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;

public class Project {
    private final List<String> rootTargets;
    private final Map<String, TargetInfo> targetInfoMap;

    public Project(List<String> rootTargets, Map<String, TargetInfo> targetInfoMap) {
        this.rootTargets = rootTargets;
        this.targetInfoMap = targetInfoMap;
    }

    public Map<String, TargetInfo> getTargets() {
        return targetInfoMap;
    }

    public List<String> getRootTargets() {
        return rootTargets;
    }
}
