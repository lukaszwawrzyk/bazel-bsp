package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import java.util.ArrayList;

public class BspProjectMapper {
    public WorkspaceBuildTargetsResult workspaceTargets(Project project) {
        var buildTargets = new ArrayList<BuildTarget>();

        return new WorkspaceBuildTargetsResult(buildTargets);
    }
}
